/**
 * File ConnectionHandler.java
 *
 * Copyright 2017 FAN
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flexiblepower.service;

/**
 * The ConnectionHandler provides the functionality to deal with changing connection statuses.
 *
 * Specific functions that deal with incoming messages will be called when the messages are received through the
 * connection. Reflection is used to determine any added functions to this interface, and will be called when object are
 * received, if the type of incoming message matches the interface method parameter type.
 *
 * @version 0.1
 * @since Apr 24, 2017
 */
public interface ConnectionHandler {

    /**
     * Marks the connection to be suspended. The connection will be reinstated when at least one of the processes
     * belonging to the connection is moved to a new node. After suspending {@link #resumeAfterSuspend()} will be called
     * to reinstate the connection between the processes.
     * @see #resumeAfterSuspend()
     */
    void onSuspend();

    /**
     * Called when a connection between processes is reinstated after a suspend action.
     * @see #onSuspend()
     */
    void resumeAfterSuspend();

    /**
     * Marks the connection to be interrupted. The connection is supposed to resume when the origin of the failure is
     * handled. After interrupting {@link #resumeAfterInterrupt()} is called when the processes should resume
     * communication.
     * @see #resumeAfterInterrupt()
     */
    void onInterrupt();

    /**
     * Called when the connection is interrupted, but the origin of the interruption is handled.
     * @see #onInterrupt()
     */
    void resumeAfterInterrupt();

    /**
     * Marks the connection to be terminated. This means the connection will be destroyed and not be reinstated in the
     * future.
     */
    void terminated();

}