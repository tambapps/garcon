package com.tambapps.http.garcon.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface QueryParam {
  public static String NO_VALUE_STRING = "\n\t\t\n\t\t\n\ue000\ue001\ue002\n\t\t\t\t\n";

  String value() default "";

  String name() default "";

  boolean required() default true;

  String defaultValue() default NO_VALUE_STRING;

}
