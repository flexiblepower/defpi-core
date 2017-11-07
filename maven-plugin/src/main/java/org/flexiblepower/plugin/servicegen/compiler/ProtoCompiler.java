/**
 * File ProtoCompiler.java
 *
 * Copyright 2017 FAN
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

    protected final File compilerFile;
    private final String protobufVersion;

    public ProtoCompiler(final String protobufVersion) throws IOException {
        final String protoFilename = String.format("protoc-%s-%s-%s.exe",
                protobufVersion,
                ProtoCompiler.getOsName(),
                ProtoCompiler.getArchitecture());
        this.protobufVersion = protobufVersion;

        final Path tempPath = Paths.get("target", "protoc");
        this.compilerFile = tempPath.resolve(protoFilename).toFile();
    }

    public static String getOsName() {
        final String rawName = System.getProperty("os.name").toLowerCase();
        if (rawName.startsWith("windows")) {
            return "windows";
        } else {
            return rawName;
        }
    }

    public static String getArchitecture() {
        return System.getProperty("os.arch").equals("amd64") ? "x86_64" : "x86_32";
    }

    /**
     * @param sourcePath
     * @throws IOException
     */
    @Override
    public void compile(final Path filePath, final Path targetPath) throws IOException {
        // Delay making the target folder to this point so it won't be made unnessecarily
        if (!targetPath.toFile().exists()) {
            Files.createDirectories(targetPath);
        }

        if (!this.compilerFile.exists()) {
            Files.createDirectories(this.compilerFile.toPath().getParent());
            PluginUtils.downloadFile(
                    "http://central.maven.org/maven2/com/google/protobuf/protoc/" + this.protobufVersion + "/"
                            + this.compilerFile.getName().toString(),
                    this.compilerFile);
            this.compilerFile.setExecutable(true);
        }

        // Build and execute the command
        final String cmd = String.format("%s --java_out=%s --proto_path=%s %s",
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
