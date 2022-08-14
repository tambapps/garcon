package com.tambapps.http.garcon.io.parser

import com.tambapps.http.garcon.Headers
import com.tambapps.http.garcon.HttpRequest
import com.tambapps.http.garcon.io.parser.HttpRequestParser
import groovy.transform.CompileDynamic
import org.junit.jupiter.api.Test

import java.nio.ByteBuffer

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertFalse
import static org.junit.jupiter.api.Assertions.assertTrue

// needed because project is compiled statically by default
@CompileDynamic
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
    assertFalse(parser.parse(toBuffer('Connec')))
    assertFalse(parser.parse(toBuffer('tion: Ke')))
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

  @Test
  void parseRequest_withBody() {
    def parser = new HttpRequestParser()
    String body = 'hello world'
    int bodyLength = body.bytes.length
    assertTrue(parser.parse(ByteBuffer.wrap(
        ('POST /path?hello=world HTTP/1.1\r\n' +
            'Content-Length: ' + bodyLength + '\r\n' +
            '\r\n' + body).bytes
    )))
    assertEquals(new HttpRequest('POST', '/path', [hello: 'world'], 'HTTP/1.1',
        new Headers([('Content-Length'): bodyLength]), body.bytes),
        parser.getRequest())
  }

  @Test
  void parseRequest_withBodyTwoParts() {
    def parser = new HttpRequestParser()
    String bodyPart1 = 'hello '
    String bodyPart2 = 'world'
    int bodyLength = bodyPart1.bytes.length + bodyPart2.bytes.length
    assertFalse(parser.parse(toBuffer(
        'PUT /path?hello=world HTTP/1.1\r\n' +
            'Content-Length: ' + bodyLength + '\r\n' +
            '\r\n' + bodyPart1
    )))
    assertTrue(parser.parse(toBuffer(bodyPart2)))
    assertEquals(new HttpRequest('PUT', '/path', [hello: 'world'], 'HTTP/1.1',
        new Headers([('Content-Length'): bodyLength]), (bodyPart1 + bodyPart2).bytes),
        parser.getRequest())
  }

  private static ByteBuffer toBuffer(String s) {
    return ByteBuffer.wrap(s.bytes)
  }
}
