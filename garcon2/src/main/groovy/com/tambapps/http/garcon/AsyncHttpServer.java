package com.tambapps.http.garcon;

import com.tambapps.http.garcon.logger.DefaultLogger;
import com.tambapps.http.garcon.logger.Logger;
import lombok.Setter;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class AsyncHttpServer {

  private final AtomicBoolean running = new AtomicBoolean(false);
  private final ByteBuffer buffer = ByteBuffer.allocateDirect(1024 * 64 - 1);
  private ServerSocketChannel serverSocket;
  private Selector selector;
  @Setter
  private Logger logger = new DefaultLogger();

  public void stop() {
    if (!isRunning()) {
      return;
    }
    running.set(false);
    DefaultGroovyMethods.closeQuietly(selector);
    DefaultGroovyMethods.closeQuietly(serverSocket);
  }

  // return true if server was actually started
  public boolean start() {
    if (isRunning()) {
      return false;
    }
    running.set(true);
    final Selector selector;
    final ServerSocketChannel serverSocket;
    try {
      selector = Selector.open();
      serverSocket = ServerSocketChannel.open();
      serverSocket.bind(new InetSocketAddress("localhost", 8081));
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
      }
      running.set(false);
      DefaultGroovyMethods.closeQuietly(selector);
      DefaultGroovyMethods.closeQuietly(serverSocket);
    };

    new Thread(serverRunnable, "garcon-loop").start();
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
      if (attachment.getRequestParser().parse(buffer)) {
        // if parsing completed, write response
        // TODO add a handler to handle response
        write(selectionKey);
      }
    }
  }

  private void write(SelectionKey selectionKey) throws IOException {
    SocketChannel channel = (SocketChannel) selectionKey.channel();
    channel.write(    ByteBuffer.wrap((    "HTTP/1.0 200 OK\r\n" +
        "Date: Fri, 31 Dec 1999 23:59:59 GMT\r\n" +
        "Server: Apache/0.8.4\r\n" +
        "Content-Type: text/html\r\n" +
        "Content-Length: 59\r\n" +
        "Expires: Sat, 01 Jan 2000 00:59:59 GMT\r\n" +
        "Last-modified: Fri, 09 Aug 1996 14:21:40 GMT\r\n" +
        "\r\n" +
        "<TITLE>Exemple</TITLE>\r\n" +
        "<P>Ceci est une page d'exemple.</P>"
    ).getBytes(StandardCharsets.UTF_8))
    );
    channel.close();
  }

  public boolean isRunning() {
    return running.get();
  }

}
