/*-
 * #%L
 * dEF-Pi service codegen-common
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

package org.flexiblepower.codegen.compiler;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CompilerTest
 *
 * @version 0.1
 * @since 21 jun. 2018
 */
@SuppressWarnings({"static-method", "javadoc"})
public class CompilerTest {

    private final static Logger log = LoggerFactory.getLogger(CompilerTest.class);

    @Test
    public void testGetArchitectures() {
        CompilerTest.log.info("Detected  OS name: {}", ProtoCompiler.getOsName());
        CompilerTest.log.info("Detected architecture: {}", ProtoCompiler.getArchitecture());
    }
}
