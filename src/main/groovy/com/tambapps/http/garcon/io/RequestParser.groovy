package com.tambapps.http.garcon.io

import com.tambapps.http.garcon.HttpRequest
import com.tambapps.http.garcon.ImmutableHeaders
import com.tambapps.http.garcon.exception.RequestParsingException

// https://fr.wikipedia.org/wiki/Hypertext_Transfer_Protocol#Impl%C3%A9mentation
class RequestParser {

  HttpRequest parse(InputStream is) {
    BufferedReader reader = is.newReader()
    def (method, path, httpVersion) = parseCommand(reader.readLine())
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
    return new HttpRequest(method: method, path: path, httpVersion: httpVersion, headers: new ImmutableHeaders(headers), body: body)
  }

  private String[] parseCommand(String command) {
    if (command == null) {
      throw new EOFException('End of stream')
    }
    def fields = command.split(/\s/)
    if (fields.size() != 3) {
      throw new RequestParsingException('Request command is invalid')
    }
    return fields
  }
}
