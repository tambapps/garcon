package com.tambapps.http.garcon;

/**
 * Interface for processing an HTTP request, returning an HTTP response
 */
public interface HttpExchangeHandler {

  /**
   * process the provided request to return a response
   *
   * @param request the request
   * @return an HTTP response
   */
  HttpResponse processExchange(HttpRequest request);

}
