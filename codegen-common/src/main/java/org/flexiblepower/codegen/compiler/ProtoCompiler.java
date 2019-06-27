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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.flexiblepower.codegen.PluginUtils;

/**
 * AbstractProtoCompiler
 *
 * @version 0.1
 * @since 21 jun. 2018
 */
public abstract class ProtoCompiler implements InterfaceCompiler {

    private static final Object downloadFileLock = new Object();
    private final File compilerFile;
    private final String protobufVersion;

    /**
     * Create the proto compiler for the specified protobuf version. This will download the compiler from the maven
     * repository, and put it in a temporary folder.
     *
     * @param protobufVersion The protobuf version to get the compiler for
     */
    public ProtoCompiler(final String protobufVersion) {
        final String protoFilename = String.format("protoc-%s-%s-%s.exe",
                protobufVersion,
                ProtoCompiler.getOsName(),
                ProtoCompiler.getArchitecture());
        this.protobufVersion = protobufVersion;

        final String tmpdir = System.getProperty("java.io.tmpdir",
                ProtoCompiler.getOsName().equals("windows") ? "C:/Windows/Temp" : "/tmp");
        final Path tempPath = Paths.get(tmpdir);

        this.compilerFile = tempPath.resolve(protoFilename).toFile();
    }

    /**
     * @throws IOException When an exception occurs reading or writing the protobuf compiler to/from disk
     */
    private void ensureCompilerExists() throws IOException {
        synchronized (ProtoCompiler.downloadFileLock) {
            if (!this.compilerFile.exists()) {
                Files.createDirectories(this.compilerFile.toPath().getParent());
                PluginUtils.downloadFile(
                        "http://central.maven.org/maven2/com/google/protobuf/protoc/" + this.protobufVersion + "/"
                                + this.compilerFile.getName(),
                        this.compilerFile);
                this.compilerFile.setExecutable(true);
            }
        }
    }

    /**
     * The name of the operating system will be used to find the correct version of the compiler on maven central.
     *
     * @return The name of the current systems operating system (e.g. windows, linux).
     */
    public static String getOsName() {
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
     * @return The file with the compiler for this protobuf version
     * @throws IOException When an exception occurs reading or writing the protobuf compiler to/from disk
     */
    protected File getCompilerFile() throws IOException {
        this.ensureCompilerExists();
        return this.compilerFile;
    }

}
