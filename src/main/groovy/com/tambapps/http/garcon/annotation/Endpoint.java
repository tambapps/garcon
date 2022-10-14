package com.tambapps.http.garcon.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Endpoint {

  String accept() default "";

  String contentType() default "";

  String path() default "";

  String method();

}
