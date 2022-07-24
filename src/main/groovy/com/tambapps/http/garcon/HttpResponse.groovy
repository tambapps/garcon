package com.tambapps.http.garcon


class HttpResponse {

  String httpVersion
  int statusCode
  String message
  Headers headers

  // TODO make body lazy and allow to write it as a stream
  byte[] body

  void writeInto(OutputStream os) {
    def writer = os.newPrintWriter()
    // TODO status code text OK
    writer.println("$httpVersion $statusCode $message")
    headers.each { name, value -> writer.println("$name: $value") }
    writer.println()
    writer.flush()
    if (body) {
      os.write(body)
    }
    os.flush()
  }
}
