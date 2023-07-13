package com.tambapps.http.garcon.util;

import com.tambapps.http.garcon.HttpStatus;
import com.tambapps.http.garcon.exception.BadRequestException;
import groovy.lang.MissingPropertyException;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.codehaus.groovy.runtime.StringGroovyMethods;
import org.codehaus.groovy.runtime.typehandling.GroovyCastException;

import java.lang.reflect.Method;
import java.util.Map;

public class ReflectMethodInvoker extends AbstractReflectMethodInvoker {

  public ReflectMethodInvoker(Object owner, Method method, HttpStatus status) {
    super(owner, method, status);
  }

  @Override
  protected Object doSmartCast(Object o, Class<?> aClass, boolean allowAdditionalProperties) {
    try {
      return doGroovySmartCast(o, aClass, allowAdditionalProperties);
    } catch (GroovyCastException e) {
      throw new IllegalArgumentException(e);
    }
  }

  protected Object doGroovySmartCast(Object o, Class<?> aClass, boolean allowAdditionalProperties) {
    if (o instanceof CharSequence) {
      // for smart number conversion
      return StringGroovyMethods.asType(o.toString(), aClass);
    } else if (o instanceof Map && !Map.class.isAssignableFrom(aClass)
        && !aClass.isPrimitive()
        && !Number.class.isAssignableFrom(aClass)
        && String.class != aClass && Character.class != aClass
        && Boolean.class != aClass) {
      Object newInstance = DefaultGroovyMethods.newInstance(aClass);
      Map<?, ?> map = (Map<?, ?>) o;

      for (Map.Entry<?, ?> entry : map.entrySet()) {
        try {
          InvokerHelper.setProperty(newInstance, entry.getKey().toString(), entry.getValue());
        } catch (MissingPropertyException e) {
          if (!allowAdditionalProperties) {
            throw new BadRequestException("Unknown property " + e.getProperty());
          }
        }
      }
      return newInstance;
    } else {
      return DefaultGroovyMethods.asType(o, aClass);
    }
  }
}
