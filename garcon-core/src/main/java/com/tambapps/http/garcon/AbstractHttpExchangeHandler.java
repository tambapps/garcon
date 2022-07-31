package com.tambapps.http.garcon;

import com.tambapps.http.garcon.exception.RequestParsingException;
import com.tambapps.http.garcon.io.RequestParser;
import lombok.AllArgsConstructor;

import static com.tambapps.http.garcon.Headers.CONNECTION_CLOSE;
import static com.tambapps.http.garcon.Headers.CONNECTION_KEEP_ALIVE;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.Collection;

@AllArgsConstructor
abstract class AbstractHttpExchangeHandler implements Runnable {

  private final Socket socket;
  protected final AbstractGarcon garcon;
  private final Collection<Closeable> connections;

  @Override
  public void run() {
    try {
      InputStream inputStream = socket.getInputStream();
      OutputStream outputStream = socket.getOutputStream();
      while (garcon.isRunning()) {
        HttpRequest request = null;
        HttpResponse response;
        try {
          request = RequestParser.parse(inputStream);
          response = processExchange(request);
        } catch (RequestParsingException e) {
          response = new HttpResponse();
          response.setStatusCode(HttpStatus.BAD_REQUEST);
          response.getHeaders().putConnectionHeader(CONNECTION_CLOSE);
        }
        addDefaultHeaders(request, response);
        response.writeInto(outputStream);
        if (!CONNECTION_KEEP_ALIVE.equals(response.getHeaders().getConnectionHeader())) {
          break;
        }
      }
    } catch (EOFException | SocketException e) {
      // do nothing
    } catch (IOException e) {
      // TODO handle errors
      e.printStackTrace();
    } catch (Exception e) {
      // TODO handle errors
      e.printStackTrace();
    } finally {
      // closing socket will also close InputStream and OutputStream
      try {
        socket.close();
      }catch (IOException ignored) {}
      connections.remove(socket);
    }
  }

  private void addDefaultHeaders(HttpRequest request, HttpResponse response) {
    Headers responseHeaders = response.getHeaders();
    responseHeaders.put("Server", "Garcon (Tambapps)");
    Long contentLength = response.getContentLength();
    if (contentLength != null) {
      responseHeaders.put("Content-Length", contentLength.toString());
    }
    String connectionHeader = responseHeaders.getConnectionHeader();
    if (connectionHeader == null) {
      // keep connection alive if request body and response body are with definite length AND client want so
      responseHeaders.putConnectionHeader(response.is2xxSuccessful()
          && contentLength != null
          && request != null
          && CONNECTION_KEEP_ALIVE.equals(request.getHeaders().getConnectionHeader())
          && request.getHeaders().getContentLength() != null
          ? CONNECTION_KEEP_ALIVE : CONNECTION_CLOSE);
    }
  }

  protected abstract HttpResponse processExchange(HttpRequest request);
}
