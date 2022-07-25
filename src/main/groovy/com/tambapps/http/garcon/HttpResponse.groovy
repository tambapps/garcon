package com.tambapps.http.garcon


class HttpResponse {

  String httpVersion
  int statusCode
  String message
  Headers headers

  // body can be a byte array, a string, or an input stream
  /**
   * Body of the response. Can be a byte array, a String, or an InputStream (or null for no body)
   */
  Object body

  void setBody(Object body) {
    if (body == null) {
      return
    }
    switch (body) {
      case byte[]:
      case String:
      case InputStream:
        this.@body = body
        break
      default:
        throw new IllegalStateException("Cannot handled body of type ${body.class}")
    }

  }
  boolean isIndefiniteSize() {
    return contentSize == null
  }

  Long getContentSize() {
    if (!body) {
      return 0L
    }
    switch (body) {
      case byte[]:
      case String:
        return body.size()
      case InputStream:
        return null
      default:
        throw new IllegalStateException("Cannot handled body of type ${body.class}")
    }
  }

  void writeInto(OutputStream os) {
    def writer = os.newPrintWriter()
    writer.println("$httpVersion $statusCode $message")
    headers.each { name, value -> writer.println("$name: $value") }
    writer.println()
    writer.flush()
    writeBody(os)
  }

  private void writeBody(OutputStream os) {
    if (!body) {
      return
    }
    switch (body) {
      case byte[]:
        os.write((byte[]) body)
        break
      case String:
        os.write(body.bytes)
        break
      case InputStream:
        os << (InputStream) body
        break
      default:
        throw new IllegalStateException("Cannot handled body of type ${body.class}")
    }
    os.flush()
  }

}
