package com.tambapps.http.garcon;

import java.util.Map;

/**
 * Immutable version of {@link com.tambapps.http.garcon.Headers }
 */
public class ImmutableHeaders extends Headers {

  public ImmutableHeaders(Map<?, ?> map) {
    map.forEach((name, value) -> super.putAt(String.valueOf(name), String.valueOf(value)));
  }

  @Override
  public void putAt(String name, Object value) {
    throw new UnsupportedOperationException("Cannot modify immutable map");
  }

}
