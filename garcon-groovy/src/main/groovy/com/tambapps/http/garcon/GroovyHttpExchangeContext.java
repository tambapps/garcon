package com.tambapps.http.garcon;

import com.tambapps.http.garcon.util.ContentTypeFunctionMap;
import groovy.lang.MissingPropertyException;

class GroovyHttpExchangeContext extends HttpExchangeContext {
  GroovyHttpExchangeContext(HttpRequest request, HttpResponse response, ContentTypeFunctionMap<Object, byte[]> composers, ContentTypeFunctionMap<byte[], Object> parsers, ContentType contentType, ContentType accept) {
    super(request, response, composers, parsers, contentType, accept);
  }


  // useful for meta programming
  public Object propertyMissing(String propertyName) {
    if (getPathVariables() != null) {
      String s = getPathVariables().get(propertyName);
      if (s != null) {
        return s;
      }
    }
    String q = getQueryParams().get(propertyName);
    if (q != null) {
      return q;
    }
    String reqH = getRequestHeaders().get(propertyName);
    if (reqH != null) {
      return reqH;
    }
    String resH = getResponseHeaders().get(propertyName);
    if (resH != null) {
      return resH;
    }
    throw new MissingPropertyException(propertyName, getClass());
  }
}
