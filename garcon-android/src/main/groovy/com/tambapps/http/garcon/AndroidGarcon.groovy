package com.tambapps.http.garcon

import groovy.transform.CompileStatic
import groovy.transform.PackageScope

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference

@CompileStatic
class AndroidGarcon extends Garcon {

  private int nbThreads = 200
  private final AtomicReference<HttpServer> serverReference = new AtomicReference()
  private ExecutorService executorService

  AndroidGarcon() {}

  AndroidGarcon(String address, int port) {
    this((InetAddress) InetAddress.getByName(address), port)
  }

  AndroidGarcon(InetAddress address, int port) {
    super()
    super.setAddress(address)
    super.setPort(port)
  }

  AndroidGarcon(InetAddress address, int port, int backlog) {
    super()
    super.setAddress(address)
    super.setPort(port)
    super.setBacklog(backlog)
  }

  void startAsync() {
    if (isRunning()) {
      // already running
      return;
    }
    if (executorService == null) {
      executorService = Executors.newSingleThreadExecutor();
    }
    executorService.submit {
      try {
        start()
      } catch (Exception e) {
        // shouldn't happen... but well...
        e.printStackTrace()
        doStop()
      }
    }
  }

  @Override
  void doStart(EndpointsHandler endpointsHandler) {
    if (isRunning()) {
      // already running
      return
    }
    HttpServer server = HttpServer.create(this, address, port ?: 0, backlog ?: 0, requestReadTimeoutMillis, nbThreads)
    serverReference.set(server)
    server.run()
  }

  protected void onStarted(InetAddress address, int port) {
    this.onStarted?.call(address, port)
  }

  @PackageScope
  void onServerSocketClosed(SocketException e) {
    onClosed?.call()
  }

  @PackageScope
  void onServerException(IOException e) {
    onError?.call(e)
  }


  int getNbThreads() {
    return this.@nbThreads
  }

  void setNbThreads(int nbThreads) {
    checkRunning("Cannot modify nbThreads while running");
    this.nbThreads = nbThreads;
  }


  @Override
  boolean isRunning() {
    return serverReference.get() != null
  }

  @Override
  void doStop() {
    HttpServer server = serverReference.get()
    server.stop()
    serverReference.set(null)
    if (executorService != null) {
      executorService.shutdown()
    }
    executorService = null
  }
}
