package com.tambapps.http.garcon;

import com.tambapps.http.garcon.io.parser.HttpRequestParser;
import lombok.Data;

@Data
public class HttpAttachment {
  private final HttpRequestParser requestParser = new HttpRequestParser();
}
