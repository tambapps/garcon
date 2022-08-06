package com.tambapps.http.garcon;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

abstract class AbstractGarcon {

  @Getter
  private InetAddress address;
  @Getter
  private Integer port;
  @Getter
  private Integer backlog;
  @Getter
  private int nbThreads = 200;
  @Getter
  private int requestReadTimeoutMillis = 4000;
  @Getter
  @Setter
  private Long maxRequestBytes = null;

  @Getter
  @Setter
  private ContentType accept;
  @Getter
  @Setter
  private ContentType contentType;

  @Getter
  @Setter
  Closure<?> onClosed;
  @Getter
  @Setter
  Closure<?> onError;
  @Getter
  @Setter
  Closure<?> onStarted;
  @Getter
  @Setter
  Closure<?> onConnectionClosed;
  @Getter
  @Setter
  Closure<?> onConnectionError;
  @Getter
  @Setter
  Closure<?> onConnectionUnexpectedError;

  private final EndpointsHandler endpointsHandler = new EndpointsHandler();
  private ExecutorService executorService;

  // package private constructor
  AbstractGarcon() {}

  public abstract boolean isRunning();


  public void start() {
    doStart(endpointsHandler);
  }

  abstract void doStart(EndpointsHandler endpointsHandler);

  public void startAsync() {
    if (isRunning()) {
      // already running
      return;
    }
    if (executorService == null) {
      executorService = Executors.newSingleThreadExecutor();
    }
    executorService.submit(() -> {
      try {
        start();
      } catch (Exception e) {
        // shouldn't happen... but well...
        e.printStackTrace();
        doStop();
      }
    });
  }

  abstract void doStop();

  public void stop() {
    if (executorService != null) {
      executorService.shutdown();
    }
    executorService = null;
    doStop();
  }

  public AbstractGarcon define(@DelegatesTo(EndpointDefiner.class) Closure closure) {
    endpointsHandler.define(this, closure);
    return this;
  }

  public AbstractGarcon serve(@DelegatesTo(EndpointDefiner.class) Closure closure) {
    define(closure);
    start();
    return this;
  }

  public AbstractGarcon serveAsync(@DelegatesTo(EndpointDefiner.class) Closure closure) {
    define(closure);
    startAsync();
    return this;
  }

  @SneakyThrows
  public void setAddress(String address) {
    setAddress(InetAddress.getByName(address));
  }

  public void setAddress(InetAddress address) {
    checkRunning("Cannot modify address while running");
    this.address = address;
  }

  public void setBacklog(Integer backlog) {
    checkRunning("Cannot modify backlog while running");
    this.backlog = backlog;
  }

  public void setPort(Integer port) {
    checkRunning("Cannot modify port while running");
    this.port = port;
  }

  public void setNbThreads(int nbThreads) {
    checkRunning("Cannot modify nbThreads while running");
    this.nbThreads = nbThreads;
  }

  public void setRequestReadTimeoutMillis(int requestReadTimeoutMillis) {
    checkRunning("Cannot modify requestReadTimeoutMillis while running");
    this.requestReadTimeoutMillis = requestReadTimeoutMillis;
  }

  private void checkRunning(String errorMessage) {
    if (isRunning()) {
      throw new IllegalStateException(errorMessage);
    }
  }
}
