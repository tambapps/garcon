package com.tambapps.http.garcon.io.composer;

import com.tambapps.http.garcon.HttpResponse;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.Map;

public class HttpResponseComposer {

  private static final String NEW_LINE = "\r\n";

  public static void writeInto(WritableByteChannel channel, HttpResponse response) throws IOException {
    StringBuilder builder = new StringBuilder();
    builder.append(response.getHttpVersion()).append(" ")
            .append(response.getStatusCode().getValue()).append(" ")
            .append(response.getStatusCode().getMessage()).append(NEW_LINE);
    for (Map.Entry<String, String> entry : response.getHeaders().entrySet()) {
      builder.append(entry.getKey()).append(": ").append(entry.getValue()).append(NEW_LINE);
    }
    builder.append(NEW_LINE);
    channel.write(ByteBuffer.wrap(builder.toString().getBytes()));
    if (response.getBody() != null) {
      channel.write(response.getBody());
    }
  }
}
