package com.tambapps.http.garcon;

import com.tambapps.http.garcon.util.IoUtils;
import lombok.Data;
import lombok.SneakyThrows;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

@Data
public class HttpResponse {

  final String httpVersion = "HTTP/1.1";
  HttpStatusCode statusCode = HttpStatus.OK;
  final Headers headers = new Headers();

  // body can be a byte array, a string, or an input stream
  /**
   * Body of the response. Can be a byte array, a String, or an InputStream (or null for no body).
   * Volatile because the thread setting it is different from the thread getting it to write it into
   * the response.
   */
  volatile ByteBuffer body;

  public boolean is2xxSuccessful() {
    return statusCode.getValue() >= 200 && statusCode.getValue() < 300;
  }

  @SneakyThrows
  public void setBody(Object body) {
    if (body == null) {
      this.body = null;
      return;
    }
    if (body instanceof ByteBuffer) {
      this.body = (ByteBuffer) body;
    } else // for Groovy
      if (body instanceof byte[]) {
      this.body = ByteBuffer.wrap(((byte[]) body));
    } else if (body instanceof String
          // for groovy
          || body.getClass().getName().equals("groovy.lang.GString")) {
      this.body = ByteBuffer.wrap(body.toString().getBytes());
    } else if (body instanceof InputStream) {
      this.body = ByteBuffer.wrap(getBytes(((InputStream) body)));
    } else {
      throw new IllegalArgumentException("Cannot handle body of type " + body.getClass().getSimpleName());
    }
  }
  public boolean isIndefiniteLength() {
    return getContentLength() == null;
  }

  public Integer getContentLength() {
    return body != null ? body.capacity() : null;
  }

  public boolean isKeepAlive() {
    return Headers.CONNECTION_KEEP_ALIVE.equalsIgnoreCase(headers.get(Headers.CONNECTION_HEADER));
  }

  private static byte[] getBytes(InputStream is) throws IOException {
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
      IoUtils.closeQuietly(is);
    }
    return answer.toByteArray();
  }
}
