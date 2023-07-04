package com.tambapps.http.garcon.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

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

  public static ObjectMapper newObjectMapper() {
    ObjectMapper mapper = new ObjectMapper();

    mapper.registerModule(new JavaTimeModule());
    mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    return mapper;
  }

  public static byte[] getBytes(InputStream is) throws IOException {
    ByteArrayOutputStream answer = new ByteArrayOutputStream();
    // reading the content of the stream within a byte buffer
    byte[] byteBuffer = new byte[8192];
    int nbByteRead /* = 0*/;
    try {
      while ((nbByteRead = is.read(byteBuffer)) != -1) {
        // appends buffer
        answer.write(byteBuffer, 0, nbByteRead);
      }
    } finally {
      closeQuietly(is);
    }
    return answer.toByteArray();
  }
}
