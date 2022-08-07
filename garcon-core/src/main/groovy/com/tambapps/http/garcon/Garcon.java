package com.tambapps.http.garcon;

import com.tambapps.http.garcon.io.Composers;
import com.tambapps.http.garcon.io.Parsers;
import com.tambapps.http.garcon.util.ContentTypeMap;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

import java.net.InetAddress;

public abstract class Garcon {

  @Getter
  private InetAddress address;
  @Getter
  private Integer port;
  @Getter
  private Integer backlog;
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


  public final ContentTypeMap<Closure<?>> composers = Composers.getMap();
  public final ContentTypeMap<Closure<?>> parsers = Parsers.getMap();
  final EndpointsHandler endpointsHandler = new EndpointsHandler();

  // package private constructor
  Garcon() {}

  public abstract boolean isRunning();


  public void start() {
    if (address == null || port == null) {
      throw new IllegalStateException("Cannot start server without address and port");
    }
    if (isRunning()) {
      return;
    }
    doStart(endpointsHandler);
  }

  abstract void doStart(EndpointsHandler endpointsHandler);

  abstract void startAsync();

  abstract void doStop();

  public void stop() {
    if (isRunning()) {
      doStop();
    }
  }

  public Garcon define(@DelegatesTo(EndpointDefiner.class) Closure closure) {
    endpointsHandler.define(this, closure);
    return this;
  }

  public Garcon serve(@DelegatesTo(EndpointDefiner.class) Closure closure) {
    define(closure);
    start();
    return this;
  }

  public Garcon serveAsync(@DelegatesTo(EndpointDefiner.class) Closure closure) {
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

  public void setRequestReadTimeoutMillis(int requestReadTimeoutMillis) {
    checkRunning("Cannot modify requestReadTimeoutMillis while running");
    this.requestReadTimeoutMillis = requestReadTimeoutMillis;
  }

  protected void checkRunning(String errorMessage) {
    if (isRunning()) {
      throw new IllegalStateException(errorMessage);
    }
  }
}
