package com.tambapps.http.garcon

import com.tambapps.http.garcon.io.composer.Composers
import com.tambapps.http.garcon.io.composer.Parsers
import com.tambapps.http.garcon.util.ContentTypeMap
import groovy.transform.PackageScope

class Garcon extends AbstractGarcon {

  Garcon() {}

  Garcon(InetAddress address, int port) {
    super()
    super.setAddress(address)
    super.setPort(port)
  }
  Garcon(InetAddress address, int port, int backlog) {
    super()
    super.setAddress(address)
    super.setPort(port)
    super.setBacklog(backlog)
  }

  @PackageScope
  final EndpointsHandler endpointsHandler = new EndpointsHandler()
  final ContentTypeMap<Closure<?>> composers = Composers.map
  final ContentTypeMap<Closure<?>> parsers = Parsers.map

  ContentType accept
  ContentType contentType
  Closure onClosed
  Closure onError
  Closure onConnectionClosed
  Closure onConnectionError
  Closure onConnectionUnexpectedError

  void define(@DelegatesTo(EndpointsHandler) Closure closure) {
    endpointsHandler.define(this, closure)
  }

  void serve(@DelegatesTo(EndpointDefiner) Closure closure) {
    define(closure)
    start()
  }

  void serveAsync(@DelegatesTo(EndpointDefiner) Closure closure) {
    define(closure)
    startAsync()
  }

  @Override
  Runnable newExchangeHandler(Socket socket, AbstractGarcon garcon, Collection<Closeable> connections) {
    return new HttpExchangeHandler(socket, this, connections)
  }

  @PackageScope
  @Override
  void onServerSocketClosed(SocketException e) {
    onClosed?.call(e)
  }

  @PackageScope
  @Override
  void onServerException(IOException e) {
    onError?.call(e)
  }
}
