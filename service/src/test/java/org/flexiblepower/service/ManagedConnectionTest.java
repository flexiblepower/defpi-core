/**
 * File ManagedConnectionTest.java
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

import org.flexiblepower.proto.ConnectionProto.ConnectionState;
import org.junit.Assert;
import org.junit.Test;

/**
 * ManagedConnectionTest
 *
 * @version 0.1
 * @since May 29, 2017
 */
@SuppressWarnings("static-method")
public class ManagedConnectionTest {

    @Test(timeout = 5000)
    public void testConnection() throws Exception {
        try (
                TCPConnection conn = new TCPConnection("ConnID1234",
                        1234,
                        "tcp://localhost:5678",
                        TestService.class.getAnnotation(InterfaceInfo.class),
                        "",
                        "",
                        "")) {
            Assert.assertEquals(ConnectionState.STARTING, conn.getState());
            conn.goToTerminatedState();
            Assert.assertEquals(ConnectionState.TERMINATED, conn.getState());
        }
    }

}
