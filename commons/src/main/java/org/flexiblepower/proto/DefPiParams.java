/*-
 * #%L
 * dEF-Pi commons Library
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
package org.flexiblepower.proto;

/**
 * DefPiParams
 */
public enum DefPiParams {

    /**
     * The hostname or IP-address of the orchestrator
     */
    ORCHESTRATOR_HOST,
    /**
     * The port where the orchestrator REST API is available
     */
    ORCHESTRATOR_PORT,
    /**
     * The user authentication token for the orchestrator API
     */
    ORCHESTRATOR_TOKEN,
    /**
     * The current Process ObjectId
     */
    PROCESS_ID,
    /**
     * The current User ObjectId
     */
    USER_ID,
    /**
     * The current User name
     */
    USER_NAME,
    /**
     * The current User e-mail
     */
    USER_EMAIL;

}
