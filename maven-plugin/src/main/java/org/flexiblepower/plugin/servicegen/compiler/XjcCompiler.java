/**
 * File XjcCompiler.java
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
package org.flexiblepower.plugin.servicegen.compiler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.flexiblepower.codegen.compiler.Compiler;

import com.sun.tools.xjc.Driver;

import lombok.Setter;

/**
 * XjcCompiler
 *
 * @version 0.1
 * @since Jun 28, 2017
 */
public class XjcCompiler implements Compiler {

    @Setter
    private String basePackageName = "";

    /*
     * (non-Javadoc)
     *
     * @see org.flexiblepower.plugin.servicegen.Compiler#compileSources(java.nio.file.Path, java.nio.file.Path)
     */
    @Override
    public void compile(final Path filePath, final Path targetPath) throws IOException {
        // Delay making the target folder to this point so it won't be made unnessecarily
        if (!targetPath.toFile().exists()) {
            Files.createDirectories(targetPath);
        }

        // Build and execute the command
        @SuppressWarnings("unused")
        final Driver d = new Driver();

        try (
                final OutputStream stateOs = new ByteArrayOutputStream();
                final PrintStream statePs = new PrintStream(stateOs);
                final OutputStream outOs = new ByteArrayOutputStream();
                final PrintStream outPs = new PrintStream(outOs)) {
            final String[] args = {"-npa", "-d", targetPath.toString(), "-p", XjcCompiler.this.basePackageName,
                    filePath.toString(), "-Xfluent-api"};
            final int result = Driver.run(args, statePs, outPs);

            if (result != 0) {
                throw new IOException(
                        "Exception while compiling " + filePath + ":\n" + stateOs.toString() + "\n" + outOs.toString());
            }
        } catch (final Exception e) {
            throw new IOException("Exception while compiling " + filePath, e);
        }
    }

}
