package com.tambapps.http.garcon

import com.sun.net.httpserver.HttpServer
import groovy.transform.CompileStatic

import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference

// this garcon doesn't handle path variables, because sun HttpServer only handle static paths
@CompileStatic
class SunGarcon extends Garcon {

  private final AtomicReference<HttpServer> serverReference = new AtomicReference<>()
  private int maxThreads = 200

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
    if (this.@maxThreads > 1) {
      server.setExecutor(Executors.newFixedThreadPool(this.@maxThreads))
    }
    def address = this.address
    def port = this.port ?: 0
    server.bind(new InetSocketAddress(address, port), backlog ?: 0)
    // start in background thread
    server.start()
    serverReference.set(server)
    this.onStart?.call(address, port)
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

  int getMaxThreads() {
    return this.@maxThreads
  }

  void setMaxThreads(int nbThreads) {
    checkRunning("Cannot modify maxThreads while running")
    this.maxThreads = nbThreads;
  }
}
