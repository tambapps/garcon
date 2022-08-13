package com.tambapps.http.garcon.io;

import com.tambapps.http.garcon.exception.EndOfBufferException;
import lombok.AllArgsConstructor;

import java.nio.ByteBuffer;

@AllArgsConstructor
public class ByteBufferReader {

  private static final byte CARRIAGE_RETURN = '\r';
  private static final byte LINE_BREAK = '\n';

  // line separator is CR followed by LF
  public String readLine(ByteBuffer buffer) {
    byte b;
    boolean readCr = false;
    int startIndex = buffer.position();
    int index = 0;
    while (buffer.hasRemaining()) {
      b = buffer.get();
      index++;
      if (b == CARRIAGE_RETURN) {
        readCr = true;
      } else if (readCr && b == LINE_BREAK) {
        return newString(buffer, startIndex, index - 2, true);
      }
    }
    if (buffer.position() == startIndex) {
      // was empty to begin with? end of file
      throw new EndOfBufferException();
    }
    return newString(buffer, startIndex, index, false);
  }

  private String newString(ByteBuffer buffer, int bufferStartIndex, int length, boolean skipLineReturn) {
    // + 1 because index, - 2 to remove CR and LB
    byte[] bytes = new byte[length];
    buffer.position(bufferStartIndex);
    buffer.get(bytes, 0, bytes.length);
    if (skipLineReturn) {
      buffer.position(buffer.position() + 2);
    }
    return new String(bytes);
  }
}
