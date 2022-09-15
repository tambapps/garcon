package com.tambapps.http.garcon;

import com.tambapps.http.garcon.endpoint.EndpointDefiner;
import com.tambapps.http.garcon.endpoint.EndpointDefinition;
import com.tambapps.http.garcon.endpoint.EndpointsHandler;
import com.tambapps.http.garcon.exception.MethodNotAllowedException;
import com.tambapps.http.garcon.exception.ParsingException;
import com.tambapps.http.garcon.exception.PathNotFoundException;
import com.tambapps.http.garcon.io.composer.Composers;
import com.tambapps.http.garcon.io.parser.Parsers;
import com.tambapps.http.garcon.server.AsyncHttpServer;
import com.tambapps.http.garcon.server.HttpServer;
import com.tambapps.http.garcon.util.ContentTypeFunctionMap;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

import java.net.InetAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Garcon, the grooviest HTTP Server
 */
public class Garcon extends AbstractHttpExchangeHandler {
  @Getter
  private InetAddress address;
  @Getter
  private Integer port;
  @Getter
  private int requestReadTimeoutMillis = 4000;
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

  /**
   * Response composers per content type
   */
  public final ContentTypeFunctionMap<Object, byte[]> composers = Composers.getMap();

  /**
   * Request parsers per content type
   */
  public final ContentTypeFunctionMap<byte[], Object> parsers = Parsers.getMap();
  private EndpointsHandler endpointsHandler;

  private HttpServer httpServer;

  /**
   * Empty constructor for garcon. Note that you'll need to set the address and the port before
   * starting it
   */
  public Garcon() {}

  @SneakyThrows
  public Garcon(String address, int port) {
    this((InetAddress) null, port);
    // setting it later for SneakyThrows to work
    this.address = InetAddress.getByName(address);
  }

  public Garcon(InetAddress address, int port) {
    this.address = address;
    this.port = port;
  }

  public boolean isRunning() {
    return httpServer != null && httpServer.isRunning();
  }


  /**
   * Starts the server
   */
  public void start() {
    if (isRunning()) {
      return;
    }
    if (endpointsHandler == null || endpointsHandler.isEmpty()) {
      throw new IllegalStateException("You must define endpoints before starting garcon");
    }
    if (address == null || port == null) {
      throw new IllegalStateException("Cannot start server without address and port");
    }
    httpServer = new AsyncHttpServer(Executors.newFixedThreadPool(maxThreads, new GarconThreadPool()), requestReadTimeoutMillis, this);
    httpServer.start(address, port);
    if (onStart != null) {
      onStart.call(address, port);
    }
  }

  public void join() {
    if (httpServer != null) {
      httpServer.join();
    }
  }


  /**
   * Stops the server
   */
  public void stop() {
    if (isRunning()) {
      httpServer.stop();
      if (onStop != null) {
        onStop.call();
      }
    }
  }

  public Garcon define(@DelegatesTo(EndpointDefiner.class) Closure<?> closure) {
    EndpointDefiner definer = EndpointDefiner.newInstance(this, endpointsHandler);
    closure.setDelegate(definer);
    closure.setResolveStrategy(Closure.DELEGATE_FIRST);
    closure.call();
    endpointsHandler = definer.build();
    return this;
  }

  /**
   * Define endpoints and starts the server
   * @param closure the definition of the garcon
   * @return this
   */
  public Garcon serve(@DelegatesTo(EndpointDefiner.class) Closure<?> closure) {
    define(closure);
    start();
    return this;
  }

  /**
   * Sets the address to use when starting the server
   * @param address the address
   */
  @SneakyThrows
  public void setAddress(String address) {
    setAddress(InetAddress.getByName(address));
  }

  /**
   * Sets the address to use when starting the server
   * @param address the address
   */
  public void setAddress(InetAddress address) {
    checkRunning("Cannot modify address while running");
    this.address = address;
  }

  /**
   * Sets the port to use when starting the server
   * @param port the port
   */
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

  private static class GarconThreadPool implements ThreadFactory {
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private final ThreadGroup group;

    GarconThreadPool() {
      SecurityManager s = System.getSecurityManager();
      group = (s != null) ? s.getThreadGroup() :
          Thread.currentThread().getThreadGroup();
    }

    @Override
    public Thread newThread(Runnable r) {
      Thread t = new Thread(group, r,
          String.format("garcon-worker-thread-%d", threadNumber.getAndIncrement()),
          0);
      if (t.isDaemon())
        t.setDaemon(false);
      if (t.getPriority() != Thread.NORM_PRIORITY)
        t.setPriority(Thread.NORM_PRIORITY);
      return t;
    }
  }
}
