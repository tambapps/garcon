package com.tambapps.http.garcon.io.parser;

import com.tambapps.http.garcon.Headers;
import com.tambapps.http.garcon.HttpRequest;
import com.tambapps.http.garcon.exception.BadProtocolException;
import com.tambapps.http.garcon.exception.BadRequestException;
import com.tambapps.http.garcon.io.ByteBufferReader;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Map;

public class HttpRequestParser {

  enum ParsingState {
    COMMAND_LINE,
    HEADERS,
    BODY,
    COMPLETE
  }
  ParsingState state;
  final ByteBufferReader reader = new ByteBufferReader();
  final Headers headers = new Headers();
  String method;
  String path;
  String protocolVersion;
  Map<String, String> queryParams;
  BodyParser bodyParser;

  public HttpRequestParser() {
    reset();
  }
  // return true if the whole request has been parsed
  public boolean parse(ByteBuffer buffer) {
    if (state == ParsingState.COMPLETE) {
      return true;
    }

    switch (state) {
      case COMMAND_LINE:
        String commandLine = reader.readLine(buffer);
        if (commandLine == null) {
          return false;
        }
        parseCommand(commandLine);
        state = ParsingState.HEADERS;
      case HEADERS:
        String headerLine = null;
        while (state == ParsingState.HEADERS && (headerLine = reader.readLine(buffer)) != null) {
          if (headerLine.isEmpty()) {
            state = ParsingState.BODY;
          } else {
            parseHeader(headers, headerLine);
          }
        }
        if (headerLine == null && state == ParsingState.HEADERS) {
          return false;
        }
        if ("get".equalsIgnoreCase(method) || "head".equalsIgnoreCase(method)) {
          // no request body to parse for GET or HEAD according to the RFC
          state = ParsingState.COMPLETE;
          return true;
        }
      case BODY:
        if (bodyParser == null) {
          Long contentLength = headers.getContentLength();
          String transferEncoding = headers.get(Headers.TRANSFER_ENCODING_HEADER);
          if (contentLength == null && transferEncoding == null || Long.valueOf(0L).equals(contentLength)) {
            state = ParsingState.COMPLETE;
            return true;
          }
          boolean isRequestBodyChunked = "chunked".equalsIgnoreCase(transferEncoding);
          if (isRequestBodyChunked && contentLength != null) {
            // TODO catch me
            throw new BadRequestException("Cannot have both a content length and a chunked request encoding");
          } else if (isRequestBodyChunked) {
            bodyParser = new ChunkedBodyParser();
          } else if (contentLength != null) {
            bodyParser = new ContentLengthBodyParser(contentLength.intValue());
          }
        }
        if (bodyParser.parse(buffer)) {
          state = ParsingState.COMPLETE;
          return true;
        }
    }
    return false;
  }

  private void parseHeader(Headers headers, String line) {
    int separatorIndex = line.indexOf(": ");
    if (separatorIndex < 0) {
      // TODO catch me
      throw new BadProtocolException("Malformed header");
    }
    // + 2 because the string we searched has a length of 2
    headers.put(line.substring(0, separatorIndex), line.substring(separatorIndex + 2));
  }

  private void parseCommand(String line) {
    int firstSpace = line.indexOf(' ');
    int secondSpace = line.indexOf(' ', firstSpace + 1);
    if (secondSpace < 0) {
      throw new BadProtocolException("Malformed first line");
    }
    method = line.substring(0, firstSpace);
    String pathWithParams = line.substring(firstSpace + 1, secondSpace);
    int queryStart = pathWithParams.indexOf('?');
    if (queryStart < 0) {
      path = pathWithParams;
    } else if (queryStart == pathWithParams.length() - 1) {
      path = pathWithParams.substring(0, pathWithParams.length() - 1);
    } else {
      path = pathWithParams.substring(0, queryStart);
      queryParams = QueryParamParser.parseQueryParams(pathWithParams.substring(queryStart + 1));
    }
    protocolVersion = line.substring(secondSpace + 1);
  }

  public HttpRequest getRequest() {
    return new HttpRequest(method, path, queryParams != null ? queryParams : Collections.emptyMap(), protocolVersion, headers.asImmutable(),
        bodyParser != null ? bodyParser.getBody() : null);
  }

  private interface BodyParser {

    // return true if finished parsing
    boolean parse(ByteBuffer buffer);

    byte[] getBody();
  }

  private static class ChunkedBodyParser implements BodyParser {

    @Override
    public boolean parse(ByteBuffer buffer) {
      // TODO
      return true;
    }

    @Override
    public byte[] getBody() {
      return null;
    }
  }
  private static class ContentLengthBodyParser implements BodyParser {

    public ContentLengthBodyParser(int contentLength) {
      this.body = new byte[contentLength];
      readRemaining = contentLength;
    }

    byte[] body;
    int readRemaining;
    int readCount = 0;

    @Override
    public boolean parse(ByteBuffer buffer) {
      int toRead = Math.min(buffer.remaining(), readRemaining);
      buffer.get(body, readCount, toRead);
      readRemaining -= toRead;
      readCount += toRead;
      return readCount >= body.length;
    }

    @Override
    public byte[] getBody() {
      return body;
    }
  }


  public void reset() {
    reader.clear();
    state = ParsingState.COMMAND_LINE;
    headers.clear();
    method = null;
    path = null;
    protocolVersion = null;
    queryParams = null;
    bodyParser = null;
  }
}
