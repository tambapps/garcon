package com.tambapps.http.garcon;

import com.tambapps.http.garcon.exception.RequestParsingException;
import com.tambapps.http.garcon.exception.StreamTooLongException;
import com.tambapps.http.garcon.io.LimitedInputStream;
import com.tambapps.http.garcon.io.RequestParser;
import com.tambapps.http.garcon.util.IoUtils
import groovy.transform.PackageScope;

import static com.tambapps.http.garcon.Headers.CONNECTION_CLOSE;
import static com.tambapps.http.garcon.Headers.CONNECTION_KEEP_ALIVE;

@PackageScope
// TODO remove from core and/or merge it with HttpExchangeHandler
abstract class AbstractHttpExchangeHandler implements HttpExchangeHandler, Runnable {

  private final Socket socket;
  protected final AbstractGarcon garcon;
  private final Collection<Closeable> connections;

  AbstractHttpExchangeHandler(Socket socket, AbstractGarcon garcon, Collection<Closeable> connections) {
    this.socket = socket
    this.garcon = garcon
    this.connections = connections
  }

  @PackageScope
  abstract void onConnectionClosed(IOException e);
  @PackageScope
  abstract void onConnectionError(IOException e);
  @PackageScope
  abstract void onUnexpectedError(Exception e);
  @Override
  public void run() {
    try {
      InputStream inputStream = garcon.getMaxRequestBytes() != null
          ? new LimitedInputStream(socket.getInputStream(), garcon.getMaxRequestBytes())
          : socket.getInputStream();
      OutputStream outputStream = socket.getOutputStream();
      while (garcon.isRunning()) {
        HttpRequest request = null;
        HttpResponse response;
        if (inputStream instanceof LimitedInputStream) {
          LimitedInputStream limitedInputStream = (LimitedInputStream) inputStream;
          limitedInputStream.resetBytesRead();
          // in case it has been modified since
          limitedInputStream.setMaxBytes(garcon.getMaxRequestBytes());
        }
        try {
          request = RequestParser.parse(inputStream);
          response = processExchange(request);
        } catch (RequestParsingException e) {
          response = new HttpResponse();
          response.setStatusCode(HttpStatus.BAD_REQUEST);
          response.getHeaders().putConnectionHeader(CONNECTION_CLOSE);
        } catch (StreamTooLongException e) {
          response = new HttpResponse();
          response.setStatusCode(HttpStatus.REQUEST_ENTITY_TOO_LARGE);
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
      onConnectionClosed(e);
    } catch (SocketTimeoutException e) {
      // client took too much time to write anything
    } catch (IOException e) {
      onConnectionError(e);
    } catch (Exception e) {
      onUnexpectedError(e);
    } finally {
      // closing socket will also close InputStream and OutputStream
      IoUtils.closeQuietly(socket);
      connections.remove(socket);
    }
  }

  protected abstract HttpResponse processExchange(HttpRequest request);
}
