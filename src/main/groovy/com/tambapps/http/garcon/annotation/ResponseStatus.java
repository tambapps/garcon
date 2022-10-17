package com.tambapps.http.garcon.annotation;

import com.tambapps.http.garcon.HttpStatus;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ResponseStatus {

  HttpStatus value() default HttpStatus.OK;

}
