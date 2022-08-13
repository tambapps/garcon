package com.tambapps.http.garcon;

import lombok.Value;

import java.util.Map;

@Value
public class HttpRequest {
  String method;
  String path;
  Map<String, String> queryParams;
  String httpVersion;
  Headers headers;
  byte[] body;
}
