package com.tambapps.http.garcon.endpoint;

import com.tambapps.http.garcon.ContentType;
import com.tambapps.http.garcon.HttpExchangeContext;
import com.tambapps.http.garcon.HttpResponse;
import groovy.lang.Closure;
import lombok.Getter;
import lombok.SneakyThrows;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * The definition of an endpoint
 */
@Getter
public class EndpointDefinition {

  // needed because we modify the delegate before calling the closure
  private final ThreadLocal<OptimizedClosure> threadLocalClosure;
  private final ContentType accept;
  private final ContentType contentType;

  /**
   * Constructs an endpoint definition
   *
   * @param accept             the accept content type (request)
   * @param contentType        the response content type
   */
  public EndpointDefinition(Closure<?> closure, ContentType accept, ContentType contentType) {
    this(ThreadLocal.withInitial(() -> new OptimizedClosure((Closure<?>) closure.clone())), accept, contentType);
  }

  /**
   * Constructs an endpoint definition
   *
   * @param threadLocalClosure the thread-local closure
   * @param accept             the accept content type (request)
   * @param contentType        the response content type
   */
  protected EndpointDefinition(ThreadLocal<OptimizedClosure> threadLocalClosure, ContentType accept, ContentType contentType) {
    this.threadLocalClosure = threadLocalClosure;
    this.accept = accept;
    this.contentType = contentType;
  }

  public HttpResponse call(HttpExchangeContext context) {
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
      response.getHeaders().putContentTypeHeader(context.getContentType());
    }
    return response;
  }

  public DynamicEndpointDefinition toDynamic(List<String> pathVariableNames, Pattern pattern) {
    return new DynamicEndpointDefinition(threadLocalClosure, accept, contentType, pathVariableNames, pattern);
  }

  /**
   * Class used to by-pass Groovy metaclass calls in order to make Closure executions faster
   */
  protected static class OptimizedClosure {
    private final Closure<?> closure;
    private final Method method;

    /**
     * Constructs an optimized closure based on a closure
     * @param closure the closure
     */
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
