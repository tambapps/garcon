package com.tambapps.http.garcon.io;

import com.tambapps.http.garcon.HttpRequest;
import com.tambapps.http.garcon.ImmutableHeaders;
import com.tambapps.http.garcon.exception.RequestParsingException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

// TODO test this
// https://fr.wikipedia.org/wiki/Hypertext_Transfer_Protocol#Impl%C3%A9mentation
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RequestParser {

  public static HttpRequest parse(InputStream is) throws IOException {
    return new RequestParser(new BufferedReader(new InputStreamReader(is))).parseInputStream(is);
  }

  private final BufferedReader reader;
  private HttpRequest parseInputStream(InputStream is) throws IOException {
    String[] firstFields = readLine().split("\\s");
    if (firstFields.length != 3) {
      throw new RequestParsingException("Request command is invalid");
    }
    String method = firstFields[0];
    String pathWithParams = firstFields[1];
    String httpVersion = firstFields[2];

    String line;
    Map<String, String> headers = new HashMap<>();
    // empty line is delimiter between headers and request body
    while ((line = readLine()) != null && !line.isEmpty()) {
      String[] headerFields = line.split(":", 2);
      if (headerFields.length != 2) {
        throw new RequestParsingException("Request command is invalid");
      }
      headers.put(headerFields[0], headerFields[1]);
    }
    InputStream body = null;
    if (is.available() > 0) {
      body = is;
    }
    Map<String, String> queryParams = new HashMap<>();
    String path = extractQueryParams(pathWithParams, queryParams);
    return new HttpRequest(method, path, queryParams, httpVersion, new ImmutableHeaders(headers), body);
  }

  private String readLine() throws IOException {
    String line = reader.readLine();
    if (line == null) {
      // connection was probably closed
      throw new EOFException();
    }
    return line;
  }
  private static String extractQueryParams(String pathWithParams, Map<String, String> queryParams) {
    if (pathWithParams == null || pathWithParams.isEmpty()) {
      return pathWithParams;
    }
    int start = pathWithParams.indexOf("?");
    if (start < 0) {
      return pathWithParams;
    }
    String paramsString = pathWithParams.substring(start + 1);
    String[] params = paramsString.split("&");
    for (String param : params) {
      String[] fields = param.split("=");
      if (fields.length == 2) {
        queryParams.put(urlDecode(fields[0]), urlDecode(fields[1]));
      } else {
        queryParams.put(urlDecode(fields[0]), null);
      }
    }
    return pathWithParams.substring(0, start);
  }

  private static String urlDecode(String s) {
    try {
      return URLDecoder.decode(s, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException("Couldn't URL decode", e);
    }
  }
}
