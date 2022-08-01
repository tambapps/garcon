package com.tambapps.http.garcon.io;

import com.tambapps.http.garcon.HttpRequest;
import com.tambapps.http.garcon.ImmutableHeaders;
import com.tambapps.http.garcon.exception.RequestParsingException;
import lombok.AllArgsConstructor;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

// https://fr.wikipedia.org/wiki/Hypertext_Transfer_Protocol#Impl%C3%A9mentation
@AllArgsConstructor
public class RequestParser {

  public static HttpRequest parse(InputStream is) throws IOException {
    String[] firstFields = readLine(is).split("\\s");
    if (firstFields.length != 3) {
      throw new RequestParsingException("Request command is invalid");
    }
    String method = firstFields[0];
    String pathWithParams = firstFields[1];
    String httpVersion = firstFields[2];

    String line;
    Map<String, String> headersMap = new HashMap<>();
    // empty line is delimiter between headers and request body
    while (!(line = readLine(is)).isEmpty()) {
      String[] headerFields = line.split(":\\s", 2);
      if (headerFields.length != 2) {
        throw new RequestParsingException("Request command is invalid");
      }
      headersMap.put(headerFields[0], headerFields[1]);
    }

    ImmutableHeaders headers = new ImmutableHeaders(headersMap);
    Long contentLength = headers.getContentLength();
    InputStream body = contentLength != null ? new BoundedInputStream(is, contentLength) : is;
    Map<String, String> queryParams = new HashMap<>();
    String path = extractQueryParams(pathWithParams, queryParams);
    return new HttpRequest(method, path, queryParams, httpVersion, new ImmutableHeaders(headers), body);
  }

  private static String readLine(InputStream in) throws IOException {
    // according to HTTP spec, separator should always be '\r\n': https://www.rfc-editor.org/rfc/rfc2616#section-2.2
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    int lastChar1 = in.read();
    if (lastChar1 < 0) {
      throw new EOFException();
    }
    int lastChar2 = in.read();
    if (lastChar2 < 0) {
      throw new EOFException();
    }

    int b;
    while (lastChar1 != '\r' && lastChar2 != '\n' && ((b = in.read()) > 0)) {
      bos.write(lastChar1);
      lastChar1 = lastChar2;
      lastChar2 = (byte) b;
    }
    return bos.toString();
  }

  private static String extractQueryParams(String pathWithParams, Map<String, String> queryParams) throws IOException {
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
      } else if (fields.length == 1) {
        queryParams.put(urlDecode(fields[0]), String.valueOf(true));
      }
    }
    return pathWithParams.substring(0, start);
  }

  private static String urlDecode(String s) throws IOException {
    try {
      return URLDecoder.decode(s, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new RequestParsingException("Couldn't URL decode", e);
    }
  }
}
