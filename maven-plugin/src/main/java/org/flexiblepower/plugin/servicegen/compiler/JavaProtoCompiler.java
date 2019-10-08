/*-
 * #%L
 * dEF-Pi service creation maven plugin
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
package org.flexiblepower.plugin.servicegen.compiler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

import org.flexiblepower.codegen.compiler.ProtoCompiler;

/**
 * ProtoCompiler
 *
 * @version 0.1
 * @since Jun 27, 2017
 */
public class JavaProtoCompiler extends ProtoCompiler {

    /**
     * @param protobufVersion The protobuf version to get the compiler for
     */
    public JavaProtoCompiler(final String protobufVersion) {
        super(protobufVersion);
    }

    /**
     * Compile the proto file which is at the specified location to java code at a certain destination.
     *
     * @param filePath The input .proto file to compile to java code
     * @param targetPath The target path where the java code should be put
     * @throws IOException When an exception occurs while reading the proto file, or writing the java code
     */
    @Override
    public void compile(final Path filePath, final Path targetPath) throws IOException {
        // Delay making the target folder to this point so it won't be made unnecessarily
        if (!targetPath.toFile().exists()) {
            Files.createDirectories(targetPath);
        }

        // Build and execute the command
        final String formatString = ProtoCompiler.getOsName().equals("windows")
                ? "\"%s\" --java_out=\"%s\" --proto_path=\"%s\" \"%s\""
                : "%s --java_out=%s --proto_path=%s %s";

        final String cmd = String.format(formatString,
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
