package com.tambapps.http.garcon;

import com.tambapps.http.garcon.exception.MethodNotAllowedException;
import com.tambapps.http.garcon.exception.ParsingException;
import com.tambapps.http.garcon.exception.PathNotFoundException;
import com.tambapps.http.garcon.io.composer.Composers;
import com.tambapps.http.garcon.io.parser.Parsers;
import com.tambapps.http.garcon.util.AddressUtils;
import com.tambapps.http.garcon.util.ContentTypeFunctionMap;
import com.tambapps.http.garcon.util.ContentTypeMap;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

import java.net.InetAddress;
import java.util.concurrent.Executors;

public class Garcon extends AbstractHttpExchangeHandler {
  @Getter
  private InetAddress address;
  @Getter
  private Integer port;
  // TODO handle me
  @Getter
  private int requestReadTimeoutMillis = 4000;
  // TODO handle me
  @Getter
  @Setter
  private Long maxRequestBytes = null;

  @Getter
  private int maxThreads = 200;


  @Getter
  @Setter
  private ContentType accept;
  @Getter
  @Setter
  private ContentType contentType;

  @Getter
  @Setter
  Closure<?> onStart;
  @Getter
  @Setter
  Closure<?> onStop;
  @Getter
  @Setter
  Closure<?> onServerError;
  @Getter
  @Setter
  Closure<?> onExchangeError;

  public final ContentTypeFunctionMap<Object, byte[]> composers = Composers.getMap();
  public final ContentTypeFunctionMap<byte[], Object> parsers = Parsers.getMap();
  final EndpointsHandler endpointsHandler = new EndpointsHandler();

  private AsyncHttpServer httpServer;

  public Garcon() {}

  @SneakyThrows
  public Garcon(String address, int port) {
    this(AddressUtils.getAddress(address), port);
  }

  public Garcon(InetAddress address, int port) {
    this.address = address;
    this.port = port;
  }

  public boolean isRunning() {
    return httpServer != null && httpServer.isRunning();
  }


  public void start() {
    if (isRunning()) {
      return;
    }
    if (address == null || port == null) {
      throw new IllegalStateException("Cannot start server without address and port");
    }
    httpServer = new AsyncHttpServer(Executors.newFixedThreadPool(maxThreads), this);
    httpServer.start(address, port);
    if (onStart != null) {
      onStart.call(address, port);
    }
  }

  public void waitStop() {
    if (httpServer != null) {
      httpServer.waitStop();
    }
  }



  public void stop() {
    if (isRunning()) {
      httpServer.stop();
      if (onStop != null) {
        onStop.call();
      }
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

  @SneakyThrows
  public void setAddress(String address) {
    setAddress(InetAddress.getByName(address));
  }

  public void setAddress(InetAddress address) {
    checkRunning("Cannot modify address while running");
    this.address = address;
  }

  public void setPort(Integer port) {
    checkRunning("Cannot modify port while running");
    this.port = port;
  }

  public void setRequestReadTimeoutMillis(int requestReadTimeoutMillis) {
    checkRunning("Cannot modify requestReadTimeoutMillis while running");
    this.requestReadTimeoutMillis = requestReadTimeoutMillis;
  }

  public void setMaxThreads(int maxThreads) {
    checkRunning("Cannot modify maxThreads while running");
    this.maxThreads = maxThreads;
  }

  protected void checkRunning(String errorMessage) {
    if (isRunning()) {
      throw new IllegalStateException(errorMessage);
    }
  }

  @Override
  public HttpResponse processExchange(HttpRequest request) {
    EndpointDefinition definition;
    try {
      definition = endpointsHandler.getEndpoint(request.getPath(), request.getMethod());
    } catch (PathNotFoundException e) {
      return default404Response();
    } catch (MethodNotAllowedException e) {
      return default405Response(request.getMethod());
    }

    HttpExchangeContext context = new HttpExchangeContext(request, new HttpResponse(), composers, parsers,
        definition.getContentType() != null ? definition.getContentType() : contentType,
        definition.getAccept() != null ? definition.getAccept() : accept);

    try {
      return definition.call(context);
    } catch (ParsingException e) {
      return default400Response("Request body is malformed");
    } catch (Exception e) {
      if (onExchangeError != null) {
        onExchangeError.call(e);
      }
      return default500Response();
    }
  }
}
