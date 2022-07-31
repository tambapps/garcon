package com.tambapps.http.garcon.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tambapps.http.garcon.Headers;
import com.tambapps.http.garcon.HttpRequest;
import com.tambapps.http.garcon.exception.RequestParsingException;
import com.tambapps.http.garcon.util.IoUtils;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class RequestParserTest {

  private final RequestParser requestParser = new RequestParser();

  @Test
  public void testParseRequest() throws IOException {
    String input = "GET /page.html?hello=world&bool&third=wheel HTTP/1.0\r\n"
        + "Host: example.com\r\n"
        + "Referer: http://example.com/\r\n"
        + "User-Agent: CERN-LineMode/2.15 libwww/2.17b3\r\n\r\n";

    HttpRequest request = requestParser.parseInputStream(toInputStream(input));
    assertEquals("HTTP/1.0", request.getHttpVersion());
    assertEquals("GET", request.getMethod());
    assertEquals("/page.html", request.getPath());

    assertEquals(new Headers() {{
      putAt("Host", "example.com");
      putAt("Referer", "http://example.com/");
      putAt("User-Agent", "CERN-LineMode/2.15 libwww/2.17b3");
    }}, request.getHeaders());
    assertEquals(new HashMap<String, String>() {{
      put("hello", "world");
      put("bool", "true");
      put("third", "wheel");
    }}, request.getQueryParams());
  }

  @Test
  public void testParseRequest_withBody() throws IOException {
    String input = "POST /page HTTP/1.0\r\n"
        + "\r\nHello World";

    HttpRequest request = requestParser.parseInputStream(toInputStream(input));
    assertEquals("HTTP/1.0", request.getHttpVersion());
    assertEquals("POST", request.getMethod());
    assertEquals("/page", request.getPath());
    assertEquals(new Headers(), request.getHeaders());
    assertEquals("Hello World", new String(IoUtils.readAllBytes(request.getRequestBody())));
  }

  @Test
  public void testParseRequest_invalidRequests() {
    List<String> inputs = Arrays.asList(
        "GET /page.html HTTP/1.0\r\n\n",
        "GET /page.htmlHTTP/1.0\r\n\n",
        "GET /page.htmlHTTP/1.0 yaho\r\n\n",
        "GET HTTP/1.0\r\n\n",
        "GET /page.html HTTP/1.0\r\n"
    );

    for (String input: inputs) {
      Exception exception =
          assertThrows(Exception.class, () -> requestParser.parseInputStream(toInputStream(input)));
      assertTrue(exception.getClass() == RequestParsingException.class || exception.getClass() == EOFException.class);
    }
  }
  private InputStream toInputStream(String s) {
    return new ByteArrayInputStream(s.getBytes());
  }
}
