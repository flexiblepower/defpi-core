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

    /**
     * The hostname of the orchestrator that created the process.
     */
    private final String orchestratorHost;

    /**
     * The port used to connect to the orchestrator to receive management messages.
     */
    private final int orchestratorPort;

    /**
     * The authentication token used by the process to authenticate itself by the orchestrator.
     */
    private final String orchestratorToken;

    /**
     * The process identifier of the currently running process.
     */
    private final String processId;

    /**
     * The user identifier of the user that initiated this process.
     */
    private final String userId;

    /**
     * The username of the user that initiated this process.
     */
    private final String username;

    /**
     * The email address of the user that initiated this process.
     */
    private final String userEmail;

    /**
     * Constructor of the defpi parameters object. Which sets all private fields
     * @param orchestratorHost {@link #orchestratorHost}
     * @param orchestratorPort {@link #orchestratorPort}
     * @param orchestratorToken {@link #orchestratorToken}
     * @param processId {@link #processId}
     * @param userId {@link #userId}
     * @param username {@link #username}
     * @param userEmail {@link #userEmail}
     */
    public DefPiParameters(final String orchestratorHost,
            final int orchestratorPort,
            final String orchestratorToken,
            final String processId,
            final String userId,
            final String username,
            final String userEmail) {
        this.orchestratorHost = orchestratorHost;
        this.orchestratorPort = orchestratorPort;
        this.orchestratorToken = orchestratorToken;
        this.processId = processId;
        this.userId = userId;
        this.username = username;
        this.userEmail = userEmail;
    }

    /**
     * @return the orchestratorHost {@link #orchestratorHost}
     */
    public String getOrchestratorHost() {
        return this.orchestratorHost;
    }

    /**
     * @return the orchestratorPort {@link #orchestratorPort}
     */
    public int getOrchestratorPort() {
        return this.orchestratorPort;
    }

    /**
     * @return the orchestratorToken {@link #orchestratorToken}
     */
    public String getOrchestratorToken() {
        return this.orchestratorToken;
    }

    /**
     * @return the processId {@link #processId}
     */
    public String getProcessId() {
        return this.processId;
    }

    /**
     * @return the userId {@link #userId}
     */
    public String getUserId() {
        return this.userId;
    }

    /**
     * @return the username {@link #username}
     */
    public String getUsername() {
        return this.username;
    }

    /**
     * @return the userEmail {@link #userEmail}
     */
    public String getUserEmail() {
        return this.userEmail;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "DefPiParameters [orchestratorHost=" + this.orchestratorHost + ", orchestratorPort="
                + this.orchestratorPort + ", orchestratorToken=" + this.orchestratorToken + ", processId="
                + this.processId + ", userId=" + this.userId + ", username=" + this.username + ", userEmail="
                + this.userEmail + "]";
    }

}
