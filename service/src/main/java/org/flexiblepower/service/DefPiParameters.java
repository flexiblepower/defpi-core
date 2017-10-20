/**
 * File DefPiParameters.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.service;

/**
 * DefPiParameters
 *
 * @author wilco
 * @version 0.1
 * @since Oct 20, 2017
 */
public class DefPiParameters {

    private final String orchestratorHost;
    private final String orchestratorToken;
    private final String processId;
    private final String userId;
    private final String username;
    private final String userEmail;

    public DefPiParameters(final String orchestratorHost,
            final String orchestratorToken,
            final String processId,
            final String userId,
            final String username,
            final String userEmail) {
        this.orchestratorHost = orchestratorHost;
        this.orchestratorToken = orchestratorToken;
        this.processId = processId;
        this.userId = userId;
        this.username = username;
        this.userEmail = userEmail;
    }

    /**
     * @return the orchestratorHost
     */
    public String getOrchestratorHost() {
        return this.orchestratorHost;
    }

    /**
     * @return the orchestratorToken
     */
    public String getOrchestratorToken() {
        return this.orchestratorToken;
    }

    /**
     * @return the processId
     */
    public String getProcessId() {
        return this.processId;
    }

    /**
     * @return the userId
     */
    public String getUserId() {
        return this.userId;
    }

    /**
     * @return the username
     */
    public String getUsername() {
        return this.username;
    }

    /**
     * @return the userEmail
     */
    public String getUserEmail() {
        return this.userEmail;
    }

}
