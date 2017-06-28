/**
 * File ProtoCompiler.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.plugin.servicegen;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import lombok.Getter;

/**
 * ProtoCompiler
 *
 * @author coenvl
 * @version 0.1
 * @since Jun 27, 2017
 */
public class ProtoCompiler {

    public enum Status {
        SUCCESS,
        FAILURE
    }

    protected final File compilerFile;

    @Getter
    protected Status state = Status.SUCCESS;

    public ProtoCompiler(final String protobufVersion) throws IOException {
        final String protoFilename = String.format("protoc-%s-%s-%s.exe",
                protobufVersion,
                ProtoCompiler.getOsName(),
                ProtoCompiler.getArchitecture());
        this.compilerFile = new File(protoFilename);

        if (!this.compilerFile.exists()) {
            ProtoCompiler.downloadFile("http://central.maven.org/maven2/com/google/protobuf/protoc/" + protobufVersion
                    + "/" + protoFilename, this.compilerFile);
        }

        this.compilerFile.setExecutable(true);
    }

    public static String getOsName() {
        return System.getProperty("os.name").toLowerCase();
    }

    public static String getArchitecture() {
        return System.getProperty("os.arch").equals("amd64") ? "x86_64" : "x86_32";
    }

    /**
     * @param string
     * @param targetFile
     */
    private static void downloadFile(final String string, final File targetFile) throws IOException {
        final URL url = new URL(string);

        try (
                final InputStream in = new BufferedInputStream(url.openStream());
                final FileOutputStream out = new FileOutputStream(targetFile)) {
            final byte[] buf = new byte[1024];
            int n = 0;
            while (-1 != (n = in.read(buf))) {
                out.write(buf, 0, n);
            }
        }

    }

    /**
     * @param sourcePath
     * @throws IOException
     */
    public void compileSources(final Path sourcePath, final Path targetPath) throws IOException {
        Files.walkFileTree(sourcePath, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(final Path filePath, final BasicFileAttributes attrs) throws IOException {
                // Delay making the target folder to this point so it won't be made unnessecarily
                if (!targetPath.toFile().exists()) {
                    Files.createDirectory(targetPath);
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
                        try (InputStream s = p.getErrorStream()) {
                            final byte[] buf = new byte[1024];
                            while (s.read(buf) > 0) {
                                System.err.println(new String(buf));
                            }
                        }
                        ProtoCompiler.this.state = Status.FAILURE;
                        return FileVisitResult.TERMINATE;
                    } else {
                        return FileVisitResult.CONTINUE;
                    }
                } catch (final InterruptedException e) {
                    e.printStackTrace();
                    ProtoCompiler.this.state = Status.FAILURE;
                    return FileVisitResult.TERMINATE;
                }
            }
        });
    }

}
