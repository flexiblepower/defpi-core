package org.flexiblepower.service;

import org.flexiblepower.service.exceptions.ServiceInvocationException;
import org.flexiblepower.service.serializers.MessageSerializer;

/**
 * Abstract class of a messages handler that will receive messages of type T
 *
 * @author Maarten Kollenstart, coenvl
 *
 * @param <T>
 *            Type of messages the class will be able to deserialize
 */
public abstract class AbstractMessageHandler<T> implements MessageHandlerWrapper {

    /**
     * Session the instance of the subscribe handler is connected to.
     */
    // private Connection connection;
    private final String handlesHash;
    private final MessageSerializer<T> serializer;
    protected MessageHandler protocolHandler;

    /**
     * @param string
     * @param protobufMessageSerializer
     */
    public AbstractMessageHandler(final String hash, final MessageSerializer<T> serializer) {
        this.serializer = serializer;
        this.handlesHash = hash;
    }

    @Override
    public String getHandlesHash() {
        return this.handlesHash;
    }

    @Override
    public byte[] handleMessage(final byte[] buff) throws ServiceInvocationException {
        final T input = this.serializer.deserialize(buff);
        final T output = this.handleMessage(input);
        return this.serializer.serialize(output);
    }

    /*
     * protected void send(final T msg) throws ServiceInvocationException {
     * if (this.connection == null) {
     * throw new ServiceInvocationException("Cannot send without connection");
     * }
     *
     * this.connection.send(this.serializer.serialize(msg));
     * }
     */

    @Override
    public void onConnected(final Connection connection) {
        this.protocolHandler.onConnected(connection);
    }

    @Override
    public void onSuspend() {
        this.protocolHandler.onSuspend();
    }

    @Override
    public void resumeAfterSuspend() {
        this.protocolHandler.resumeAfterSuspend();
    }

    @Override
    public void onInterrupt() {
        this.protocolHandler.onInterrupt();
    }

    @Override
    public void resumeAfterInterrupt() {
        this.protocolHandler.resumeAfterInterrupt();
    }

    @Override
    public void terminated() {
        this.protocolHandler.terminated();
    }

}
