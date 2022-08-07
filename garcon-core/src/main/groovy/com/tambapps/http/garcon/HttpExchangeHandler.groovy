package com.tambapps.http.garcon;

import static com.tambapps.http.garcon.Headers.CONNECTION_CLOSE
import static com.tambapps.http.garcon.Headers.CONNECTION_KEEP_ALIVE

trait HttpExchangeHandler {

  protected AbstractGarcon garcon

  void addDefaultHeaders(HttpRequest request, HttpResponse response) {
    Headers responseHeaders = response.getHeaders()
    responseHeaders.put("Server", "Garcon (Tambapps)")
    Long contentLength = response.getContentLength()
    if (contentLength != null) {
      responseHeaders.put("Content-Length", contentLength.toString())
    }

    String connectionHeader = responseHeaders.getConnectionHeader()
    if (connectionHeader == null) {
      // keep connection alive if request body and response body are with definite length AND client want so
      responseHeaders.putConnectionHeader(response.is2xxSuccessful()
          && contentLength != null
          && request != null
          && CONNECTION_KEEP_ALIVE.equals(request.getHeaders().getConnectionHeader())
          && request.getHeaders().getContentLength() != null
          ? CONNECTION_KEEP_ALIVE : CONNECTION_CLOSE)
    }
  }

  HttpResponse default404Response() {
    return new HttpResponse(statusCode: HttpStatus.NOT_FOUND)
  }

  HttpResponse default405Response() {
    return new HttpResponse(statusCode: HttpStatus.METHOD_NOT_ALLOWED)
  }

  HttpResponse default500Response() {
    return new HttpResponse(statusCode: HttpStatus.INTERNAL_SERVER_ERROR)
  }

 abstract List<EndpointDefinition> findPathEndpoints(String path)

  HttpResponse processExchange(HttpRequest request) {
    def pathDefinitions = findPathEndpoints(request.path)
    if (!pathDefinitions) {
      return default404Response()
    }

    def definition = pathDefinitions.find { it.method == request.method }
    if (definition == null) {
      return default405Response()
    }
    def context = new HttpExchangeContext(request, new HttpResponse(), garcon.composers, garcon.parsers, definition.contentType ?: garcon.contentType, definition.accept ?: garcon.accept)

    try {
      return definition.call(context)
    } catch (Exception e) {
      garcon.onConnectionUnexpectedError?.call(e)
      return default500Response()
    }
  }
}
