package com.tambapps.http.garcon

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.tambapps.http.garcon.io.QueryParamParser
import groovy.transform.CompileStatic
import org.codehaus.groovy.runtime.DefaultGroovyMethods

@CompileStatic
class SunHttpExchangeHandler implements HttpHandler, HttpExchangeHandler {

  List<EndpointDefinition> pathEndpointDefinitions

  @Override
  void handle(HttpExchange exchange) throws IOException {
    HttpRequest request = toGarconRequest(exchange)
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

  private static HttpRequest toGarconRequest(HttpExchange exchange) {
    Map<String, String> queryParams = [:]
    String query = exchange.requestURI.query
    if (query) {
      QueryParamParser.parseQueryParams(query, queryParams)
    }

    Headers headers = new Headers()
    exchange.requestHeaders.each { key, value -> headers[key] = value.join(', ') }
    return new HttpRequest(exchange.requestMethod, exchange.requestURI.path, queryParams, exchange.protocol, headers.asImmutable(), exchange.requestBody)
  }

  @Override
  List<EndpointDefinition> findPathEndpoints(String path) {
    // we know the path will always be the same as this handler if for a specific static path
    return pathEndpointDefinitions
  }
}
