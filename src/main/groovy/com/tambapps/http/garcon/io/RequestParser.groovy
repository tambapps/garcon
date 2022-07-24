package com.tambapps.http.garcon.io

import com.tambapps.http.garcon.HttpRequest
import com.tambapps.http.garcon.ImmutableHeaders
import com.tambapps.http.garcon.QueryParam
import com.tambapps.http.garcon.exception.RequestParsingException

// https://fr.wikipedia.org/wiki/Hypertext_Transfer_Protocol#Impl%C3%A9mentation
class RequestParser {

  static HttpRequest parse(InputStream is) {
    BufferedReader reader = is.newReader()
    def (method, pathWithParams, httpVersion) = parseCommand(reader.readLine())
    String line
    Map<String, String> headers = [:]
    while ((line = reader.readLine())) {
      def (headerName, headerValue) = line.split(/:/)
      headers[headerName] = headerValue.trim()
    }
    byte[] body = null
    if (is.available()) {
      body = is.bytes
    }
    def (String path, List<QueryParam> queryParams) = extractQueryParams(pathWithParams)
    return new HttpRequest(method: method, path: path, queryParams: queryParams,
        httpVersion: httpVersion, headers: new ImmutableHeaders(headers), body: body)
  }

  private static String[] parseCommand(String command) {
    if (command == null) {
      throw new EOFException('End of stream')
    }
    def fields = command.split(/\s/)
    if (fields.size() != 3) {
      throw new RequestParsingException('Request command is invalid')
    }
    return fields
  }

  private static List extractQueryParams(String path) {
    if (path == null || path.isEmpty()) {
      return [path, []]
    }
    int start = path.indexOf("?")
    if (start < 0 || start >= path.length() - 1) {
      return [path, []]
    }
    String paramsString = path.substring(start + 1)
    String[] params = paramsString.split("&")
    List<QueryParam> queryParams = []
    for (String param : params) {
      String[] fields = param.split("=")
      if (fields.length == 2) {
        queryParams.add(new QueryParam(key: urlDecode(fields[0]), value: urlDecode(fields[1])))
      }
    }
    return [path.substring(0, start), queryParams]
  }

  private static String urlDecode(String s) {
    try {
      return URLDecoder.decode(s, "UTF-8")
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException("Couldn't URL decode", e)
    }
  }
}
