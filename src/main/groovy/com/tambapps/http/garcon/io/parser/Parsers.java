package com.tambapps.http.garcon.io.parser;

import com.tambapps.http.garcon.ContentType;
import com.tambapps.http.garcon.util.ContentTypeFunctionMap;
import groovy.json.JsonSlurper;
import groovy.xml.XmlSlurper;
import lombok.SneakyThrows;

import static com.tambapps.http.garcon.util.ClassUtils.doIfClassExists;

import java.io.ByteArrayInputStream;
import java.util.function.Function;

public class Parsers {

  private Parsers() {}

  public static ContentTypeFunctionMap<byte[], Object> getMap() {
    ContentTypeFunctionMap<byte[], Object> map = new ContentTypeFunctionMap<>();
    doIfClassExists("groovy.json.JsonSlurper", (c) -> map.putAt(ContentType.JSON, new ParseJsonFunction()));
    doIfClassExists("groovy.xml.XmlSlurper", (c) -> map.putAt(ContentType.XML, new ParseXmlFunction()));
    map.setDefaultValue(new ParseStringFunction());
    return map;
  }

  // using class because they might be faster than lambda functions
  public static class ParseJsonFunction implements Function<byte[], Object> {

    @Override
    public Object apply(byte[] bytes) {
      return new JsonSlurper().parse(bytes);
    }
  }

  public static class ParseXmlFunction implements Function<byte[], Object> {

    @SneakyThrows
    @Override
    public Object apply(byte[] bytes) {
      return new XmlSlurper().parse(new ByteArrayInputStream(bytes));
    }
  }

  public static class ParseStringFunction implements Function<byte[], Object> {

    @Override
    public Object apply(byte[] bytes) {
      return new String(bytes);
    }
  }
}
