package com.tambapps.http.garcon

import com.tambapps.http.garcon.exception.RequestParsingException
import com.tambapps.http.garcon.exception.StreamTooLongException
import com.tambapps.http.garcon.io.LimitedInputStream
import com.tambapps.http.garcon.io.RequestParser
import com.tambapps.http.garcon.util.IoUtils
import groovy.transform.CompileStatic
import groovy.transform.PackageScope

import static com.tambapps.http.garcon.Headers.CONNECTION_CLOSE
import static com.tambapps.http.garcon.Headers.CONNECTION_KEEP_ALIVE

@CompileStatic
@PackageScope
class AndroidHttpExchangeHandler implements HttpExchangeHandler, Runnable {

  private Socket socket
  private Collection<Closeable> connections
  private EndpointsHandler endpointsHandler

  @Override
  void run() {
    try {
      InputStream inputStream = garcon.getMaxRequestBytes() != null
          ? new LimitedInputStream(socket.getInputStream(), garcon.getMaxRequestBytes())
          : socket.getInputStream()
      OutputStream outputStream = socket.getOutputStream()
      while (garcon.isRunning()) {
        HttpRequest request = null
        HttpResponse response
        if (inputStream instanceof LimitedInputStream) {
          LimitedInputStream limitedInputStream = (LimitedInputStream) inputStream
          limitedInputStream.resetBytesRead()
          // in case it has been modified since
          limitedInputStream.setMaxBytes(garcon.getMaxRequestBytes())
        }
        try {
          request = RequestParser.parse(inputStream)
          response = processExchange(request)
        } catch (RequestParsingException e) {
          response = new HttpResponse()
          response.setStatusCode(HttpStatus.BAD_REQUEST)
          response.getHeaders().putConnectionHeader(CONNECTION_CLOSE)
        } catch (StreamTooLongException e) {
          response = new HttpResponse()
          response.setStatusCode(HttpStatus.REQUEST_ENTITY_TOO_LARGE)
          response.getHeaders().putConnectionHeader(CONNECTION_CLOSE)
        }
        addDefaultHeaders(request, response)
        // writing response
        PrintWriter writer = new PrintWriter(outputStream);
        writer.format("%s %d %s", response.httpVersion, response.statusCode.value, response.statusCode.message).println();
        response.headers.forEach((name, value) -> writer.println(name + ": " + value))
        writer.println()
        writer.flush()
        response.writeBody(outputStream)
        if (!CONNECTION_KEEP_ALIVE.equalsIgnoreCase(response.getHeaders().getConnectionHeader())) {
          break
        }
      }
    } catch (EOFException | SocketException e) {
      // do nothing
      onConnectionClosed(e)
    } catch (SocketTimeoutException e) {
      // client took too much time to write anything
    } catch (IOException e) {
      onConnectionError(e)
    } catch (Exception e) {
      onUnexpectedError(e)
    } finally {
      // closing socket will also close InputStream and OutputStream
      IoUtils.closeQuietly(socket)
      connections.remove(socket)
    }
  }

  @Override
  List<EndpointDefinition> findPathEndpoints(String path) {
    return endpointsHandler.getDefinitionsForPath(path)
  }

  private void onConnectionClosed(IOException e) {
    garcon.onConnectionClosed?.call(e)
  }

  private void onConnectionError(IOException e) {
    garcon.onConnectionError?.call(e)
  }

  private void onUnexpectedError(Exception e) {
    garcon.onConnectionUnexpectedError?.call(e)
  }
}
