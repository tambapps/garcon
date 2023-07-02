package com.tambapps.http.garcon.io.composer;

import com.tambapps.http.garcon.ContentType;
import com.tambapps.http.garcon.util.ContentTypeFunctionMap;
import groovy.json.JsonOutput;
import groovy.util.Node;
import lombok.SneakyThrows;
import org.codehaus.groovy.runtime.IOGroovyMethods;

import static com.tambapps.http.garcon.util.ClassUtils.doIfClassExists;

import java.io.InputStream;
import java.util.function.Function;

/**
 * Utility class holding several common composers.
 * An composer should return a byte array
 *
 */
public final class Composers {

  private Composers() {}

  /**
   * Returns the map of all default content type composers
   * @return a map of all default content type composers
   */
  public static ContentTypeFunctionMap<Object, byte[]> getMap() {
    ContentTypeFunctionMap<Object, byte[]> map = new ContentTypeFunctionMap<>();
    doIfClassExists("groovy.json.JsonOutput", (c) -> map.putAt(ContentType.JSON, new ComposeJsonFunction()));
    doIfClassExists("groovy.xml.XmlSlurper", (c) -> map.putAt(ContentType.XML, new ComposeXmlFunction()));
    map.put(ContentType.BINARY, new ComposeBytesFunction());
    // default composer (when no content type was found)
    map.setDefaultValue(new ComposeStringFunction());
    return map;
  }

  public static class ComposeJsonFunction implements Function<Object, byte[]> {
    @Override
    public byte[] apply(Object object) {
      return JsonOutput.toJson(object).getBytes();
    }
  }

  public static class ComposeXmlFunction implements Function<Object, byte[]> {
    @Override
    public byte[] apply(Object body) {
      String xmlData;
      if (body instanceof CharSequence) {
        xmlData = body.toString();
      } else if (body instanceof Node) {
        xmlData = groovy.xml.XmlUtil.serialize((Node) body);
      } else {
        throw new IllegalArgumentException("body must be a String or a groovy.util.Node to be serialized to XML");
      }
      return xmlData.getBytes();
    }
  }

  public static class ComposeStringFunction implements Function<Object, byte[]> {
    @Override
    public byte[] apply(Object object) {
      return String.valueOf(object).getBytes();
    }
  }

  public static class ComposeBytesFunction implements Function<Object, byte[]> {

    @SneakyThrows
    @Override
    public byte[] apply(Object body) {
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
        return IOGroovyMethods.getBytes((InputStream) body);
      } else {
        throw new IllegalArgumentException(
            "Body must be a byte array or an InputStream to be serialized to bytes");
      }
    }
  }

}
