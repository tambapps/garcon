package com.tambapps.http.garcon;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Class handling case insensitive headers
 */
public class Headers implements Map<String, String> {

  public static final String CONNECTION_KEEP_ALIVE = "Keep-Alive";
  public static final String CONNECTION_CLOSE = "Close";
  public static final String CONNECTION_HEADER = "Connection";
  public static final String CONTENT_TYPE_HEADER = "Content-Type";

  private final Set<AbstractMap.SimpleEntry<String, String>> entries = new HashSet<>();

  public Headers() {
  }

  public Headers(Map<?, ?> map) {
    map.forEach((name, value) -> putAt(String.valueOf(name), String.valueOf(value)));
  }

  public void putAt(String name, Object value) {
    AbstractMap.SimpleEntry<String, String> entry = findEntryWithName(name);
    if (entry != null) {
      entries.remove(entry);
    }
    entries.add(new AbstractMap.SimpleEntry<>(name, String.valueOf(value)));
  }

  public String getAt(String name) {
    AbstractMap.SimpleEntry<String, String> entry = findEntryWithName(name);
    return entry != null ? entry.getValue() : null;
  }

  private AbstractMap.SimpleEntry<String, String> findEntryWithName(String name) {
    return entries.stream()
        .filter(e -> e.getKey().equalsIgnoreCase(name))
        .findFirst()
        .orElse(null);
  }

  public void putConnectionHeader(String value) {
    putAt(CONNECTION_HEADER, value);
  }

  public void putContentTypeHeader(String value) {
    putAt(CONTENT_TYPE_HEADER, value);
  }
  public String getConnectionHeader() {
    return getAt(CONNECTION_HEADER);
  }

  @Override
  public int size() {
    return entries.size();
  }

  @Override
  public boolean isEmpty() {
    return entries.isEmpty();
  }

  @Override
  public boolean containsKey(Object key) {
    return key instanceof CharSequence && findEntryWithName(key.toString()) != null;
  }

  @Override
  public boolean containsValue(Object value) {
    return entries.stream()
        .anyMatch(it -> Objects.equals(it.getValue(), value));
  }

  @Override
  public String get(Object key) {
    return key instanceof CharSequence ? getAt(key.toString()) : null;
  }

  @Override
  public String put(String key, String value) {
    putAt(key, value);
    return null;
  }

  @Override
  public String remove(Object key) {
    if (key instanceof CharSequence) {
      AbstractMap.SimpleEntry<String, String> entry = findEntryWithName(key.toString());
      if (entry != null) {
        entries.remove(entry);
      }
      return entry != null ? entry.getValue() : null;
    } else {
      return null;
    }
  }

  @Override
  public void putAll(Map<? extends String, ? extends String> m) {
    m.forEach(this::putAt);
  }

  @Override
  public void clear() {
    entries.clear();
  }

  @Override
  public Set<String> keySet() {
    return entries.stream()
        .map(AbstractMap.SimpleEntry::getKey)
        .collect(Collectors.toSet());
  }

  @Override
  public Collection<String> values() {
    return entries.stream()
        .map(AbstractMap.SimpleEntry::getValue)
        .collect(Collectors.toSet());
  }

  @Override
  public Set<Entry<String, String>> entrySet() {
    return Collections.unmodifiableSet(entries);
  }

  public ImmutableHeaders asImmutable() {
    return new ImmutableHeaders(this);
  }
}
