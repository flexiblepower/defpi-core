package org.flexiblepower.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Maarten Kollenstart
 *
 *         Abstract connection factory, implementations of this class must be able to
 *         create subscribehandlers and publishhandlers.
 */
@Getter
@Setter
@AllArgsConstructor
public abstract class ConnectionFactory {

    /**
     * The service that created this instance of the ConnectionFactory
     */
    private final Service service;

    public abstract SubscribeHandler<?> newSubscribeHandler(ServiceSession session);

    public abstract PublishHandler<?> newPublishHandler(ServiceSession session);

    public abstract void deleteSubscribeHandler(SubscribeHandler<?> subscribeHandler);

    public abstract void deletePublishHandler(PublishHandler<?> publishHandler);

}
