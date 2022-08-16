package com.tambapps.http.garcon

import com.tambapps.http.garcon.exception.ParsingException
import com.tambapps.http.garcon.util.ContentTypeFunctionMap

/**
 * Context used for endpoint definition closures, as delegate
 */
class HttpExchangeContext {

  // definition order matters because of @delegate
  @Delegate
  final HttpResponse response
  @Delegate
  final HttpRequest request
  final ContentTypeFunctionMap<Object, byte[]> composers
  final ContentTypeFunctionMap<byte[], Object> parsers
  final ContentType contentType
  final ContentType accept
  private Object parsedBody

  HttpExchangeContext(HttpRequest request, HttpResponse response, ContentTypeFunctionMap<Object, byte[]> composers,
                      ContentTypeFunctionMap<byte[], Object> parsers, ContentType contentType, ContentType accept) {
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
          try {
            b = parser.apply(request.getBody())
          } catch (Exception e) {
            throw new ParsingException(e)
          }
        } else {
          b = request.getBody()
        }
      }
      this.@parsedBody = b
    }
    return this.@parsedBody
  }

}

