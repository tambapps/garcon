package com.tambapps.http.garcon;

import lombok.Data;
import org.codehaus.groovy.runtime.IOGroovyMethods;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@Data
public class HttpResponse {

  final String httpVersion = "HTTP/1.1";
  HttpStatusCode statusCode = HttpStatus.OK;
  Headers headers = new Headers();

  // body can be a byte array, a string, or an input stream
  /**
   * Body of the response. Can be a byte array, a String, or an InputStream (or null for no body)
   */
  Object body;

  public boolean is2xxSuccessful() {
    return statusCode.getValue() >= 200 && statusCode.getValue() < 300;
  }
  public void setBody(Object body) {
    if (body == null) {
      return;
    }
    if (!(body instanceof byte[]) && !(body instanceof String) && !(body instanceof InputStream)) {
      throw new IllegalStateException(String.format("Cannot handle body of type %s", body.getClass()));
    }
    this.body = body;
  }
  public boolean isIndefiniteLength() {
    return getContentLength() == null;
  }

  public Long getContentLength() {
    if (body == null) {
      return 0L;
    } else if (body instanceof byte[]) {
      return (long) ((byte[]) body).length;
    } else if (body instanceof String) {
      return (long) ((String) body).getBytes().length;
    } else if (body instanceof InputStream) {
      return null;
    } else {
      throw new IllegalStateException("Cannot handle body of type " + body.getClass());
    }
  }

  public void writeBody(OutputStream os) throws IOException {
    if (body == null) {
      return;
    }
    if (body instanceof byte[]) {
      os.write((byte[]) body);
    } else if (body instanceof String) {
      os.write(((String) body).getBytes());
    } else if (body instanceof InputStream) {
      IOGroovyMethods.leftShift(os, (InputStream) body);
    } else {
      throw new IllegalStateException("Cannot handle body of type " + body.getClass());
    }
    os.flush();
  }

  public boolean isKeepAlive() {
    return Headers.CONNECTION_KEEP_ALIVE.equalsIgnoreCase(headers.get(Headers.CONNECTION_HEADER));
  }
}
