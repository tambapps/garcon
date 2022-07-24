package com.tambapps.http.garcon

/**
 * Immutable version of {@link com.tambapps.http.garcon.Headers }
 */
class ImmutableHeaders extends Headers {

  ImmutableHeaders(Map<String, String> map) {
    map.each { name, value -> super.putAt(name, value) }
  }

  @Override
  void putAt(String name, String value) {
    throw new UnsupportedOperationException('Cannot modify immutable map')
  }

}
