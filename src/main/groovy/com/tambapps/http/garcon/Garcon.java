package com.tambapps.http.garcon;

import static com.tambapps.http.garcon.util.ParametersUtils.getOrDefault;

import com.tambapps.http.garcon.annotation.Delete;
import com.tambapps.http.garcon.annotation.Endpoint;
import com.tambapps.http.garcon.annotation.Get;
import com.tambapps.http.garcon.annotation.Patch;
import com.tambapps.http.garcon.annotation.Post;
import com.tambapps.http.garcon.annotation.Put;
import com.tambapps.http.garcon.annotation.ResponseStatus;
import com.tambapps.http.garcon.endpoint.EndpointDefiner;
import com.tambapps.http.garcon.endpoint.EndpointDefinition;
import com.tambapps.http.garcon.endpoint.EndpointsHandler;
import com.tambapps.http.garcon.exception.BadRequestException;
import com.tambapps.http.garcon.exception.HttpStatusException;
import com.tambapps.http.garcon.exception.MethodNotAllowedException;
import com.tambapps.http.garcon.exception.NotFoundException;
import com.tambapps.http.garcon.io.composer.Composers;
import com.tambapps.http.garcon.io.parser.Parsers;
import com.tambapps.http.garcon.logger.DefaultLogger;
import com.tambapps.http.garcon.logger.Logger;
import com.tambapps.http.garcon.server.AsyncHttpServer;
import com.tambapps.http.garcon.server.HttpServer;
import com.tambapps.http.garcon.util.ContentTypeFunctionMap;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import groovy.transform.NamedParam;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.codehaus.groovy.runtime.MethodClosure;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Garcon, the grooviest HTTP Server
 */
public class Garcon extends AbstractHttpExchangeHandler {

  private static Logger LOGGER;

  public static Logger getLogger() {
    if (LOGGER == null) {
      LOGGER = new DefaultLogger();
    }
    return LOGGER;
  }
  public static void setLogger(Logger logger) {
    LOGGER = logger;
  }
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

  // can provide own HttpServer implementation
  @Getter
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

  @SneakyThrows
  public void start(
      @NamedParam(value = "port", type = Integer.class)
      @NamedParam(value = "address", type = InetAddress.class)
      Map<?,?> args) {
    if (args != null) {
      Object a = args.get("address");
      if (a != null) {
        InetAddress address;
        if (a instanceof String) {
          address = InetAddress.getByName((String) a);
        } else {
          address = getOrDefault(args, "address", InetAddress.class, null);
        }
        setAddress(address);
      }
      Integer port = getOrDefault(args, "port", Integer.class, null);
      if (port != null) {
        setPort(port);
      }
    }
    start();
  }

