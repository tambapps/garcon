package com.tambapps.http.garcon;

public interface HttpExchangeHandler {
  HttpResponse processExchange(HttpRequest request);
}
