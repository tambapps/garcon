package com.tambapps.http.garcon

import com.sun.net.httpserver.HttpServer
import groovy.transform.CompileStatic

import java.util.concurrent.atomic.AtomicReference

// this garcon doesn't handle path variables, because sun HttpServer only handle static paths
@CompileStatic
class SunGarcon extends Garcon {

  private final AtomicReference<HttpServer> serverReference = new AtomicReference<>()

  SunGarcon() {}

  SunGarcon(String address, int port) {
    // don't know why, groovy complains about it when just using class name
    this((java.net.InetAddress) InetAddress.getByName(address), port)
  }

  SunGarcon(InetAddress address, int port) {
    super()
    super.setAddress(address)
    super.setPort(port)
  }

  @Override
  boolean isRunning() {
    return serverReference.get() != null
  }

  @Override
  void doStart(EndpointsHandler endpointsHandler) {
    def server = HttpServer.create()
    Map<String, List<EndpointDefinition>> definitionsPerPath = endpointsHandler.definitionsPerPath
    definitionsPerPath.each { String path, List<EndpointDefinition> definitions ->
      server.createContext(path, new SunHttpExchangeHandler(garcon: this, pathEndpointDefinitions: definitions))
    }
    server.bind(new InetSocketAddress(address, port ?: 0), backlog ?: 0)
    // start in background thread
    server.start()
    serverReference.set(server)
  }

  @Override
  void startAsync() {
    start()
  }

  @Override
  void doStop() {
    def server = serverReference.get()
    if (server == null) {
      return
    }
    server.stop(0)
    serverReference.set(null)
  }
}
