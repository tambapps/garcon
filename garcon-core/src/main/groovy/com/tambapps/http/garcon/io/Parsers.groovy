package com.tambapps.http.garcon.io

import com.tambapps.http.garcon.ContentType
import com.tambapps.http.garcon.RequestBody
import com.tambapps.http.garcon.exception.ParsingException
import com.tambapps.http.garcon.util.ContentTypeMap
import groovy.json.JsonSlurper
import groovy.xml.XmlSlurper

import static com.tambapps.http.garcon.util.ClassUtils.doIfClassExists

class Parsers {

  private Parsers() {}

  static ContentTypeMap<Closure<?>> getMap() {
    ContentTypeMap<Closure<?>> map = new ParsingMap()

    doIfClassExists('groovy.json.JsonSlurper') { Class c ->
      map[ContentType.JSON] = Parsers.&parseJson
    }
    doIfClassExists('groovy.xml.XmlSlurper') { Class c ->
      map[ContentType.XML] = Parsers.&parseXml
    }
    map.setDefaultValue(Parsers.&parseStringResponseBody)
    map[ContentType.HTML] = map.getDefaultValue()
    map[ContentType.TEXT] = map.getDefaultValue()

    return map
  }

  static Object parseJson(RequestBody body) {
    return new JsonSlurper().parseText(body.getText())
  }

  static Object parseXml(RequestBody body) {
    return new XmlSlurper().parseText(body.getText())
  }

  static String parseStringResponseBody(RequestBody body) {
    return body.getText();
  }

  private static class ParsingMap extends ContentTypeMap<Closure<?>> {

    @Override
    Closure<?> put(ContentType key, Closure<?> value) {
      return super.put(key, new ParsingClosure(value))
    }
  }

  private static class ParsingClosure extends Closure {

    private final Closure closure
    ParsingClosure(Closure closure) {
      super(null)
      this.closure = closure
    }

    def doCall(Object arg) {
      try {
        return closure.call(arg)
      } catch (Exception e) {
        throw new ParsingException(e)
      }
    }
  }
}
