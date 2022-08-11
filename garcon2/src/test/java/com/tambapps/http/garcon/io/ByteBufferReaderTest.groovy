package com.tambapps.http.garcon.io

import com.tambapps.http.garcon.exception.EndOfBufferException
import org.junit.Test

import java.nio.ByteBuffer

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertThrows

class ByteBufferReaderTest {


  @Test
  void testReadLines() {
    def buffer = ByteBuffer.wrap("Hello World\r\nWorld".bytes)
    ByteBufferReader reader = new ByteBufferReader(buffer)
    assertEquals('Hello World', reader.readLine())
    assertEquals('World', reader.readLine())
    assertThrows(EndOfBufferException.class, reader.&readLine)
  }

  @Test
  void testReadEmptyLine() {
    def buffer = ByteBuffer.wrap("\r\nHello\nWorld\r\n".bytes)
    ByteBufferReader reader = new ByteBufferReader(buffer)
    assertEquals('', reader.readLine())
    assertEquals('Hello\nWorld', reader.readLine())
    assertThrows(EndOfBufferException.class, reader.&readLine)
  }
}
