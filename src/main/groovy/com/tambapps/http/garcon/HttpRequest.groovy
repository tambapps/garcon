package com.tambapps.http.garcon

import groovy.transform.Immutable

@Immutable
class HttpRequest {
  String method
  String path
  Map<String, String> queryParams
  String httpVersion
  Headers headers
  // TODO make body lazy and allow to read it as a stream
  byte[] body
}
