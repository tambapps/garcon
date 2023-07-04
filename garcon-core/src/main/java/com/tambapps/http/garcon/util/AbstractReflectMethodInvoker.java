package com.tambapps.http.garcon.util;

import com.tambapps.http.garcon.HttpExchangeContext;
import com.tambapps.http.garcon.HttpRequest;
import com.tambapps.http.garcon.HttpResponse;
import com.tambapps.http.garcon.HttpStatus;
import com.tambapps.http.garcon.annotation.ParsedRequestBody;
import com.tambapps.http.garcon.annotation.PathVariable;
import com.tambapps.http.garcon.annotation.QueryParam;
import com.tambapps.http.garcon.annotation.RequestHeader;
import com.tambapps.http.garcon.exception.BadRequestException;
import lombok.SneakyThrows;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

abstract class AbstractReflectMethodInvoker {

  private interface ArgFunction {
    Object supply(HttpExchangeContext context);
  }

  private final Object owner;
  private final Method method;
  private final ArgFunction[] argFunctions;
  private final HttpStatus status;

  public AbstractReflectMethodInvoker(Object owner, Method method, HttpStatus status) {
    this.owner = owner;
    this.method = method;
    this.argFunctions = validateAndCreateArgFunctions(method);
    this.status = status;
  }

  private ArgFunction[] validateAndCreateArgFunctions(Method method) {
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
      } else if (parameter.getAnnotation(ParsedRequestBody.class) != null) {
        ParsedRequestBody annotation = parameter.getAnnotation(ParsedRequestBody.class);
        final Class<?> requestBodyClazz = type;
        argSuppliers[i] = (context) -> {
          Object parsedRequestBody = context.getParsedRequestBody();
          if (parsedRequestBody == null && annotation.required()) {
            throw new BadRequestException("Request body is required");
          }
          try {
            return smartCast(parsedRequestBody, requestBodyClazz, annotation.allowAdditionalProperties());
          } catch (IllegalArgumentException|ClassCastException ignored) {
            throw new BadRequestException("Request body is of unexpected type");
          }
        };
      } else if (parameter.getAnnotation(QueryParam.class) != null) {
        QueryParam annotation = parameter.getAnnotation(QueryParam.class);
        String queryParamName = !annotation.value().isEmpty() ? annotation.value() : annotation.name();
        final Class<?> queryParamType = type;
        argSuppliers[i] = (context) -> {
          String queryParamValue = context.getQueryParams().get(queryParamName);
          if (queryParamValue == null && !annotation.defaultValue().equals(QueryParam.NO_VALUE_STRING)) {
            queryParamValue = annotation.defaultValue();
          }
          if (queryParamValue == null && annotation.required()) {
            throw new BadRequestException(String.format("Query param %s is required", queryParamName));
          }
          try {
            return smartCast(queryParamValue, queryParamType);
          } catch (IllegalArgumentException|ClassCastException exception) {
            throw new BadRequestException(String.format("Query param %s is of unexpected type", queryParamName));
          }
        };
      } else if (parameter.getAnnotation(RequestHeader.class) != null) {
        RequestHeader annotation = parameter.getAnnotation(RequestHeader.class);
        String headerName = !annotation.value().isEmpty() ? annotation.value() : annotation.name();
        final Class<?> headerType = type;
        argSuppliers[i] = (context) -> {
          String headerValue = context.getRequest().getHeaders().get(headerName);
          if (headerValue == null && !annotation.defaultValue().equals(RequestHeader.NO_VALUE_STRING)) {
            headerValue = annotation.defaultValue();
          }
          if (headerValue == null && annotation.required()) {
            throw new BadRequestException(String.format("Header %s is required", headerName));
          }
          try {
            return smartCast(headerValue, headerType);
          } catch (IllegalArgumentException|ClassCastException exception) {
            throw new BadRequestException(String.format("Header %s is of unexpected type", headerName));
          }
        };
      } else if (parameter.getAnnotation(PathVariable.class) != null) {
        PathVariable annotation = parameter.getAnnotation(PathVariable.class);
        final String name;
        if (!annotation.value().isEmpty()) {
          name = annotation.value();
        } else if (!annotation.name().isEmpty()) {
          name = annotation.name();
        } else {
          throw new IllegalStateException("PathVariable's name is required");
        }
        final Class<?> variableType = type;
        argSuppliers[i] = (context) -> {
          if (context.getPathVariables() == null) {
            throw new RuntimeException("No path variable were configured for method endpoint " + method.getName());
          }
          String value = context.getPathVariables().get(name);
          if (value == null) {
            throw new BadRequestException(String.format("Path variable %s is required", name));
          }
          try {
            return smartCast(value, variableType);
          } catch (IllegalArgumentException|ClassCastException ignored) {
            throw new BadRequestException(String.format("path variable %s is of unexpected type", name));
          }
        };
      } else {
        throw new IllegalArgumentException(String.format("Cannot handle type %s for method %s", type.getSimpleName(), method.getName()));
      }
    }
    return argSuppliers;
  }

  @SneakyThrows
  public Object invoke(HttpExchangeContext context) {
    context.getResponse().setStatusCode(status);
    Object[] args = new Object[argFunctions.length];
    for (int i = 0; i < args.length; i++) {
      args[i] = argFunctions[i].supply(context);
    }
    try {
      return method.invoke(owner, args);
    } catch (InvocationTargetException e) {
      throw e.getCause() != null ? e.getCause() : e;
    }
  }

  protected Object smartCast(Object o, Class<?> aClass) {
    return smartCast(o, aClass, false);
  }

  abstract Object smartCast(Object o, Class<?> aClass, boolean allowAdditionalProperties);


}