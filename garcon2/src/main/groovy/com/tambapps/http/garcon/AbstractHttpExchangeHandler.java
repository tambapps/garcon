package com.tambapps.http.garcon;

import static com.tambapps.http.garcon.Headers.CONNECTION_CLOSE;
import static com.tambapps.http.garcon.Headers.CONNECTION_KEEP_ALIVE;

public abstract class AbstractHttpExchangeHandler implements HttpExchangeHandler {

  @Override
  public abstract HttpResponse processExchange(HttpRequest request);

  HttpResponse default404Response() {
    return newErrorResponse(HttpStatus.NOT_FOUND, "No resource were found at the provided path");
  }

  HttpResponse default405Response(String method) {
    return newErrorResponse(HttpStatus.METHOD_NOT_ALLOWED, String.format("Method %s is not allowed for this path", method));
  }

  HttpResponse default500Response() {
    return newErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An internal error occurred");
  }


  private HttpResponse newErrorResponse(HttpStatus status, String message) {
    HttpResponse response = new HttpResponse();
    response.setStatusCode(status);
    response.setBody(message);
    response.getHeaders().put(Headers.CONTENT_TYPE_HEADER, ContentType.TEXT.getHeaderValue());
    return response;
  }

}