/**
 * File Configurable.java
 *
 * Copyright 2017 FAN
 */
package org.flexiblepower.service;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * DefaultValue
 *
 * @version 0.1
 * @since 24 aug. 2017
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DefaultValue {

    String value();

}
