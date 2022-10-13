package com.tambapps.http.garcon;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Abstract base class for an {@link HttpExchangeHandler}
 */
public abstract class AbstractHttpExchangeHandler implements HttpExchangeHandler {

  @Override
  public abstract HttpResponse processExchange(HttpRequest request);

  HttpResponse default400Response(HttpExchangeContext context, Exception e) {
    return newErrorResponse(HttpStatus.BAD_REQUEST, context, e.getMessage());
  }

  HttpResponse default404Response(HttpExchangeContext context, Exception e) {
    return newErrorResponse(HttpStatus.NOT_FOUND, context, e.getMessage());
  }

  HttpResponse default405Response(HttpExchangeContext context, String method) {
    return newErrorResponse(HttpStatus.METHOD_NOT_ALLOWED, context, String.format("Method %s is not allowed for this path", method));
  }

  HttpResponse default500Response(HttpExchangeContext context) {
    return newErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, context, "An internal error occurred");
  }


  private HttpResponse newErrorResponse(HttpStatus status, HttpExchangeContext context, String message) {
    HttpResponse response = context.getResponse();
    response.setStatusCode(status);
    ContentType contentType = context.getContentType();
    Object responseBody = message;
    if (ContentType.JSON.equals(contentType)) {
      Map<String, Object> errorResponse = new HashMap<>();
      errorResponse.put("status", status.getValue());
      errorResponse.put("message", message);
      responseBody = errorResponse;
    }

    Function<Object, byte[]> composer;
    if (contentType != null && context.getComposers().containsKey(contentType)) {
      composer = context.getComposers().getAt(contentType);
      response.getHeaders().putContentType(contentType);
    } else {
      composer = context.getComposers().getDefaultValue();
      response.getHeaders().putContentType(ContentType.TEXT);
    }
    response.setBody(composer.apply(responseBody));
    return response;
  }

}