  /**
   * Starts the server
   */
  public void start() {
    if (isRunning()) {
      throw new IllegalStateException("Server already stared");
    }
    if (endpointsHandler == null || endpointsHandler.isEmpty()) {
      throw new IllegalStateException("You must define endpoints before starting garcon");
    }
    if (address == null || port == null) {
      throw new IllegalStateException("Cannot start server without address and port");
    }
    if (httpServer == null) {
      // use our default implementation
      httpServer = new AsyncHttpServer(Executors.newFixedThreadPool(maxThreads, new GarconThreadPool()), requestReadTimeoutMillis, this);
    }
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

  public void setHttpServer(HttpServer httpServer) {
    checkRunning("Cannot modify httpServer while running");
    this.httpServer = httpServer;
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
    } catch (NotFoundException e) {
      return default404Response(
          new HttpExchangeContext(request, new HttpResponse(), composers, parsers,
              contentType, accept), e);
    } catch (MethodNotAllowedException e) {
      return default405Response(new HttpExchangeContext(request, new HttpResponse(), composers, parsers,
          contentType, accept), request.getMethod());
    }

    HttpExchangeContext context = new HttpExchangeContext(request, new HttpResponse(), composers, parsers,
        definition.getContentType() != null ? definition.getContentType() : contentType,
        definition.getAccept() != null ? definition.getAccept() : accept);

    try {
      return definition.call(context);
    } catch (HttpStatusException e) {
      return defaultHttpStatusResponse(context, e);
    } catch (Exception e) {
      Garcon.getLogger().error(String.format("Unexpected error on endpoint %s %s", context.getMethod(), context.getPath()), e);
      if (onExchangeError != null) {
        onExchangeError.call(e);
      }
      return default500Response(context);
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

  private void define(String httpMethod, String acceptStr, String contentTypeStr, String path,
      Object instance, Method method, HttpStatus status) {
    EndpointDefiner definer = EndpointDefiner.newInstance(this, endpointsHandler);
    Closure<?> closure = new ReflectMethodClosure(instance, method, status);

    Map<String, Object> params = new HashMap<>();
    if (acceptStr != null && !acceptStr.isEmpty()) {
      params.put("accept", ContentType.valueOf(acceptStr));
    }
    if (contentTypeStr != null && !contentTypeStr.isEmpty()) {
      params.put("contentType", ContentType.valueOf(contentTypeStr));
    }
    definer.method(params, httpMethod, path, closure);
    endpointsHandler = definer.build();
  }

  /**
   * Construct a garcon from the provided instance. All method annotated to an garcon endpoint annotation
   * will become an endpoint of the returned garcon
   *
   * @param instance the instance from which to construct the garcon
   * @return the garcon
   */
  public static Garcon fromInstance(Object instance) {
    return fromInstance(null, instance);
  }

  /**
   * Construct a garcon from the provided instance. All method annotated to an garcon endpoint annotation
   * will become an endpoint of the returned garcon
   *
   * @param additionalParams the additional params
   * @param instance the instance from which to construct the garcon
   * @return the garcon
   */
  public static Garcon fromInstance(
      @NamedParam(value = "contentType", type = ContentType.class)
      @NamedParam(value = "accept", type = ContentType.class)
      Map<?,?> additionalParams, Object instance) {
    if (instance instanceof Class) {
      // if a class was passed, we tried to construct an instance for it and use it as the garcon spec
      instance = DefaultGroovyMethods.newInstance((Class<?>) instance);
    }
    Class<?> clazz = instance.getClass();

    ContentType contentType = getOrDefault(additionalParams, "contentType", ContentType.class, null);
    ContentType accept = getOrDefault(additionalParams, "accept", ContentType.class, null);

    Garcon garcon = new Garcon();
    if (contentType != null) {
      garcon.setContentType(contentType);
    }
    if (accept != null) {
      garcon.setAccept(accept);
    }
    Method[] methods = clazz.getMethods();
    for (Method method : methods) {
      if (method.getName().equals("onStart")) {
        garcon.onStart = new MethodClosure(instance, method.getName());
      }
      if (method.getName().equals("onStop")) {
        garcon.onStop = new MethodClosure(instance, method.getName());
      }
      if (method.getName().equals("onServerError")) {
        garcon.onServerError = new MethodClosure(instance, method.getName());
      }
      if (method.getName().equals("onExchangeError")) {
        garcon.onExchangeError = new MethodClosure(instance, method.getName());
      }

      HttpStatus status = HttpStatus.OK;
      ResponseStatus responseStatus = method.getAnnotation(ResponseStatus.class);
      if (responseStatus != null) {
        status = responseStatus.value();
      }
      Get get = method.getAnnotation(Get.class);
      if (get != null) {
        garcon.define("GET", null, get.contentType(), get.value().isEmpty() ? get.path() : get.value(), instance, method, status);
      }
      Delete delete = method.getAnnotation(Delete.class);
      if (delete != null) {
        garcon.define("DELETE", null, delete.contentType(), delete.value().isEmpty() ? delete.path() : delete.value(), instance, method, status);
      }
      Patch patch = method.getAnnotation(Patch.class);
      if (patch != null) {
        garcon.define("PATCH", patch.accept(), patch.contentType(), patch.value().isEmpty() ? patch.path() : patch.value(), instance, method, status);
      }
      Put put = method.getAnnotation(Put.class);
      if (put != null) {
        garcon.define("PUT", put.accept(), put.contentType(), put.value().isEmpty() ? put.path() : put.value(), instance, method, status);
      }
      Post post = method.getAnnotation(Post.class);
      if (post != null) {
        garcon.define("POST", post.accept(), post.contentType(), post.value().isEmpty() ? post.path() : post.value(), instance, method, status);
      }
      Endpoint endpoint = method.getAnnotation(Endpoint.class);
      if (endpoint != null) {
        garcon.define(endpoint.method(), endpoint.accept(), endpoint.contentType(), endpoint.path(), instance, method, status);
      }
    }
    return garcon;
  }
}
