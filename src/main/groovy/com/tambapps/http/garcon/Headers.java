package com.tambapps.http.garcon;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Class representing. Note that for performance purpose, when accessing a header you should always
 * use the dash-separated, with upper-case character for each world syntax when accessing a header,
 * (e.g. 'Connection', 'Content-Type') otherwise you won't find it
 */
public class Headers extends HashMap<String, String> {

  public static final String CONNECTION_KEEP_ALIVE = "Keep-Alive";
  public static final String CONNECTION_CLOSE = "Close";
  public static final String CONNECTION_HEADER = "Connection";
  public static final String CONTENT_TYPE_HEADER = "Content-Type";
  public static final String CONTENT_LENGTH_HEADER = "Content-Length";
  public static final String TRANSFER_ENCODING_HEADER = "Transfer-Encoding";

  public Headers() {}

  public Headers(Map<?, ?> map) {
    map.forEach((name, value) -> putAt(String.valueOf(name), String.valueOf(value)));
  }

  public String putAt(String name, Object value) {
    return put(name, String.valueOf(value));
  }

  public String getAt(String name) {
    return get(name);
  }

  public String getSafe(String name) {
    return get(formattedKey(name));
  }

  public void putConnectionHeader(String value) {
    putUnsafe(CONNECTION_HEADER, value);
  }

  /**
   * Returns the connection header
   * @return the connection header
   */
  public String getConnectionHeader() {
    return getAt(CONNECTION_HEADER);
  }

  /**
   * Put the content type header based on the provided {@link ContentType}
   * @param contentType the content type
   */
  public void putContentType(ContentType contentType) {
    putUnsafe(CONTENT_TYPE_HEADER, contentType.getHeaderValue());
  }

  public String getContentTypeHeader() {
    return getAt(CONTENT_TYPE_HEADER);
  }
  public ContentType getContentType() {
    String header = getAt(CONTENT_TYPE_HEADER);
    return header != null ? ContentType.valueOf(header) : null;
  }

  public void putContentLength(Number value) {
    putUnsafe(CONTENT_LENGTH_HEADER, String.valueOf(value));
  }

  public Long getContentLength() {
    String header = get(CONTENT_LENGTH_HEADER);
    if (header == null) {
      return null;
    }
    try {
      return Long.parseLong(header);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  // put without formatting key, supposing it is already well formatted
  private String putUnsafe(String key, String value) {
    return super.put(key, value);
  }
  public String put(String key, String value) {
    return super.put(formattedKey(key), value);
  }

  @Override
  public String putIfAbsent(String key, String value) {
    return super.putIfAbsent(formattedKey(key), value);
  }

  @Override
  public String compute(String key,
      BiFunction<? super String, ? super String, ? extends String> remappingFunction) {
    return super.compute(formattedKey(key), remappingFunction);
  }

  @Override
  public String computeIfAbsent(String key,
      Function<? super String, ? extends String> mappingFunction) {
    return super.computeIfAbsent(formattedKey(key), mappingFunction);
  }

  @Override
  public String computeIfPresent(String key,
      BiFunction<? super String, ? super String, ? extends String> remappingFunction) {
    return super.computeIfPresent(formattedKey(key), remappingFunction);
  }

  @Override
  public String remove(Object key) {
    return super.remove(key != null ? formattedKey(key.toString()) : null);
  }

  @Override
  public boolean remove(Object key, Object value) {
    return super.remove(key != null ? formattedKey(key.toString()) : null, value);
  }

  private String formattedKey(String s) {
    char[] chars = new char[s.length()];
    for (int i = 0; i < chars.length; i++) {
      char c = s.charAt(i);
      chars[i] = i == 0 || s.charAt(i - 1) == '-' ? Character.toUpperCase(c) : Character.toLowerCase(c);
    }
    return new String(chars);
  }

  @Override
  public boolean containsKey(Object key) {
    return super.containsKey(key != null ? formattedKey(key.toString()) : null);
  }

  @Override
  public void putAll(Map<? extends String, ? extends String> m) {
    m.forEach(this::put);
  }

  public ImmutableHeaders asImmutable() {
    return new ImmutableHeaders(this);
  }

}
