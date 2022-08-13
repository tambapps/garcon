package com.tambapps.http.garcon.io.parser;

import com.tambapps.http.garcon.HttpRequest;
import com.tambapps.http.garcon.exception.BadProtocolException;
import com.tambapps.http.garcon.io.ByteBufferReader;

import java.nio.ByteBuffer;

public class HttpRequestParser {


  HttpRequest.HttpRequestBuilder builder = HttpRequest.builder();
  // return true if the whole request has been parsed
  public boolean parse(ByteBuffer buffer) {
    ByteBufferReader reader = new ByteBufferReader();
    String line = reader.readLine(buffer);
    int firstSpace = line.indexOf(' ');
    int secondSpace = line.indexOf(' ', firstSpace + 1);
    if (secondSpace < 0) {
      throw new BadProtocolException("Malformed first line");
    }
    builder.method(line.substring(0, firstSpace));
    String pathWithParams = line.substring(firstSpace + 1, secondSpace);
    int queryStart = pathWithParams.indexOf('?');
    if (queryStart < 0) {
      builder.path(pathWithParams);
    } else if (queryStart == pathWithParams.length() - 1) {
      builder.path(pathWithParams.substring(0, pathWithParams.length() - 1));
    } else {
      builder.path(pathWithParams.substring(0, queryStart));
      builder.queryParams(QueryParamParser.parseQueryParams(pathWithParams.substring(queryStart + 1)));
    }
    // TODO parse headers
    return true;
  }

  public HttpRequest getRequest() {
    return builder.build();
  }
}
