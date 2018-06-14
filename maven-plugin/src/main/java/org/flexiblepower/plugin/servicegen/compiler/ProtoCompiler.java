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
package org.flexiblepower.plugin.servicegen.compiler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

import org.flexiblepower.codegen.PluginUtils;
import org.flexiblepower.codegen.compiler.Compiler;

/**
 * ProtoCompiler
 *
 * @version 0.1
 * @since Jun 27, 2017
 */
public class ProtoCompiler implements Compiler {

    private static final Object downloadFileLock = new Object();
    private final File compilerFile;

    /**
     * Create the proto compiler for the specified protobuf version. This will download the compiler from the maven
     * repository, and put it in a temporary folder.
     *
     * @param protobufVersion The protobuf version to get the compiler for
     * @throws IOException When an exception occurs reading or writing the protobuf compiler to/from disk
     */
    public ProtoCompiler(final String protobufVersion) throws IOException {
        final String protoFilename = String.format("protoc-%s-%s-%s.exe",
                protobufVersion,
                ProtoCompiler.getOsName(),
                ProtoCompiler.getArchitecture());

        final String tmpfir = System.getProperty("java.io.tmpdir",
                ProtoCompiler.getOsName().equals("windows") ? "C:/Windows/Temp" : "/tmp");
        final Path tempPath = Paths.get(tmpfir);
        this.compilerFile = tempPath.resolve(protoFilename).toFile();

        ProtoCompiler.ensureCompilerExists(this.compilerFile, protobufVersion);
    }

    /**
     * @param compilerFile2
     * @throws IOException
     */
    private static void ensureCompilerExists(final File file, final String protobufVersion) throws IOException {
        synchronized (ProtoCompiler.downloadFileLock) {
            if (!file.exists()) {
                Files.createDirectories(file.toPath().getParent());
                PluginUtils.downloadFile(
                        "http://central.maven.org/maven2/com/google/protobuf/protoc/" + protobufVersion + "/"
                                + file.getName().toString(),
                        file);
                file.setExecutable(true);
            }
        }
    }

    /**
     * The name of the operating system will be used to find the correct version of the compiler on maven central.
     *
     * @return The name of the current systems operating system (e.g. windows, linux).
     */
    static String getOsName() {
        final String rawName = System.getProperty("os.name").toLowerCase();
        if (rawName.startsWith("windows")) {
            return "windows";
        } else if (rawName.startsWith("mac")) {
            return "osx";
        } else {
            return rawName;
        }
    }

    /**
     * The name of the system architecture will be used to find the correct version of the compiler on maven central.
     *
     * @return The name of the current system architecture (e.g x86_64, x64_32).
     */
    static String getArchitecture() {
        return System.getProperty("os.arch").equals("amd64") ? "x86_64" : "x86_32";
    }

    /**
     * Compile the proto file which is at the specified location to java code at a certain destination.
     *
     * @param filePath   The input .proto file to compile to java code
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
                ProtoCompiler.this.compilerFile.getAbsolutePath(),
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
