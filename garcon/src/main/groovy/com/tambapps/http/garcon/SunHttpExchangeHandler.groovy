package com.tambapps.http.garcon

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.tambapps.http.garcon.io.QueryParamParser
import org.codehaus.groovy.runtime.DefaultGroovyMethods

class SunHttpExchangeHandler implements HttpHandler, HttpExchangeHandler {

  AbstractGarcon garcon
  List<EndpointDefinition> pathEndpointDefinitions

  @Override
  void handle(HttpExchange exchange) throws IOException {
    Map<String, String> queryParams = [:]
    String query = exchange.requestURI.query
    if (query) {
      QueryParamParser.parseQueryParams(query, queryParams)
    }

    Headers headers = new Headers()
    exchange.requestHeaders.each { key, value -> headers[key] = value.join(', ') }
    HttpRequest request = new HttpRequest(exchange.requestMethod, exchange.requestURI.path, queryParams, exchange.protocol, headers.asImmutable(), exchange.requestBody)
    try {
      HttpResponse response = processExchange(request)
      def responseHeaders = exchange.responseHeaders
      addDefaultHeaders(request, response)
      response.headers.each { key, value ->
        responseHeaders.add(key, value)
      }
      long sunResponseLength
      if (response.contentLength == null) {
        sunResponseLength = 0
      } else if (response.contentLength == 0L) {
        sunResponseLength = -1
      } else {
        sunResponseLength = response.contentLength
      }
      exchange.sendResponseHeaders(response.statusCode.value,  sunResponseLength)
      response.writeBody(exchange.responseBody)
    } finally {
     DefaultGroovyMethods.closeQuietly(exchange.responseBody)
    }
  }

  private HttpResponse processExchange(HttpRequest request) {
    def definition = pathEndpointDefinitions.find { it.method == request.method }
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
