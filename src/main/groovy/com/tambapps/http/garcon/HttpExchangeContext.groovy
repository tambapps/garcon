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

  HttpExchangeContext(HttpRequest request, HttpResponse response, ContentTypeMap<Closure<?>> composers) {
    this.request = request
    this.response = response
    this.composers = composers
  }

  Headers getRequestHeaders() {
    return request.headers
  }
  Headers getResponseHeaders() {
    return response.headers
  }

  // called when method is not found
  def invokeMethod(String name, Object o) {
    Object[] args = o instanceof Object[] ? o : new Object[] { o }
    if (args.length == 1) {
      def contentType = composers.keySet().find { it.subtype == name }
      if (contentType != null) {
        return composers[contentType].call(args[0])
      }
    }
    throw new MissingMethodException(name, getClass(), args)
  }
}

