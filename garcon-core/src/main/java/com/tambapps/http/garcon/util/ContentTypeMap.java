package com.tambapps.http.garcon.util;

import com.tambapps.http.garcon.ContentType;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.TreeMap;

/**
 * A map of content type -&gt; T that handles content types inclusion when retrieving an element
 * @param <T> the type of the map values
 */
// using TreeMap to have ContentType sorted when iterating over keys and checking inclusion
public class ContentTypeMap<T> extends TreeMap<ContentType, T> {

  @Getter
  @Setter
  private T defaultValue;

  /**
   * Constructs a ContentTypeMap based on the provided map
   * @param map the map
   */
  public ContentTypeMap(Map<ContentType, T> map) {
    super(map);
  }

  public ContentTypeMap() {}

  /**
   * Put a value for the provided content type
   *
   * @param contentType the content type
   * @param value       the value
   * @return the previous value for this content type, or null
   */
  public T putAt(ContentType contentType, T value) {
    return put(contentType, value);
  }

  @Override
  public T get(Object key) {
    if (key instanceof ContentType) {
      return getAt((ContentType) key);
    } else if (key == null) {
      // normally treemap doesn't accept null values.
      // instead we return the default value
      return defaultValue;
    } else {
      return super.get(key);
    }
  }

  /**
   * Returns the value associated with the provided key, or null
   * @param contentType the key
   * @return the value associated with the provided key, or null
   */
  public T getAt(ContentType contentType) {
    T t = super.get(contentType);
    if (t != null) {
      return t;
    }
    for (ContentType candidateKey : keySet()) {
      if (candidateKey.includes(contentType)) {
        return super.get(candidateKey);
      }
    }
    return defaultValue;
  }
}
