/**
 * File Compiler.java
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
package org.flexiblepower.codegen.compiler;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Compiler
 *
 * @version 0.1
 * @since Jun 28, 2017
 */
public interface Compiler {

    public void compile(Path sourceFile, Path targetPath) throws IOException;

}
