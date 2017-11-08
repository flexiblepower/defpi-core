/**
 * File UnidentifiedNode.java
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
package org.flexiblepower.model;

import org.mongodb.morphia.annotations.Entity;

import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * UnidentifiedNode
 *
 * @version 0.1
 * @since 20 mrt. 2017
 */
@Entity
@ToString
@NoArgsConstructor
public class UnidentifiedNode extends Node {

    public UnidentifiedNode(final String dockerId, final String hostname, final Architecture architecture) {
        super(dockerId, hostname, architecture);
    }

}
