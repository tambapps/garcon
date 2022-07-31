package com.tambapps.http.garcon;

import com.tambapps.http.garcon.util.IoUtils;
import lombok.Getter;
import lombok.Setter;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

abstract class AbstractGarcon {

  private ExecutorService executorService;
  private final AtomicBoolean running = new AtomicBoolean(false);
  private final Queue<Closeable> connections = new ConcurrentLinkedQueue<>();
  private ExecutorService requestsExecutorService;

  @Getter
  @Setter
  private int nbThreads = 4;

  abstract Runnable newExchangeHandler(Socket socket, AbstractGarcon garcon, Collection<Closeable> connections);

  abstract void onServerSocketClosed(SocketException e);
  abstract void onServerException(IOException e);

  public void start() {
    if (running.get()) {
      // already running
      return;
    }
    running.set(true);
    requestsExecutorService = Executors.newFixedThreadPool(nbThreads);
    // TODO make these parameters
    try (ServerSocket serverSocket = new ServerSocket(8081, 2, InetAddress.getByName("localhost"))) {
      connections.add(serverSocket);
      while (running.get()) {
        Socket socket = serverSocket.accept();
        connections.add(socket);
        requestsExecutorService.submit(newExchangeHandler(socket, this, connections));
      }
    } catch (SocketException e) {
      // the socket was probably closed, do nothing
      onServerSocketClosed(e);
    } catch (IOException e) {
      onServerException(e);
    }
    running.set(false);
  }

  public boolean isRunning() {
    return running.get();
  }

  public void startAsync() {
    if (running.get()) {
      // already running
      return;
    }
    if (executorService == null) {
      executorService = Executors.newSingleThreadExecutor();
    }
    executorService.submit(() -> {
      try {
        start();
      } catch (Exception e) {
        // shouldn't happen... but well...
        e.printStackTrace();
        running.set(false);
      }
    });
  }

  public void stop() {
    running.set(false);
    if (requestsExecutorService != null) {
      requestsExecutorService.shutdown();
    }
    requestsExecutorService = null;

    if (executorService != null) {
      executorService.shutdown();
    }
    executorService = null;
    connections.forEach(IoUtils::closeQuietly);
    connections.clear();
  }
}
