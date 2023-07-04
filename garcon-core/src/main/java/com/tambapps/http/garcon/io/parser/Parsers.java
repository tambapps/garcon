package com.tambapps.http.garcon.io.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tambapps.http.garcon.ContentType;
import com.tambapps.http.garcon.util.ContentTypeMap;
import lombok.SneakyThrows;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class Parsers {

  private Parsers() {}

  public static ContentTypeMap<Function<byte[], Object>> getMap() {
    ContentTypeMap<Function<byte[], Object>> map = new ContentTypeMap<>();
    Function<byte[], Object> parseStringResponseBody = Parsers::parseStringBody;

    ObjectMapper objectMapper = new ObjectMapper();
    map.put(ContentType.JSON, b -> parseJsonBody(objectMapper, b));
    map.put(ContentType.TEXT, parseStringResponseBody);
    map.put(ContentType.HTML, parseStringResponseBody);
    map.put(ContentType.BINARY, body -> body);
    // default parser (when no content type was found)
    map.setDefaultValue((o) -> parseStringResponseBody);
    return map;
  }

  public static String parseStringBody(byte[] body) {
    String text = new String(body);
    if (text.isEmpty()) {
      return "(No content)";
    }
    return text;
  }

  @SneakyThrows
  public static Object parseJsonBody(ObjectMapper objectMapper, byte[] body) {
    if (body.length == 0) {
      return "(No content)";
    }
    try {
      JsonNode node = objectMapper.readTree(body);
      return toObject(node);
    } catch (Exception e) {
      throw new IllegalArgumentException(e.getMessage(), e);
    }
  }

  public static Object toObject(JsonNode node) {
    if (node.isDouble()) return node.asDouble();
    else if (node.isFloat()) return node.floatValue();
    else if (node.isLong()) return node.asLong();
    else if (node.isInt()) return node.asInt();
    else if (node.isTextual()) return node.asText();
    else if (node.isArray()) {
      List<Object> list = new ArrayList<>();
      for (int i = 0; i < node.size(); i++) {
        list.add(toObject(node.get(i)));
      }
      return list;
    } else if (node.isEmpty()) {
      return new HashMap<>();
    } else if (node.isObject()) {
      Map<String, Object> map = new HashMap<>();
      node.fields().forEachRemaining(e -> map.put(e.getKey(), toObject(e.getValue())));
      return map;
    } else {
      throw new RuntimeException("Internal error, doesn't handle such node");
    }
  }
}
