/*-
 * #%L
 * dEF-Pi python service creation
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
package org.flexiblepower.pythoncodegen;

import lombok.extern.slf4j.Slf4j;

/**
 * Main
 *
 * @version 0.1
 * @since Oct 23, 2017
 */
@Slf4j
public class Main {

    /**
     * Main method for generating python code from service.json
     *
     * @param args Command line arguments (ignored)
     */
    public static void main(final String[] args) {
        final PythonCodegen codeGenerator = new PythonCodegen();
        try {
            codeGenerator.run();
        } catch (final Exception e) {
            Main.log.error(e.getMessage());
            Main.log.trace(e.getMessage(), e);
            System.exit(1);
        }
    }

}
