package com.tambapps.http.garcon;

import com.tambapps.http.garcon.endpoint.EndpointDefiner;
import com.tambapps.http.garcon.endpoint.EndpointsHandler;
import com.tambapps.http.garcon.endpoint.GroovyEndpointDefiner;
import com.tambapps.http.garcon.io.composer.Composers;
import com.tambapps.http.garcon.io.parser.Parsers;
import com.tambapps.http.garcon.util.ContentTypeFunctionMap;
import com.tambapps.http.garcon.util.ReflectMethodClosure;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import groovy.transform.NamedParam;
import lombok.SneakyThrows;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.Map;

import static com.tambapps.http.garcon.util.ParametersUtils.getOrDefault;

public class Garcon extends AbstractGarcon<Closure<?>> {


  /**
   * Response composers per content type
   */
  public final ContentTypeFunctionMap<Object, byte[]> composers = Composers.getMap();

  /**
   * Request parsers per content type
   */
  public final ContentTypeFunctionMap<byte[], Object> parsers = Parsers.getMap();

  public Garcon() {
  }

  public Garcon(String address, int port) {
    super(address, port);
  }

  public Garcon(InetAddress address, int port) {
    super(address, port);
  }


  public Garcon define(@DelegatesTo(EndpointDefiner.class) Closure<?> closure) {
    EndpointDefiner<Closure<?>> definer = newDefiner();
    closure.setDelegate(definer);
    closure.setResolveStrategy(Closure.DELEGATE_FIRST);
    closure.call();

    endpointsHandler = definer.build();
    return this;
  }


  @Override
  GroovyEndpointDefiner newDefiner() {
    return new GroovyEndpointDefiner(this, endpointsHandler);
  }

  @Override
  Closure<?> fromMethod(Object instance, Method method, HttpStatus status) {
    return new ReflectMethodClosure(instance, method, status);
  }

  @Override
  HttpExchangeContext newContext(HttpRequest request, HttpResponse response, ContentType contentType, ContentType accept) {
    return new GroovyHttpExchangeContext(request, response, composers, parsers, contentType, accept);
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
   * Construct a garcon from the provided instance. All method annotated to an garcon endpoint annotation
   * will become an endpoint of the returned garcon
   *
   * @param instance the instance from which to construct the garcon
   * @return the garcon
   */
  public static AbstractGarcon<Closure<?>> fromInstance(Object instance) {
    return fromInstance(null, instance);
  }

  /**
   * Construct a garcon from the provided instance. All method annotated to an garcon endpoint annotation
   * will become an endpoint of the returned garcon
   *
   * @param additionalParams the additional params
   * @param i the instance from which to construct the garcon
   * @return the garcon
   */
  @SneakyThrows
  static Garcon fromInstance(@NamedParam(value = "contentType", type = ContentType.class)
                                                     @NamedParam(value = "accept", type = ContentType.class)
                                                     Map<?,?> additionalParams, Object i) {
    return AbstractGarcon.fromInstance(new Garcon(),
        getOrDefault(additionalParams, "contentType", ContentType.class, null),
        getOrDefault(additionalParams, "accept", ContentType.class, null), i);

  }
}
