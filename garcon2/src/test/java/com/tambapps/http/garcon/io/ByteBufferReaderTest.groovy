package com.tambapps.http.garcon.io

import org.junit.Test

import java.nio.ByteBuffer

import static org.junit.jupiter.api.Assertions.assertArrayEquals
import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertNull

class ByteBufferReaderTest {


  @Test
  void testReadLines() {
    def buffer = ByteBuffer.wrap('Hello World\r\nHello'.bytes)
    ByteBufferReader reader = new ByteBufferReader()
    assertEquals('Hello World', reader.readLine(buffer))
    // doesn't end with a line return, so the line is not full
    assertNull(reader.readLine(buffer))
    assertArrayEquals('Hello'.bytes, reader.lineStart)
    assertEquals('Hello World', reader.readLine(ByteBuffer.wrap(' World\r\n'.bytes)))
  }

  @Test
  void testReadEmptyLine() {
    def buffer = ByteBuffer.wrap("\r\nHello\nWorld\r\n".bytes)
    ByteBufferReader reader = new ByteBufferReader()
    assertEquals('', reader.readLine(buffer))
    assertEquals('Hello\nWorld', reader.readLine(buffer))
  }
}
