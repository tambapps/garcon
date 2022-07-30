package com.tambapps.http.garcon;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

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

  @SneakyThrows
  void start() {
    running.set(true);
    requestsExecutorService = Executors.newFixedThreadPool(nbThreads);
    try {
      ServerSocket serverSocket = new ServerSocket(8081, 2, InetAddress.getByName("localhost"));
      connections.add(serverSocket);
      while (running.get()) {
        Socket socket = serverSocket.accept();
        connections.add(socket);
        requestsExecutorService.submit(newExchangeHandler(socket, this, connections));
      }
    } catch (SocketException e) {
      // the socket was probably closed, do nothing
    } catch (IOException e) {
      e.printStackTrace();
    }
    running.set(false);
  }

  boolean isRunning() {
    return running.get();
  }

  void startAsync() {
    if (executorService == null) {
      executorService = Executors.newSingleThreadExecutor();
    }
    executorService.submit(() -> {
      try {
        start();
      } catch (Exception e) {
        e.printStackTrace();
        // TODO
      }
    });
  }

  void stop() {
    running.set(false);
    if (requestsExecutorService != null) {
      requestsExecutorService.shutdown();
    }
    requestsExecutorService = null;

    if (executorService != null) {
      executorService.shutdown();
    }
    executorService = null;
    connections.forEach(c -> {
      try {
        c.close();
      } catch (IOException e) {}
    });
    connections.clear();
  }
}
