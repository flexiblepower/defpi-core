package org.flexiblepower.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Maarten Kollenstart
 *
 *         Standard container class for a set of hashes and a factory
 */
@Getter
@Setter
@AllArgsConstructor
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
}
