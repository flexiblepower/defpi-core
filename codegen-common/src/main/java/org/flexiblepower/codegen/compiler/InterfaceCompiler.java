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

import java.io.IOException;
import java.nio.file.Path;

/**
 * Compiler
 *
 * @version 0.1
 * @since Jun 28, 2017
 */
public interface InterfaceCompiler {

    /**
     * Compile an interface source file into code at a certain destination.
     *
     * @param sourceFile The input file to compile to code. The type of input file depends on the interface descriptor
     * @param targetPath The target path where the code should be put. The type of code depends on the target service
     *            language
     * @throws IOException When an exception occurs while reading the input file, or writing the output code
     */
    public void compile(Path sourceFile, Path targetPath) throws IOException;

}
