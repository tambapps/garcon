package com.tambapps.http.garcon.io;

import com.tambapps.http.garcon.exception.RequestParsingException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

public class QueryParamParser {

  public static Map<String, String> parseQueryParams(String paramsString) throws IOException {
    Map<String, String> queryParams = new HashMap<>();
    parseQueryParams(paramsString, queryParams);
    return queryParams;
  }

  public static void parseQueryParams(String paramsString, Map<String, String> queryParams) throws IOException {
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

  private static String urlDecode(String s) throws IOException {
    try {
      return URLDecoder.decode(s, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new RequestParsingException("Couldn't URL decode", e);
    }
  }
}
