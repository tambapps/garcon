package com.tambapps.http.garcon;

import groovy.lang.Closure;
import lombok.Value;

@Value
class EndpointDefinition {

  // needed because we modify the delegate before calling the closure
  ThreadLocal<Closure<?>> threadLocalClosure;
  ContentType accept;
  ContentType contentType;

  public EndpointDefinition(Closure<?> closure, ContentType accept, ContentType contentType) {
    this.threadLocalClosure = ThreadLocal.withInitial(() -> (Closure<?>) closure.clone());
    this.accept = accept;
    this.contentType = contentType;
  }

  HttpResponse call(HttpExchangeContext context) {
    // rehydrating
    Closure<?> closure = threadLocalClosure.get();
    closure.setDelegate(context);
    closure.setResolveStrategy(Closure.DELEGATE_FIRST);
    HttpResponse response = context.getResponse();

    Object returnValue = closure.call();
    if (response.getBody() == null && returnValue != null) {
      ContentType contentType = context.getContentType();
      if (contentType != null) {
        Closure<?> composer = context.getComposers().getAt(contentType);
        if (composer != null) {
          returnValue = composer.call(returnValue);
        }
      }
      response.setBody(returnValue);
    }
    if (context.getContentType() != null) {
      // using context's contentType because the definition CT might be null, and the garcon's might not be null
      response.headers.putContentTypeHeader(context.getContentType().getHeaderValue());
    }
    return response;
  }

}
