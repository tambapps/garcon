package com.tambapps.http.garcon;

import lombok.Data;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

@Data
public class HttpResponse {
  private static final int DEFAULT_BUFFER_SIZE = 8192; // 8k

  final String httpVersion = "HTTP/1.1";
  HttpStatusCode statusCode;
  Headers headers = new Headers();

  // body can be a byte array, a string, or an input stream
  /**
   * Body of the response. Can be a byte array, a String, or an InputStream (or null for no body)
   */
  Object body;

  public boolean is2xxSuccessful() {
    return statusCode.getValue() >= 200 && statusCode.getValue() < 300;
  }
  void setBody(Object body) {
    if (body == null) {
      return;
    }
    if (!(body instanceof byte[]) && !(body instanceof String) && !(body instanceof InputStream)) {
      throw new IllegalStateException(String.format("Cannot handle body of type %s", body.getClass()));
    }
    this.body = body;
  }
  boolean isIndefiniteLength() {
    return getContentLength() == null;
  }

  Long getContentLength() {
    if (body == null) {
      return 0L;
    } else if (body instanceof byte[]) {
      return (long) ((byte[]) body).length;
    } else if (body instanceof String) {
      return (long) ((String) body).length();
    } else if (body instanceof InputStream) {
      return null;
    } else {
      throw new IllegalStateException("Cannot handle body of type " + body.getClass());
    }
  }

  void writeInto(OutputStream os) throws IOException {
    PrintWriter writer = new PrintWriter(os);
    writer.format("%s %d %s", httpVersion, statusCode.getValue(), statusCode.getMessage()).println();
    headers.forEach((name, value) -> writer.println(name + ": " + value));
    writer.println();
    writer.flush();
    writeBody(os);
  }

  private void writeBody(OutputStream os) throws IOException {
    if (body == null) {
      return;
    }
    if (body instanceof byte[]) {
      os.write((byte[]) body);
    } else if (body instanceof String) {
      os.write(((String) body).getBytes());
    } else if (body instanceof InputStream) {
      InputStream in = (InputStream) body;
      byte[] buf = new byte[DEFAULT_BUFFER_SIZE];
      for (int count; -1 != (count = in.read(buf)); ) {
        os.write(buf, 0, count);
      }
    } else {
      throw new IllegalStateException("Cannot handle body of type " + body.getClass());
    }
    os.flush();
  }

}
