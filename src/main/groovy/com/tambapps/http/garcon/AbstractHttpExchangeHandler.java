package com.tambapps.http.garcon;

/**
 * Abstract base class for an {@link HttpExchangeHandler}
 */
public abstract class AbstractHttpExchangeHandler implements HttpExchangeHandler {

  @Override
  public abstract HttpResponse processExchange(HttpRequest request);

  HttpResponse default400Response(String message) {
    return newErrorResponse(HttpStatus.BAD_REQUEST, message);
  }

  HttpResponse default404Response(String message) {
    return newErrorResponse(HttpStatus.NOT_FOUND, message);
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
    response.getHeaders().putContentType(ContentType.TEXT);
    return response;
  }

}