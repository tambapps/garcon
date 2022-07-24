package com.tambapps.http.garcon

import groovy.transform.Immutable

@Immutable
class QueryParam {
  String key
  String value

  @Override
  String toString() {
    return key + "=" + value
  }

  /**
   * Returns this query parameter URL encoded
   * @return this query parameter URL encoded
   */
  public String encoded() {
    return urlEncode(key) + "=" + urlEncode(value)
  }

  private static String urlEncode(Object o) {
    return URLEncoder.encode(String.valueOf(o), "UTF-8")
  }
}
