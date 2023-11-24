package com.tambapps.http.garcon;

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
import com.tambapps.http.garcon.exception.HttpStatusException;
import com.tambapps.http.garcon.exception.MethodNotAllowedException;
import com.tambapps.http.garcon.exception.NotFoundException;
import com.tambapps.http.garcon.logger.DefaultLogger;
import com.tambapps.http.garcon.logger.Logger;
import com.tambapps.http.garcon.server.AsyncHttpServer;
import com.tambapps.http.garcon.server.HttpServer;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Garcon, the grooviest HTTP Server
 */
public abstract class AbstractGarcon<T> extends AbstractHttpExchangeHandler {

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
  BiConsumer<Object, Object> onStart;
  @Getter
  @Setter
  Runnable onStop;
  @Getter
  @Setter
  Consumer<Object> onServerError;
  @Getter
  @Setter
  Consumer<Object> onExchangeError;


  abstract EndpointDefiner<T> newDefiner();
  abstract T fromMethod(Object instance, Method method, HttpStatus status);

  abstract HttpExchangeContext newContext(HttpRequest request, HttpResponse response, ContentType contentType, ContentType accept);

  EndpointsHandler<T> endpointsHandler;

  // can provide own HttpServer implementation
  @Getter
  private HttpServer httpServer;

  /**
   * Empty constructor for garcon. Note that you'll need to set the address and the port before
   * starting it
   */
  public AbstractGarcon() {}

  @SneakyThrows
  public AbstractGarcon(String address, int port) {
    this((InetAddress) null, port);
    // setting it later for SneakyThrows to work
    this.address = InetAddress.getByName(address);
  }

  public AbstractGarcon(InetAddress address, int port) {
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
      onStart.accept(address, port);
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
      httpServer = null;
      if (onStop != null) {
        onStop.run();
      }
    }
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
          newContext(request, new HttpResponse(),
              contentType, accept), e);
    } catch (MethodNotAllowedException e) {
      return default405Response(newContext(request, new HttpResponse(),
          contentType, accept), request.getMethod());
    }

    HttpExchangeContext context = newContext(request, new HttpResponse(),
        definition.getContentType() != null ? definition.getContentType() : contentType,
        definition.getAccept() != null ? definition.getAccept() : accept);

    try {
      return definition.call(context);
    } catch (HttpStatusException e) {
      return defaultHttpStatusResponse(context, e);
    } catch (Exception e) {
      AbstractGarcon.getLogger().error(String.format("Unexpected error on endpoint %s %s", context.getMethod(), context.getPath()), e);
      if (onExchangeError != null) {
        onExchangeError.accept(e);
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


  void define(String httpMethod, String acceptStr, String contentTypeStr, String path,
      Object instance, Method method, HttpStatus status) {
    EndpointDefiner<T> definer = newDefiner();
    T closure = fromMethod(instance, method, status);

    ContentType accept = acceptStr != null && !acceptStr.isEmpty() ? ContentType.valueOf(acceptStr) : null;
    ContentType contentType = contentTypeStr != null && !contentTypeStr.isEmpty() ? ContentType.valueOf(contentTypeStr) : null;

    definer.method(httpMethod, path, accept, contentType, closure);
    endpointsHandler = definer.build();
  }

  @SneakyThrows
  static <T extends AbstractGarcon> T fromInstance(
      T garcon,
      ContentType contentType,
      ContentType accept,
      Object i) {
    // if a class was passed, we tried to construct an instance for it and use it as the garcon spec
    Object instance = i instanceof Class ? ((Class)i).getConstructor().newInstance() : i;
    Class<?> clazz = instance.getClass();

    if (contentType != null) {
      garcon.setContentType(contentType);
    }
    if (accept != null) {
      garcon.setAccept(accept);
    }
    Method[] methods = clazz.getMethods();
    for (Method method : methods) {
      if (method.getName().equals("onStart")) {
        garcon.onStart = (address, port) -> {
          try {
            switch (method.getParameterCount()) {
              case 0:
                method.invoke(instance);
                break;
              case 1:
                method.invoke(instance, address);
                break;
              default:
                method.invoke(instance, address, port);
                break;
            }
          } catch (InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
          }
        };
      }
      if (method.getName().equals("onStop")) {
        garcon.onStop = () -> {
          try {
            method.invoke(instance);
          } catch (InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
          }
        };
      }
      if (method.getName().equals("onServerError")) {
        garcon.onServerError = (exception) -> {
          try {
            method.invoke(instance, exception);
          } catch (InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
          }
        };;
      }
      if (method.getName().equals("onExchangeError")) {
        garcon.onExchangeError = (exception) -> {
          try {
            method.invoke(instance, exception);
          } catch (InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
          }
        };
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
