package com.tambapps.http.garcon;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Immutable version of {@link Headers }
 */
public class ImmutableHeaders extends Headers {

  public ImmutableHeaders(Map<?, ?> map) {
    map.forEach((name, value) -> super.put(String.valueOf(name), String.valueOf(value)));
  }

  @Override
  public String putAt(String name, Object value) {
    throw new UnsupportedOperationException("Cannot modify immutable map");
  }

  @Override
  public String put(String key, String value) {
    throw new UnsupportedOperationException("Cannot modify immutable map");
  }

  @Override
  public String putIfAbsent(String key, String value) {
    throw new UnsupportedOperationException("Cannot modify immutable map");
  }

  @Override
  public void putAll(Map<? extends String, ? extends String> m) {
    throw new UnsupportedOperationException("Cannot modify immutable map");
  }

  @Override
  public String compute(String key,
      BiFunction<? super String, ? super String, ? extends String> remappingFunction) {
    throw new UnsupportedOperationException("Cannot modify immutable map");
  }

  @Override
  public String computeIfPresent(String key,
      BiFunction<? super String, ? super String, ? extends String> remappingFunction) {
    throw new UnsupportedOperationException("Cannot modify immutable map");
  }

  @Override
  public String computeIfAbsent(String key,
      Function<? super String, ? extends String> mappingFunction) {
    throw new UnsupportedOperationException("Cannot modify immutable map");
  }

  @Override
  public String remove(Object key) {
    throw new UnsupportedOperationException("Cannot modify immutable map");
  }

  @Override
  public boolean remove(Object key, Object value) {
    throw new UnsupportedOperationException("Cannot modify immutable map");
  }
}
