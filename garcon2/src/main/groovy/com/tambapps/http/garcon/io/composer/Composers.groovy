package com.tambapps.http.garcon.io.composer

import com.tambapps.http.garcon.ContentType
import com.tambapps.http.garcon.exception.ComposingException
import com.tambapps.http.garcon.exception.ParsingException
import com.tambapps.http.garcon.util.ContentTypeMap
import groovy.json.JsonOutput
import org.codehaus.groovy.runtime.MethodClosure

import static com.tambapps.http.garcon.util.ClassUtils.doIfClassExists

/**
 * Utility class holding several common composers.
 * An composer should return a byte array
 *
 */
final class Composers {

  private Composers() {}

  static ContentTypeMap<Closure<?>> getMap() {
    ContentTypeMap<Closure<?>> map = new ComposingMap()
    doIfClassExists('groovy.json.JsonOutput') { Class c ->
      map[ContentType.JSON] = Composers.&composeJsonBody
    }
    doIfClassExists('groovy.xml.XmlSlurper') { Class c ->
      map[ContentType.XML] = Composers.&composeXmlBody
    }

    map.put(ContentType.TEXT, Composers.&composeStringBody)
    map.put(ContentType.HTML, Composers.&composeStringBody)
    map.put(ContentType.BINARY, Composers.&composeBytesBody)
    // default composer (when no content type was found)
    map.setDefaultValue(Composers.&composeStringBody)
    return map
  }

  static byte[] composeJsonBody(Object body) {
    return JsonOutput.toJson(body).getBytes()
  }

  static byte[] composeXmlBody(Object body) {
    String xmlData
    if (body instanceof CharSequence) {
      xmlData = body.toString()
    } else if (body instanceof Node) {
      xmlData = groovy.xml.XmlUtil.serialize((Node) body)
    } else {
      throw new IllegalArgumentException("body must be a String or a groovy.util.Node to be serialized to XML")
    }
    return xmlData.getBytes()
  }

  static byte[] composeStringBody(Object body) {
    return String.valueOf(body).getBytes()
  }

  static byte[] composeBytesBody(Object body) throws IOException {
    if (body == null) {
      return null
    }
    switch (body) {
      case byte[]:
        return (byte[]) body
      case Byte[]:
        Byte[] bytes = (Byte[]) body
        byte[] bytes2 = new byte[bytes.length]
        for (i in 0..<bytes.length) {
          bytes2[i] = bytes[i]
        }
        return bytes2
      case InputStream:
        return ((InputStream) body).getBytes()
      default:
        throw new IllegalArgumentException("Body must be a byte array or an InputStream to be serialized to bytes")
    }
  }

  private static class ComposingMap extends ContentTypeMap<Closure<?>> {
    @Override
    Closure<?> put(ContentType key, Closure<?> value) {
      return super.put(key, new ComposingClosure(value))
    }
  }

  private static class ComposingClosure extends Closure {

    private final Closure closure

    ComposingClosure(Closure closure) {
      super(null)
      this.closure = closure
    }

    def doCall(Object arg) {
      try {
        return closure.call(arg)
      } catch (Exception e) {
        throw new ComposingException(e)
      }
    }
  }

}
