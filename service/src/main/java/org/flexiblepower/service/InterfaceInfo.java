/**
 * File InterfaceInfo.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.service;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.flexiblepower.service.serializers.MessageSerializer;

/**
 * InterfaceInfo
 *
 * @author coenvl
 * @version 0.1
 * @since May 18, 2017
 */
@Inherited
@Target({ElementType.TYPE})
@Retention(value = RetentionPolicy.RUNTIME)
public @interface InterfaceInfo {

    public String name();

    public String version();

    public String receivesHash();

    public String sendsHash();

    public Class<?>[] receiveTypes();

    public Class<?>[] sendTypes();

    public Class<? extends MessageSerializer<?>> serializer();

}
