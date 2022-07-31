package com.tambapps.http.garcon.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class IoUtils {
  private static final int DEFAULT_BUFFER_SIZE = 8192; // 8k

  public static void write(InputStream in, OutputStream os) throws IOException {
    byte[] buf = new byte[DEFAULT_BUFFER_SIZE];

    for (int count; in.available() > 0 && -1 != (count = in.read(buf)); ) {
      os.write(buf, 0, count);
    }
  }

  public static byte[] readAllBytes(InputStream is) throws IOException {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    write(is, os);
    return os.toByteArray();
  }
}
