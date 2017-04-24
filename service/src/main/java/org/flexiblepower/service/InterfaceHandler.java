package org.flexiblepower.service;

/**
 * @author Maarten Kollenstart
 *
 *         Standard container class for a set of hashes and a factory
 */
public class InterfaceHandler {

    /**
     * Hash corresponding to the description of messages the handler is able to subscribe to
     */
    private final String subscribeHash;
    /**
     * Hash corresponding to the description of messages the handler is able to publish
     */
    private final String publishHash;
    /**
     * The ConnectionFactory that creates the subscribe and publish handlers for
     * the InterfaceHandler
     */
    private final ConnectionFactory connectionFactory;

    /**
     * @param subscribeHash
     * @param publishHash
     * @param connectionFactory
     */
    public InterfaceHandler(final String subscribeHash,
            final String publishHash,
            final ConnectionFactory connectionFactory) {
        super();
        this.subscribeHash = subscribeHash;
        this.publishHash = publishHash;
        this.connectionFactory = connectionFactory;
    }

    /**
     * @return the subscribeHash
     */
    public String getSubscribeHash() {
        return this.subscribeHash;
    }

    /**
     * @return the publishHash
     */
    public String getPublishHash() {
        return this.publishHash;
    }

    /**
     * @return the connectionFactory
     */
    public ConnectionFactory getConnectionFactory() {
        return this.connectionFactory;
    }
}
