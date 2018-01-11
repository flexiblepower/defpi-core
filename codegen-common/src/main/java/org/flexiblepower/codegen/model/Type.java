/**
 * File Type.java
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

package org.flexiblepower.codegen.model;

import lombok.Data;

/**
 * Type
 *
 * @version 0.1
 * @since Apr 13, 2017
 */
@Data
public class Type {

    private String descriptor;
    private String type;
    private Descriptor descriptorObject;

}
