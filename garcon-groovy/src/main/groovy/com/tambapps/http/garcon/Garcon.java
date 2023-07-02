package com.tambapps.http.garcon;

import groovy.transform.NamedParam;
import lombok.SneakyThrows;

import java.net.InetAddress;
import java.util.Map;

import static com.tambapps.http.garcon.util.ParametersUtils.getOrDefault;

public class Garcon extends AbstractGarcon {
  public Garcon() {
  }

  public Garcon(String address, int port) {
    super(address, port);
  }

  public Garcon(InetAddress address, int port) {
    super(address, port);
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
  public static AbstractGarcon fromInstance(Object instance) {
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
