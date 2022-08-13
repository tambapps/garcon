package com.tambapps.http.garcon.io.parser

import com.tambapps.http.garcon.Headers
import com.tambapps.http.garcon.HttpRequest
import com.tambapps.http.garcon.io.parser.HttpRequestParser
import org.junit.jupiter.api.Test

import java.nio.ByteBuffer

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertTrue

class HttpRequestParserTest {

  @Test
  void parseFullRequest() {
    def parser = new HttpRequestParser()
    assertTrue(parser.parse(ByteBuffer.wrap(
        ('GET /path?hello=world HTTP/1.1\r\n' +
            'Host: localhost:8081\r\n' +
            'Connection: Keep-Alive\r\n' +
            'Accept-Encoding: gzip\r\n' +
            'User-Agent: okhttp/4.9.3\r\n' +
            '\r\n').bytes
    ))
    )
    assertEquals(new HttpRequest('GET', '/path', [hello: 'world'], 'HTTP/1.1',
        new Headers([Host: 'localhost:8081', Connection: 'Keep-Alive',
                     ('Accept-Encoding'): 'gzip', ('User-Agent'): 'okhttp/4.9.3']), null),
        parser.getRequest())
  }

  @Test
  void parseFullRequest_multipleBuffers() {
    def parser = new HttpRequestParser()
    assertFalse(parser.parse(toBuffer('GET /path?hello=world HTTP/1.1\r\n' +
        'Host: localhost:8081\r\n')))
    assertEquals(HttpRequestParser.ParsingState.HEADERS, parser.state)
    assertFalse(parser.parse(toBuffer('Connection: Ke')))
    assertEquals(HttpRequestParser.ParsingState.HEADERS, parser.state)
    assertFalse(parser.parse(toBuffer('ep-Alive\r\n' +
        'Accept-Encodi')))
    assertEquals(HttpRequestParser.ParsingState.HEADERS, parser.state)
    assertTrue(parser.parse(toBuffer(
        'g: gzip\r\n' +
        'User-Agent: okhttp/4.9.3\r\n' +
            '\r\n'
    )))
    assertEquals(HttpRequestParser.ParsingState.COMPLETE, parser.state)

    assertEquals(new HttpRequest('GET', '/path', [hello: 'world'], 'HTTP/1.1',
        new Headers([Host: 'localhost:8081', Connection: 'Keep-Alive',
                     ('Accept-Encoding'): 'gzip', ('User-Agent'): 'okhttp/4.9.3']), null),
        parser.getRequest())
  }

  private static ByteBuffer toBuffer(String s) {
    return ByteBuffer.wrap(s.bytes)
  }
}
