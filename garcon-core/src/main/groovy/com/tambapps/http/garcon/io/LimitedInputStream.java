package com.tambapps.http.garcon.io;

import com.tambapps.http.garcon.exception.StreamTooLongException;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.io.InputStream;

// TODO test max request size
/**
 * InputStream that throws an exception if the number of bytes read exceed a provided length
 */
public class LimitedInputStream extends InputStream {

  private final InputStream inputStream;
  @Setter
  @Getter
  private Long maxBytes;
  private long bytesRead = 0;

  public LimitedInputStream(InputStream inputStream, long maxBytes) {
    this.inputStream = inputStream;
    this.maxBytes = maxBytes;
  }



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
    if (maxBytes != null && bytesRead > maxBytes) {
      resetBytesRead();
      throw new StreamTooLongException();
    }
  }
}
