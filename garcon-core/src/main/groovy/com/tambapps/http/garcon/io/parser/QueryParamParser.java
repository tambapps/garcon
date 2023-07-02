package com.tambapps.http.garcon.io.parser;

import com.tambapps.http.garcon.exception.BadProtocolException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class QueryParamParser {

  public static Map<String, String> parseFromPath(String path) {
    int index = path.indexOf('?');
    if (index < 0 || index + 1 == path.length()) {
      return Collections.emptyMap();
    } else {
      return parseQueryParams(path.substring(index + 1));
    }
  }
  public static Map<String, String> parseQueryParams(String paramsString) {
    Map<String, String> queryParams = new HashMap<>();
    parseQueryParams(paramsString, queryParams);
    return queryParams;
  }

  public static void parseQueryParams(String paramsString, Map<String, String> queryParams) {
    if (paramsString == null || paramsString.isEmpty()) {
      return;
    }
    String[] params = paramsString.split("&");
    for (String param : params) {
      String[] fields = param.split("=");
      if (fields.length == 2) {
        queryParams.put(urlDecode(fields[0]), urlDecode(fields[1]));
      } else if (fields.length == 1) {
        queryParams.put(urlDecode(fields[0]), String.valueOf(true));
      }
    }
  }

  private static String urlDecode(String s) {
    try {
      return URLDecoder.decode(s, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new BadProtocolException("Couldn't URL decode", e);
    }
  }
}
