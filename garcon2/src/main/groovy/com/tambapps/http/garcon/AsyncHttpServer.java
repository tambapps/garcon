package com.tambapps.http.garcon;

import static com.tambapps.http.garcon.Headers.CONNECTION_CLOSE;
import static com.tambapps.http.garcon.Headers.CONNECTION_KEEP_ALIVE;

import com.tambapps.http.garcon.exception.BadProtocolException;
import com.tambapps.http.garcon.exception.BadRequestException;
import com.tambapps.http.garcon.io.composer.HttpResponseComposer;
import com.tambapps.http.garcon.logger.DefaultLogger;
import com.tambapps.http.garcon.logger.Logger;
import lombok.AllArgsConstructor;
import lombok.Setter;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

public class AsyncHttpServer {

  private final AtomicBoolean running = new AtomicBoolean(false);
  private final ByteBuffer buffer = ByteBuffer.allocateDirect(1024 * 64 - 1);
  private ServerSocketChannel serverSocket;
  private Selector selector;
  @Setter
  private Logger logger = new DefaultLogger();
  // TODO will be used by executor
  private final ConcurrentMap<SelectionKey, HttpResponse> pendingResponses = new ConcurrentHashMap<>();
  private Thread serverThread;

  private final ExecutorService executor;
  @Setter
  private HttpExchangeHandler exchangeHandler;

  public AsyncHttpServer(ExecutorService executor, HttpExchangeHandler exchangeHandler) {
    this.executor = executor;
    this.exchangeHandler = exchangeHandler;
  }

  // TODO configure request timeout
  public void stop() {
    if (!isRunning()) {
      return;
    }
    running.set(false);
    DefaultGroovyMethods.closeQuietly(selector);
    DefaultGroovyMethods.closeQuietly(serverSocket);
    executor.shutdown();
    try {
      serverThread.join();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return;
    }
  }

  // return true if server was actually started
  public boolean start(InetAddress address, Integer port) {
    if (isRunning()) {
      return false;
    }
    running.set(true);
    final Selector selector;
    final ServerSocketChannel serverSocket;
    try {
      selector = Selector.open();
      serverSocket = ServerSocketChannel.open();
      serverSocket.bind(new InetSocketAddress(address, port));
      // Put the ServerSocketChannel into non-blocking mode
      serverSocket.configureBlocking(false);
      // Now register the channel with the Selector. The SelectionKey
      // represents the registration of this channel with this Selector.
      serverSocket.register(selector, SelectionKey.OP_ACCEPT);
    } catch (IOException e) {
      logger.error("Couldn't start server", e);
      return false;
    }

    this.serverSocket = serverSocket;
    this.selector = selector;
    Runnable serverRunnable = () -> {
      try {
        while (running.get()) {
          selector.select();
          Set<SelectionKey> selectedKeys = selector.selectedKeys();
          Iterator<SelectionKey> iter = selectedKeys.iterator();
          while (iter.hasNext()) {
            SelectionKey key = iter.next();

            if (key.isAcceptable()) {
              accept(selector, serverSocket);
            } else if (key.isReadable()) {
              read(key);
            } else if (key.isWritable()) {
              write(key);
            }
            iter.remove();
          }
        }
      } catch (ClosedSelectorException ignored) {
      } catch (IOException e) {
        logger.error("Error while running server. Stopping it", e);
      } catch (Exception e) {
        logger.error("Unexpected Error while running server. Stopping it", e);
      }
      running.set(false);
      DefaultGroovyMethods.closeQuietly(selector);
      DefaultGroovyMethods.closeQuietly(serverSocket);
    };

    serverThread = new Thread(serverRunnable, "garcon-loop");
    serverThread.start();
    return true;
  }

  private void accept(Selector selector, ServerSocketChannel serverSocket) throws IOException {
    SocketChannel client = serverSocket.accept();
    client.configureBlocking(false);
    client.register(selector, SelectionKey.OP_READ, new HttpAttachment());
  }

  private void read(SelectionKey selectionKey) throws IOException {
    // for now we assume the buffer is big enough to read whole request
    SocketChannel ch = (SocketChannel) selectionKey.channel();
    buffer.clear();
    int read = ch.read(buffer);
    if (read == -1) {
      DefaultGroovyMethods.closeQuietly(selectionKey.channel());
    } else if (read > 0) {
      buffer.flip();
      HttpAttachment attachment = (HttpAttachment) selectionKey.attachment();
      try {
        HttpRequest request = attachment.parseRequest(buffer);
        if (request != null) {
          attachment.setPendingWrite(true);
          // we are done reading, now we need to handle the request
          executor.submit(new ExchangeRunnable(selectionKey, request));
          selectionKey.interestOps(SelectionKey.OP_WRITE);
        }
      } catch (BadProtocolException e) {
        DefaultGroovyMethods.closeQuietly(selectionKey.channel());
      } catch (BadRequestException e) {
        // TODO write error response
      }
    }
  }

  private void write(SelectionKey selectionKey) throws IOException {
    HttpResponse response = pendingResponses.remove(selectionKey);
    if (response == null) {
      // handler did not finish yet. Waiting
      return;
    }
    SocketChannel channel = (SocketChannel) selectionKey.channel();
    HttpResponseComposer.writeInto(channel, response);
    HttpAttachment attachment = (HttpAttachment) selectionKey.attachment();
    // TODO keep track of all open connections (selectionKey) to be able to close them all when stopping server
    if (response.isKeepAlive() && selectionKey.isValid()) {
      selectionKey.interestOps(SelectionKey.OP_READ);
      attachment.reset();
    } else {
      DefaultGroovyMethods.closeQuietly(channel);
    }
  }

  public boolean isRunning() {
    return running.get();
  }

  public void waitStop() {
    while (isRunning()) {
      try {
        Thread.sleep(500L);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return;
      }
    }
  }

  @AllArgsConstructor
  private class ExchangeRunnable implements Runnable {

    private final SelectionKey key;
    private final HttpRequest request;

    @Override
    public void run() {
      HttpResponse response;
      try {
        response = exchangeHandler.processExchange(request);
      } catch (Exception e) {
        logger.error("Error while processing exchange for request " + request, e);
        response = new HttpResponse();
        response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        response.setBody("An internal server error occurred");
        response.getHeaders().put(Headers.CONTENT_TYPE_HEADER, ContentType.TEXT.getHeaderValue());
      }
      addDefaultHeaders(request, response);
      pendingResponses.put(key, response);
    }

    private void addDefaultHeaders(HttpRequest request, HttpResponse response) {
      Headers responseHeaders = response.getHeaders();
      responseHeaders.put("Server", "Garcon (Tambapps)");
      Integer contentLength = response.getContentLength();
      if (contentLength != null) {
        responseHeaders.put("Content-Length", contentLength.toString());
      }

      String connectionHeader = responseHeaders.getConnectionHeader();
      if (connectionHeader == null) {
        // keep connection alive if request body and response body are with definite length AND client want so
        responseHeaders.putConnectionHeader(response.is2xxSuccessful()
            && contentLength != null
            && request != null
            && CONNECTION_KEEP_ALIVE.equalsIgnoreCase(request.getHeaders().getConnectionHeader())
            ? CONNECTION_KEEP_ALIVE : CONNECTION_CLOSE);
      }
    }
  }
}
