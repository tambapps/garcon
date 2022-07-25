package com.tambapps.http.garcon

/**
 * Immutable version of {@link com.tambapps.http.garcon.Headers }
 */
class ImmutableHeaders extends Headers {

  ImmutableHeaders(Map<?, ?> map) {
    map.each { name, value -> super.putAt(String.valueOf(name), String.valueOf(value)) }
  }

  @Override
  void putAt(String name, Object value) {
    throw new UnsupportedOperationException('Cannot modify immutable map')
  }

}
