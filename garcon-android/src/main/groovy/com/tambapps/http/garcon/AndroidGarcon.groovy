package com.tambapps.http.garcon

import groovy.transform.CompileStatic
import groovy.transform.PackageScope

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference

@CompileStatic
class AndroidGarcon extends Garcon {

  private int maxThreads = 200
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
      return
    }
    if (executorService == null) {
      executorService = Executors.newSingleThreadExecutor()
    }
    executorService.submit {
      try {
        start()
      } catch (Exception e) {
        // shouldn't happen... but well...
        e.printStackTrace()
        stop()
      }
    }
  }

  @Override
  void doStart(EndpointsHandler endpointsHandler) {
    if (isRunning()) {
      // already running
      return
    }
    HttpServer server = HttpServer.create(this, address, port ?: 0, backlog ?: 0, requestReadTimeoutMillis, maxThreads)
    serverReference.set(server)
    server.run()
  }

  @PackageScope
  void onServerException(IOException e) {
    onServerError?.call(e)
  }


  int getMaxThreads() {
    return this.@maxThreads
  }

  void setMaxThreads(int nbThreads) {
    checkRunning("Cannot modify maxThreads while running")
    this.maxThreads = nbThreads;
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
