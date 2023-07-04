package com.tambapps.http.garcon.util;

import java.io.Closeable;
import java.io.IOException;

public class IoUtils {

  public static void closeQuietly(Closeable c) {
    if (c != null) {
      try {
        c.close();
      } catch (IOException e) {
        /* ignore */
      }
    }
  }
}
