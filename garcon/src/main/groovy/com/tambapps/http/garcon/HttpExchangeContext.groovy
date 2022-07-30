package com.tambapps.http.garcon

import com.tambapps.http.garcon.util.ContentTypeMap

/**
 * Context used for endpoint definition closures, as delegate
 */
class HttpExchangeContext {

  // definition order matters because of @delegate
  @Delegate
  final HttpResponse response
  @Delegate
  final HttpRequest request
  private final ContentTypeMap<Closure<?>> composers
  private final ContentTypeMap<Closure<?>> parsers
  final ContentType accept

  HttpExchangeContext(HttpRequest request, HttpResponse response, ContentTypeMap<Closure<?>> composers,
                      ContentTypeMap<Closure<?>> parsers, ContentType accept) {
    this.request = request
    this.response = response
    this.composers = composers
    this.parsers = parsers
    this.accept = accept
  }

  Headers getRequestHeaders() {
    return request.headers
  }
  Headers getResponseHeaders() {
    return response.headers
  }

  def getParsedRequestBody() {
    if (accept == null) {
      return request.requestBody
    } else {
      def parser = parsers[accept]
      if (parser) {
        return parser.call(request.requestBody)
      } else {
        return request.requestBody
      }
    }
  }

  // called when method is not found
  def invokeMethod(String name, Object o) {
    Object[] args = o instanceof Object[] ? o : new Object[] { o }
    if (args.length == 1) {
      def contentType = composers.keySet().find { it.subtype == name }
      if (contentType != null) {
        response.headers.putContentTypeHeader(contentType.headerValue)
        // set response body here so that the HttpExchangeHandler doesn't compose it again
        response.body = composers[contentType].call(args[0])
        return response.body
      }
    }
    throw new MissingMethodException(name, getClass(), args)
  }
}

