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
  final ContentTypeMap<Closure<?>> composers
  final ContentTypeMap<Closure<?>> parsers
  final ContentType contentType
  final ContentType accept
  private Object parsedBody

  HttpExchangeContext(HttpRequest request, HttpResponse response, ContentTypeMap<Closure<?>> composers,
                      ContentTypeMap<Closure<?>> parsers, ContentType contentType, ContentType accept) {
    this.request = request
    this.response = response
    this.composers = composers
    this.parsers = parsers
    this.contentType = contentType
    this.accept = accept
  }

  Headers getRequestHeaders() {
    return request.getHeaders()
  }
  Headers getResponseHeaders() {
    return response.getHeaders()
  }

  def getParsedRequestBody() {
    return getParsedRequestBody(accept)
  }

  def getParsedRequestBody(ContentType accept) {
    if (request.getBody() == null) {
      return null
    }
    if (this.@parsedBody == null) {
      def b
      if (accept == null) {
        b = request.getBody()
      } else {
        def parser = parsers[accept]
        if (parser != null) {
          b = parser.call(request.getBody())
        } else {
          b = request.getBody()
        }
      }
      this.@parsedBody = b
    }
    return this.@parsedBody
  }

  // called when method is not found
  def invokeMethod(String name, Object o) {
    Object[] args = o instanceof Object[] ? o : new Object[] { o }
    if (args.length == 1) {
      def contentType = composers.keySet().find { it.getSubtype() == name }
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

