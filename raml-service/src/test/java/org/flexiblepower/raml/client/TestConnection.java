/*-
 * #%L
 * dEF-Pi API
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
package org.flexiblepower.raml.client;

import java.io.IOException;

import org.flexiblepower.proto.ConnectionProto.ConnectionState;
import org.flexiblepower.service.Connection;

/**
 * TestConnection
 *
 * @version 0.1
 * @since Aug 26, 2019
 */
public class TestConnection implements Connection {

    private Object lastMessage;

    public Object peek() {
        return this.lastMessage;
    }

    public Object pop() {
        final Object ret = this.lastMessage;
        this.lastMessage = null;
        return ret;
    }

    public boolean contains(final Class<?> clazz) {
        return (this.lastMessage != null) && clazz.isAssignableFrom(this.lastMessage.getClass());
    }

    @Override
    public void send(final Object message) throws IOException {
        this.lastMessage = message;
    }

    @Override
    public boolean isConnected() {
        return true;
    }

    @Override
    public ConnectionState getState() {
        return ConnectionState.CONNECTED;
    }

    @Override
    public String remoteProcessId() {
        return null;
    }

    @Override
    public String remoteServiceId() {
        return null;
    }

    @Override
    public String remoteInterfaceId() {
        return null;
    }

}
