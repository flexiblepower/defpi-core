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

/**
 * Architecture
 *
 * @version 0.1
 * @since May 8, 2017
 */
public enum Architecture {
    /**
     * Any other architecture
     */
    UNKNOWN,
    /**
     * x86 64 bit
     */
    X86_64,
    /**
     * ARM based architecture
     */
    ARM;

    /**
     * Get the enum type from a text representation of the architecture. This function is used for parsing responses
     *
     * @param text The textual representation to parse into an Architecture enum
     * @return The Archictecture enum that corresponds to the input
     */
    public static Architecture fromString(final String text) {
        for (final Architecture a : Architecture.values()) {
            if (text.toUpperCase().startsWith(a.name())) {
                return a;
            }
        }
        return Architecture.UNKNOWN;
    }
}
