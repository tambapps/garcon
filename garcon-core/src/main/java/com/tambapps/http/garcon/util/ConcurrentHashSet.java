package com.tambapps.http.garcon.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Concurrent hash set, backed by a concurrent hash map. Doesn't support null elements
 * @param <T> the type of objects to hold
 */
public class ConcurrentHashSet<T> implements Set<T> {
  
  private final ConcurrentHashMap<T, T> map = new ConcurrentHashMap<>();
  
  @Override
  public int size() {
    return map.size();
  }

  @Override
  public boolean isEmpty() {
    return map.isEmpty();
  }

  @Override
  public boolean contains(Object o) {
    return map.containsKey(o);
  }

  @Override
  public Iterator<T> iterator() {
    return map.keySet().iterator();
  }

  @Override
  public Object[] toArray() {
    return map.keySet().toArray();
  }

  @Override
  public <T1> T1[] toArray(T1[] a) {
    return map.keySet().toArray(a);
  }

  @Override
  public boolean add(T t) {
    return map.put(t, t) == null;
  }

  @Override
  public boolean remove(Object o) {
    return map.remove(o) != null;
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    return map.keySet().containsAll(c);
  }

  @Override
  public boolean addAll(Collection<? extends T> c) {
    int size = size();
    c.forEach(e -> map.put(e, e));
    return size() > size;
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    return map.keySet().retainAll(c);
  }

  @Override public boolean removeAll(Collection<?> c) {
    return map.keySet().removeAll(c);
  }

  @Override
  public void clear() {
    map.clear();
  }
}
