/**
 * File MessageHandlerWrapper.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.service;

import org.flexiblepower.service.exceptions.ServiceInvocationException;

/**
 * MessageHandlerWrapper
 *
 * @author coenvl
 * @version 0.1
 * @since May 12, 2017
 */
public interface MessageHandlerWrapper extends MessageHandler {

    public String getHandlesHash();

    public byte[] handleMessage(byte[] buffer) throws ServiceInvocationException;

    public abstract <T> T handleMessage(Object message) throws ServiceInvocationException;

}
