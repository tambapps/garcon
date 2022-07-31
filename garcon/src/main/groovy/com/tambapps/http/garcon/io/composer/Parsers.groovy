package com.tambapps.http.garcon.io.composer

import com.tambapps.http.garcon.ContentType
import com.tambapps.http.garcon.exception.ParsingException
import com.tambapps.http.garcon.util.ContentTypeMap
import groovy.json.JsonSlurper
import groovy.xml.XmlSlurper

class Parsers {

  private Parsers() {}

  static ContentTypeMap<Closure<?>> getMap() {
    ContentTypeMap<Closure<?>> map = new ParsingMap()

    map[ContentType.JSON] = new JsonSlurper().&parse
    map[ContentType.XML] = new XmlSlurper().&parse
    map.setDefaultValue(Parsers.&parseStringResponseBody)
    map[ContentType.HTML] = map.getDefaultValue()
    map[ContentType.TEXT] = map.getDefaultValue()

    return map
  }

  static String parseStringResponseBody(InputStream body) {
    // we don't use InputStream.text because it closes the stream
    Reader reader = new InputStreamReader(body)
    StringBuilder answer = new StringBuilder();
    // reading the content of the file within a char buffer
    // allow to keep the correct line endings
    char[] charBuffer = new char[8192];
    int nbCharRead /* = 0*/;
    while ((nbCharRead = reader.read(charBuffer)) != -1) {
      // appends buffer
      answer.append(charBuffer, 0, nbCharRead);
    }
    return answer.toString();
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
