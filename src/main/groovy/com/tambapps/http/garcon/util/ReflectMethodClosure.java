package com.tambapps.http.garcon.util;

import com.tambapps.http.garcon.HttpExchangeContext;
import com.tambapps.http.garcon.HttpRequest;
import com.tambapps.http.garcon.HttpResponse;
import groovy.lang.Closure;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ReflectMethodClosure extends Closure<Object> {

  private interface ArgFunction {
    Object supply(HttpExchangeContext context);
  }

  private final Method method;
  private final ArgFunction[] argFunctions;

  public ReflectMethodClosure(Object owner, Method method) {
    super(owner);
    this.method = method;
    this.argFunctions = validateAndCreateArgFunctions(method);
  }

  private static ArgFunction[] validateAndCreateArgFunctions(Method method) {
    Class<?>[] parameterTypes = method.getParameterTypes();
    ArgFunction[] argSuppliers = new ArgFunction[parameterTypes.length];
    for (int i = 0; i < method.getParameterCount(); i++) {
      Class<?> type = parameterTypes[i];
      if (type == HttpExchangeContext.class) {
        argSuppliers[i] = (context -> context);
      } else if (type == HttpRequest.class) {
        argSuppliers[i] = HttpExchangeContext::getRequest;
      } else if (type == HttpResponse.class) {
        argSuppliers[i] = HttpExchangeContext::getResponse;
      } else {
        throw new IllegalArgumentException(String.format("Cannot handle type %s for method %s", type.getSimpleName(), method.getName()));
      }
    }
    return argSuppliers;
  }

  // for Closure
  public Object doCall(HttpExchangeContext context)
      throws InvocationTargetException, IllegalAccessException {
    Object[] args = new Object[argFunctions.length];
    for (int i = 0; i < args.length; i++) {
      args[i] = argFunctions[i].supply(context);
    }
    return method.invoke(getOwner(), args);
  }

}
