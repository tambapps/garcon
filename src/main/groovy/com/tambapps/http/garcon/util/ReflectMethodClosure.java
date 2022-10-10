package com.tambapps.http.garcon.util;

import com.tambapps.http.garcon.HttpExchangeContext;
import com.tambapps.http.garcon.HttpRequest;
import com.tambapps.http.garcon.HttpResponse;
import com.tambapps.http.garcon.annotation.ParsedRequestBody;
import com.tambapps.http.garcon.exception.BadRequestException;
import groovy.lang.Closure;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.codehaus.groovy.runtime.typehandling.GroovyCastException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

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
    Parameter[] parameters = method.getParameters();
    ArgFunction[] argSuppliers = new ArgFunction[parameters.length];
    for (int i = 0; i < method.getParameterCount(); i++) {
      Parameter parameter = parameters[i];
      Class<?> type = parameter.getType();
      if (type == HttpExchangeContext.class) {
        argSuppliers[i] = (context -> context);
      } else if (type == HttpRequest.class) {
        argSuppliers[i] = HttpExchangeContext::getRequest;
      } else if (type == HttpResponse.class) {
        argSuppliers[i] = HttpExchangeContext::getResponse;
      } else  if (parameter.getAnnotation(ParsedRequestBody.class) != null) {
        final Class<?> requestBodyClazz = type;
        argSuppliers[i] = (context) -> {
          Object parsedRequestBody = context.getParsedRequestBody();
          if (parsedRequestBody == null && !requestBodyClazz.isPrimitive()) {
            return null;
          } else if (requestBodyClazz.isInstance(parsedRequestBody)) {
            return parsedRequestBody;
          } else {
            try {
              return DefaultGroovyMethods.asType(parsedRequestBody, requestBodyClazz);
            } catch (GroovyCastException ignored) {
              // exception will be thrown later
            }
          }
          throw new BadRequestException("Request body is of unexpected type");
        };
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
