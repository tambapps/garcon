package com.tambapps.http.garcon.io.composer

import com.tambapps.http.garcon.ContentType
import com.tambapps.http.garcon.util.ContentTypeMap
import groovy.json.JsonOutput
import groovy.xml.XmlUtil

/**
 * Utility class holding several common composers.
 * An composer should return one of the following types
 * - a byte array (primitive byte, not Byte)
 * - an InputStream
 * - a String
 *
 */
final class Composers {

  private Composers() {}

  static ContentTypeMap<Closure<?>> getMap() {
    ContentTypeMap<Closure<?>> map = new ContentTypeMap<>()
    // TODO compose closure and try catch to throw composing exception instead, and handle it better
    map.put(ContentType.JSON, JsonOutput.&toJson)
    map.put(ContentType.XML, Composers.&composeXmlBody)
    map.put(ContentType.TEXT, Composers.&composeStringBody)
    map.put(ContentType.HTML, Composers.&composeStringBody)
    map.put(ContentType.BINARY, Composers.&composeBytesBody)
    // TODO map.put(ContentType.URL_ENCODED, new MethodClosure(queryParamComposer, "composeToString"))
    // default composer (when no content type was found)
    map.setDefaultValue(Composers.&composeStringBody)
    return map
  }

  static String composeJsonBody(Object body) {
    return JsonOutput.toJson(body)
  }

  static String composeXmlBody(Object body) {
    String xmlData
    if (body instanceof CharSequence) {
      xmlData = body.toString()
    } else if (body instanceof Node) {
      xmlData = XmlUtil.serialize((Node) body)
    } else {
      throw new IllegalArgumentException("body must be a String or a groovy.util.Node to be serialized to XML")
    }
    return xmlData
  }

  static String composeStringBody(Object body) {
    return String.valueOf(body)
  }

  static InputStream composeBytesBody(Object body) throws IOException {
    switch (body) {
      case byte[]:
        return new ByteArrayInputStream((byte[]) body)
      case Byte[]:
        Byte[] bytes = (Byte[]) body
        byte[] bytes2 = new byte[bytes.length]
        return new ByteArrayInputStream(bytes2)
      case InputStream:
        return (InputStream) body
      default:
        throw new IllegalArgumentException("Body must be a byte array or an InputStream to be serialized to bytes")
    }
  }


}
