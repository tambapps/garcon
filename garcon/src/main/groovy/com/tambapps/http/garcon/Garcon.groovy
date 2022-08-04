package com.tambapps.http.garcon

import com.tambapps.http.garcon.io.composer.Composers
import com.tambapps.http.garcon.io.composer.Parsers
import com.tambapps.http.garcon.util.ContentTypeMap
import groovy.transform.CompileStatic
import groovy.transform.PackageScope

@CompileStatic
class Garcon extends AbstractGarcon {

  Garcon() {}

  Garcon(String address, int port) {
    this(InetAddress.getByName(address), port)
  }

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
  Closure onStarted
  Closure onConnectionClosed
  Closure onConnectionError
  Closure onConnectionUnexpectedError

  Garcon define(@DelegatesTo(EndpointDefiner) Closure closure) {
    endpointsHandler.define(this, closure)
    return this
  }

  Garcon serve(@DelegatesTo(EndpointDefiner) Closure closure) {
    define(closure)
    start()
    return this
  }

  Garcon serveAsync(@DelegatesTo(EndpointDefiner) Closure closure) {
    define(closure)
    startAsync()
    return this
  }

  @Override
  Runnable newExchangeHandler(Socket socket, AbstractGarcon garcon, Collection<Closeable> connections) {
    return new HttpExchangeHandler(socket, this, connections)
  }

  @Override
  protected void onStarted(InetAddress address, int port) {
    this.onStarted?.call(address, port)
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
