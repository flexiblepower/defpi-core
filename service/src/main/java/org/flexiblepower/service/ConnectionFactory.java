package org.flexiblepower.service;

/**
 * @author Maarten Kollenstart
 *
 *         Abstract connection factory, implementations of this class must be able to
 *         create subscribehandlers and publishhandlers.
 */
public abstract class ConnectionFactory {

    /**
     * @return the service
     */
    public Service getService() {
        return this.service;
    }

    /**
     * @param service
     */
    public ConnectionFactory(final Service service) {
        super();
        this.service = service;
    }

    /**
     * The service that created this instance of the ConnectionFactory
     */
    private final Service service;

    public abstract SubscribeHandler<?> newSubscribeHandler(ServiceSession session);

    public abstract PublishHandler<?> newPublishHandler(ServiceSession session);

    public abstract void deleteSubscribeHandler(SubscribeHandler<?> subscribeHandler);

    public abstract void deletePublishHandler(PublishHandler<?> publishHandler);

}
