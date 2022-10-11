package com.tambapps.http.garcon.util;

import com.tambapps.http.garcon.HttpExchangeContext;
import com.tambapps.http.garcon.HttpRequest;
import com.tambapps.http.garcon.HttpResponse;
import com.tambapps.http.garcon.annotation.ParsedRequestBody;
import com.tambapps.http.garcon.annotation.QueryParam;
import com.tambapps.http.garcon.exception.BadRequestException;
import groovy.lang.Closure;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.codehaus.groovy.runtime.StringGroovyMethods;
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
      } else if (parameter.getAnnotation(ParsedRequestBody.class) != null) {
        final Class<?> requestBodyClazz = type;
        argSuppliers[i] = (context) -> {
          Object parsedRequestBody = context.getParsedRequestBody();
          try {
            return smartCast(parsedRequestBody, requestBodyClazz);
          } catch (GroovyCastException ignored) {
            throw new BadRequestException("Request body is of unexpected type");
          }
        };
      } else if (parameter.getAnnotation(QueryParam.class) != null) {
        QueryParam annotation = parameter.getAnnotation(QueryParam.class);
        String queryParamName = !annotation.value().isEmpty() ? annotation.value() : annotation.name();
        final Class<?> queryParamType = type;
        argSuppliers[i] = (context) -> {
          String queryParamValue = context.getQueryParams().get(queryParamName);
          // yes equality check on string is wanted
          if (queryParamValue == null && !annotation.defaultValue().equals(QueryParam.NO_VALUE_STRING)) {
            queryParamValue = annotation.defaultValue();
          }
          if (queryParamValue == null && annotation.required()) {
            throw new BadRequestException(String.format("Query param %s is required", queryParamName));
          }
          try {
            return smartCast(queryParamValue, queryParamType);
          } catch (GroovyCastException ignored) {
            throw new BadRequestException(String.format("Query param %s is of unexpected type", queryParamName));
          }
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

  private static Object smartCast(Object o, Class<?> aClass) {
    if (o == null && !aClass.isPrimitive()) {
      return null;
    } else if (aClass.isInstance(o)) {
      return o;
    } else if (o instanceof CharSequence) {
      // for smart number conversion
      return StringGroovyMethods.asType(o.toString(), aClass);
    } else {
      return DefaultGroovyMethods.asType(o, aClass);
    }
  }
}
