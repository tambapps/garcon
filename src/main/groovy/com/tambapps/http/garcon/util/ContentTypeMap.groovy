package com.tambapps.http.garcon.util;

import com.tambapps.http.garcon.ContentType;

/**
 * A map of content type -&gt; T that handles content types inclusion when retrieving an element
 * @param <T> the type of the map values
 */
// using TreeMap to have ContentType sorted when iterating over keys and checking inclusion
class ContentTypeMap<T> extends TreeMap<ContentType, T> {

  T defaultValue

  ContentTypeMap(Map<ContentType, T> map) {
    super(map)
  }

  ContentTypeMap() {}

  T putAt(ContentType contentType, T value) {
    return put(contentType, value);
  }

  @Override
  T get(Object key) {
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

  T getAt(ContentType contentType) {
    if (contentType == null) {
      return super.get(null)
    }
    if (containsKey(contentType)) {
      return super.get(contentType)
    }
    for (ContentType candidateKey : keySet()) {
      if (candidateKey.includes(contentType)) {
        return super.get(candidateKey)
      }
    }
    return defaultValue
  }
}
