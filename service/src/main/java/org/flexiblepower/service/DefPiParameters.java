/*-
 * #%L
 * dEF-Pi service managing library
 * %%
 * Copyright (C) 2017 - 2018 Flexible Power Alliance Network
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.flexiblepower.service;

/**
 * DefPiParameters
 *
 * @version 0.1
 * @since Oct 20, 2017
 */
public class DefPiParameters {

    private final String orchestratorHost;
    private final int orchestratorPort;
    private final String orchestratorToken;
    private final String processId;
    private final String userId;
    private final String username;
    private final String userEmail;

    /**
     * Constructor of the defpi parameters object. Which sets all private fields
     *
     * @param orchestratorHost {@link #getOrchestratorHost()}
     * @param orchestratorPort {@link #getOrchestratorPort()}
     * @param orchestratorToken {@link #getOrchestratorToken()}
     * @param processId {@link #getProcessId()}
     * @param userId {@link #getUserId()}
     * @param username {@link #getUsername()}
     * @param userEmail {@link #getUserEmail()}
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
     * @return The hostname of the orchestrator that created the process.
     */
    public String getOrchestratorHost() {
        return this.orchestratorHost;
    }

    /**
     * @return The port used to connect to the orchestrator to receive management messages.
     */
    public int getOrchestratorPort() {
        return this.orchestratorPort;
    }

    /**
     * @return The authentication token used by the process to authenticate itself by the orchestrator.
     */
    public String getOrchestratorToken() {
        return this.orchestratorToken;
    }

    /**
     * @return The process identifier of the currently running process.
     */
    public String getProcessId() {
        return this.processId;
    }

    /**
     * @return The user identifier of the user who owns this process.
     */
    public String getUserId() {
        return this.userId;
    }

    /**
     * @return The name of the user who owns this process.
     */
    public String getUsername() {
        return this.username;
    }

    /**
     * @return The email address of the user who owns this process.
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
