package com.tambapps.http.garcon;

import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;

public class JavaHttpExchangeHandler extends AbstractHttpExchangeHandler {

  private final Function<HttpRequest, HttpResponse> exchange;
  private final Consumer<IOException> onConnectionClosed;
  private final Consumer<IOException> onConnectionError;
  private final Consumer<Exception> onConnectionUnexpectedError;

  public JavaHttpExchangeHandler(Socket socket, AbstractGarcon garcon,
      Collection<Closeable> connections, Function<HttpRequest, HttpResponse> exchange,
      Consumer<IOException> onConnectionClosed, Consumer<IOException> onConnectionError,
      Consumer<Exception> onConnectionUnexpectedError) {
    super(socket, garcon, connections);
    this.exchange = exchange;
    this.onConnectionClosed = onConnectionClosed;
    this.onConnectionError = onConnectionError;
    this.onConnectionUnexpectedError = onConnectionUnexpectedError;
  }


  @Override
  void onConnectionClosed(IOException e) {
    if (onConnectionClosed != null) {
      onConnectionClosed.accept(e);
    }
  }

  @Override
  void onConnectionError(IOException e) {
    if (onConnectionError != null) {
      onConnectionError.accept(e);
    }
  }

  @Override
  void onUnexpectedError(Exception e) {
    if (onConnectionUnexpectedError != null) {
      onConnectionUnexpectedError.accept(e);
    }
  }

  @Override
  protected HttpResponse processExchange(HttpRequest request) {
    return exchange.apply(request);
  }
}
