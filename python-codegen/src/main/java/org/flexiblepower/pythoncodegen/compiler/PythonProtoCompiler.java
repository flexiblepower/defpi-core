/**
 * File ProtoCompiler.java
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
package org.flexiblepower.pythoncodegen.compiler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

import org.flexiblepower.codegen.compiler.ProtoCompiler;
import org.flexiblepower.pythoncodegen.PythonCodegen;

/**
 * ProtoCompiler
 *
 * @version 0.1
 * @since Jun 27, 2017
 */
public class PythonProtoCompiler extends ProtoCompiler {

    /**
     * Create the proto compiler for the specified protobuf version. This will download the compiler from the maven
     * repository, and put it in a temporary folder.
     *
     * @param protobufVersion The protobuf version to get the compiler for
     */
    public PythonProtoCompiler(final String protobufVersion) throws IOException {
        super(protobufVersion);
    }

    /**
     * @param sourcePath
     * @throws IOException
     */
    @Override
    public void compile(final Path filePath, final Path targetPath) throws IOException {
        // Delay making the target folder to this point so it won't be made unnessecarily
        if (!targetPath.toFile().exists()) {
            Files.createDirectory(targetPath);
        }

        if (Files.notExists(targetPath.resolve(PythonCodegen.PACKAGE_DECLARATION))) {
            Files.createFile(targetPath.resolve(PythonCodegen.PACKAGE_DECLARATION));
        }

        // Build and execute the command
        final String cmd = String.format("%s --python_out=%s --proto_path=%s %s",
                this.getCompilerFile().getAbsolutePath(),
                targetPath.toString(),
                filePath.getParent().toString(),
                filePath.toString());
        final Process p = Runtime.getRuntime().exec(cmd);

        try {
            if (p.waitFor() != 0) {
                try (final Scanner s = new Scanner(p.getErrorStream()).useDelimiter("\\A")) {
                    final String error = s.hasNext() ? s.next() : "";
                    throw new IOException("Error while compiling " + filePath + ": " + error);
                }
            }
        } catch (final InterruptedException e) {
            throw new IOException("Interrupted while compiling " + filePath);
        }
    }

}
