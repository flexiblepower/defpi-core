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
package org.flexiblepower.model;

import java.util.Date;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import lombok.Value;

/**
 * The pending change description refers to an existing pending change which is an update to the dEF-Pi stack, but is
 * not yet implemented.
 *
 * @version 0.1
 * @since Aug 24, 2017
 */
@Value
public class PendingChangeDescription {

    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId id;

    private String type;

    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId userId;

    private Date created;

    private String description;

    private int count;

    private String state;
}
