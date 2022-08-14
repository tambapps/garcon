package com.tambapps.http.garcon.io;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class ByteBufferReader {

  private static final byte CARRIAGE_RETURN = '\r';
  private static final byte LINE_BREAK = '\n';

  byte[] lineStart = null;
  // line separator is CR followed by LF
  public String readLine(ByteBuffer buffer) {
    byte b;
    boolean readCr = lineStart != null && lineStart.length > 0 && lineStart[0] == CARRIAGE_RETURN;
    int startIndex = buffer.position();
    int index = 0;
    while (buffer.hasRemaining()) {
      b = buffer.get();
      index++;
      if (b == CARRIAGE_RETURN) {
        readCr = true;
      } else if (readCr && b == LINE_BREAK) {
        return newString(buffer, startIndex, index - 2);
      }
    }
    // line is not full. Will wait for a next call with a different buffer to return the full line
    if (buffer.position() > startIndex) {
      // index is the length because at the end of
      if (lineStart == null) {
        lineStart = readBytes(buffer, startIndex, index);
      } else {
        byte[] newLineStart = Arrays.copyOf(lineStart, lineStart.length + index);
        buffer.position(startIndex);
        buffer.get(newLineStart, lineStart.length, index);
        this.lineStart = newLineStart;
      }
    }
    return null;
  }

  private byte[] readBytes(ByteBuffer buffer, int bufferStartIndex, int length) {
    byte[] bytes = new byte[length];
    buffer.position(bufferStartIndex);
    buffer.get(bytes, 0, bytes.length);
    return bytes;
  }

  private String newString(ByteBuffer buffer, int bufferStartIndex, int length) {
    // + 1 because index, - 2 to remove CR and LB
    byte[] bytes;
    if (lineStart != null) {
      bytes = Arrays.copyOf(lineStart, lineStart.length + length);
      buffer.position(bufferStartIndex);
      buffer.get(bytes, lineStart.length, length);
      lineStart = null;
    } else {
      bytes = readBytes(buffer, bufferStartIndex, length);
    }

    // don't include line return in sting
    buffer.position(buffer.position() + 2);
    return new String(bytes);
  }

  public void clear() {
    lineStart = null;
  }
}
