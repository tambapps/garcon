package com.tambapps.http.garcon.io;

import com.tambapps.http.garcon.exception.StreamTooLongException;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.io.InputStream;

// TODO test max request size
/**
 * InputStream that throws an exception if the number of bytes read exceed a provided length
 */
@RequiredArgsConstructor
public class LimitedInputStream extends InputStream {

  private long bytesRead = 0;

  private final InputStream inputStream;
  private final long maxBytes;


  @Override
  public int read() throws IOException {
    int r = inputStream.read();
    bytesRead++;
    checkBytesRead();
    return r;
  }

  @Override
  public int read(byte[] b) throws IOException {
    int r = inputStream.read(b);
    if (r > 0) {
      bytesRead += r;
      checkBytesRead();
    }
    return r;
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    int r = inputStream.read(b, off, len);
    if (r > 0) {
      bytesRead += r;
      checkBytesRead();
    }
    return r;
  }

  public void resetBytesRead() {
    bytesRead = 0;
  }

  private void checkBytesRead() throws IOException {
    if (bytesRead > maxBytes) {
      resetBytesRead();
      throw new StreamTooLongException();
    }
  }
}
