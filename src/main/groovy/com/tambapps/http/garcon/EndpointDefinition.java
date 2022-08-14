package com.tambapps.http.garcon;

import com.tambapps.http.garcon.exception.ParsingException;
import groovy.lang.Closure;
import lombok.Value;

@Value
class EndpointDefinition {

  Closure<?> closure;
  ContentType accept;
  ContentType contentType;

  HttpResponse call(HttpExchangeContext context) {
    // rehydrating
    closure.setDelegate(context);
    closure.setResolveStrategy(Closure.DELEGATE_FIRST);
    HttpResponse response = context.getResponse();
    try {
      Object returnValue = closure.call();
      if (response.getBody() == null && returnValue != null) {
        ContentType contentType = context.getContentType();
        if (contentType != null) {
          response.headers.putContentTypeHeader(contentType.getHeaderValue());
          Closure<?> composer = context.getComposers().getAt(contentType);
          if (composer != null) {
            returnValue = composer.call(returnValue);
          }
        }
        response.setBody(returnValue);
      }
    } catch (ParsingException e) {
      response = new HttpResponse();
      response.setStatusCode(HttpStatus.BAD_REQUEST);
      response.setBody(e.getMessage());
    }
    return response;
  }

}
