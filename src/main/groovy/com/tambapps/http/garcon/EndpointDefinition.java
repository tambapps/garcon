package com.tambapps.http.garcon;

import groovy.lang.Closure;
import lombok.SneakyThrows;
import lombok.Value;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Function;

@Value
class EndpointDefinition {

  // needed because we modify the delegate before calling the closure
  ThreadLocal<OptimizedClosure> threadLocalClosure;
  ContentType accept;
  ContentType contentType;

  public EndpointDefinition(Closure<?> closure, ContentType accept, ContentType contentType) {
    this.threadLocalClosure = ThreadLocal.withInitial(() -> new OptimizedClosure((Closure<?>) closure.clone()));
    this.accept = accept;
    this.contentType = contentType;
  }

  HttpResponse call(HttpExchangeContext context) {
    // rehydrating
    OptimizedClosure closure = threadLocalClosure.get();
    HttpResponse response = context.getResponse();

    Object returnValue = closure.callWithDelegate(context);
    if (response.getBody() == null && returnValue != null) {
      ContentType contentType = context.getContentType();
      if (contentType != null) {
        Function<Object, byte[]> composer = context.getComposers().getAt(contentType);
        if (composer != null) {
          returnValue = composer.apply(returnValue);
        }
      }
      response.setBody(returnValue);
    }
    if (context.getContentType() != null) {
      // using context's contentType because the definition CT might be null, and the garcon's might not be null
      response.headers.putContentTypeHeader(context.getContentType());
    }
    return response;
  }

  /**
   * Class used to by-pass Groovy metaclass calls in order to make Closure executions faster
   */
  private static class OptimizedClosure {
    Closure<?> closure;
    Method method;

    @SneakyThrows
    public OptimizedClosure(Closure<?> closure) {
      this.closure = closure;
      closure.setResolveStrategy(Closure.DELEGATE_FIRST);
      this.method = closure.getClass().getMethod("doCall");
    }

    @SneakyThrows
    Object callWithDelegate(Object o) {
      closure.setDelegate(o);
      try {
        return method.invoke(closure);
      } catch (InvocationTargetException e) {
        throw e.getTargetException();
      }
    }
  }
}
