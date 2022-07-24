package com.tambapps.http.garcon

/**
 * Class handling case insensitive headers
 */
class Headers implements Map<String, String> {

  private Set<AbstractMap.SimpleEntry<String, String>> entries = new HashSet<>()

  Headers() {
  }

  Headers(Map<String, String> map) {
    map.each { name, value -> putAt(name, value) }
  }

  void putAt(String name, Object value) {
    def entry = findEntryWithName(name)
    if (entry != null) {
      entries.remove(entry)
    }
    entries.add(new AbstractMap.SimpleEntry<>(name, String.valueOf(value)))
  }

  String getAt(String name) {
    return findEntryWithName(name)?.value
  }

  private AbstractMap.SimpleEntry<String, String> findEntryWithName(String name) {
    return entries.find { it.key.equalsIgnoreCase(name) }
  }

  @Override
  int size() {
    return entries.size()
  }

  @Override
  boolean isEmpty() {
    return entries.isEmpty()
  }

  @Override
  boolean containsKey(Object key) {
    return key instanceof CharSequence ? findEntryWithName(key.toString()) != null : false
  }

  @Override
  boolean containsValue(Object value) {
    return entries.find { it.value == value }
  }

  @Override
  String get(Object key) {
    return key instanceof CharSequence ? getAt(key.toString()) : null
  }

  @Override
  String put(String key, String value) {
    return putAt(key, value)
  }

  @Override
  String remove(Object key) {
    if (key instanceof CharSequence) {
      def entry = findEntryWithName(key.toString())
      if (entry != null) {
        entries.remove(entry)
      }
      return entry?.value
    } else {
      return null
    }
  }

  @Override
  void putAll(Map<? extends String, ? extends String> m) {
    m.each { key, value -> putAt(key, value) }
  }

  @Override
  void clear() {
    entries.clear()
  }

  @Override
  Set<String> keySet() {
    return entries.collect { it.key }.toSet()
  }

  @Override
  Collection<String> values() {
    return entries.collect { it.value }.toSet()
  }

  @Override
  Set<Entry<String, String>> entrySet() {
    return entries.asImmutable()
  }

  ImmutableHeaders asImmutable() {
    return new ImmutableHeaders()
  }
}
