package com.tambapps.http.garcon.io.composer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tambapps.http.garcon.ContentType;
import com.tambapps.http.garcon.util.ContentTypeMap;
import com.tambapps.http.garcon.util.IoUtils;
import lombok.SneakyThrows;

import java.io.InputStream;
import java.util.function.Function;

public final class Composers {

  private Composers() {}
  public static ContentTypeMap<Function<Object, byte[]>> getMap() {
    ObjectMapper mapper = IoUtils.newObjectMapper();
    ContentTypeMap<Function<Object, byte[]>> map = new ContentTypeMap<>();
    map.put(ContentType.JSON, (o) -> composeJsonBody(mapper, o));
    map.put(ContentType.TEXT, Composers::composeStringBody);
    map.put(ContentType.HTML, Composers::composeStringBody);
    map.put(ContentType.BINARY, Composers::composeBytesBody);
    // default composer (when no content type was found)
    map.setDefaultValue(Composers::composeStringBody);
    return map;
  }

  @SneakyThrows
  public static byte[] composeJsonBody(ObjectMapper mapper, Object body) {
    try {
      return mapper.writeValueAsBytes(body);
    } catch (Exception e) {
      throw new IllegalArgumentException(e.getMessage(), e);
    }
  }

  public static byte[] composeStringBody(Object body) {
    return body != null ? String.valueOf(body).getBytes() : new byte[0];
  }

  @SneakyThrows
  public static byte[] composeBytesBody(Object body) {
    if (body == null) {
      return null;
    }
    if (body.getClass() == byte[].class) {
      return (byte[]) body;
    } else if (body.getClass() == Byte[].class) {
      Byte[] bytes = (Byte[]) body;
      byte[] bytes2 = new byte[bytes.length];
      for (int i = 0; i < bytes2.length; i++) {
        bytes2[i] = bytes[i];
      }
      return bytes2;
    } else if (body instanceof InputStream) {
      return IoUtils.getBytes((InputStream) body);
    } else {
      throw new IllegalArgumentException(
              "Body must be a byte array or an InputStream to be serialized to bytes");
    }
  }

}
