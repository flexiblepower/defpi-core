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

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.flexiblepower.serializers.MessageSerializer;

/**
 * InterfaceInfo
 *
 * @version 0.1
 * @since May 18, 2017
 */
@Inherited
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface InterfaceInfo {

    /**
     * @return The name of the interface this type defines
     */
    public String name();

    /**
     * @return The version of this interface that this type defines
     */
    public String version();

    /**
     * @return The hash that describes the incoming messages this interface can deal with
     */
    public String receivesHash();

    /**
     * @return The hash that describes the outgoing messages this interface can send
     */
    public String sendsHash();

    /**
     * @return The array of types that this interface can deal with
     */
    public Class<?>[] receiveTypes();

    /**
     * @return The array of types that this interface may send
     */
    public Class<?>[] sendTypes();

    /**
     * @return The class that implements the serializer to convert messages from/to raw byte arrays
     */
    @SuppressWarnings("rawtypes")
    public Class<? extends MessageSerializer> serializer();

}
