/**
 * File PyXBCompiler.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.pythoncodegen.compiler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

import org.flexiblepower.codegen.compiler.Compiler;

/**
 * PyXBCompiler
 *
 * @author coenvl
 * @version 0.1
 * @since Jun 28, 2017
 */
public class PyXBCompiler implements Compiler {

    /**
     *
     */
    public PyXBCompiler() {
        final String cmd = String.format("pyxbgen --version");

        try {
            final Process p = Runtime.getRuntime().exec(cmd);

            if (p.waitFor() != 0) {
                try (final Scanner s = new Scanner(p.getErrorStream()).useDelimiter("\\A")) {
                    throw new RuntimeException("Unable to determine PyXB version, please make sure it is installed.");
                }
            }
        } catch (final InterruptedException e) {
            throw new RuntimeException("Exception while initializing PyXB compiler: " + e.getMessage(), e);
        } catch (final IOException e) {
            throw new RuntimeException("Unable to determine PyXB version, please make sure it is installed.");
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.flexiblepower.pythoncodegen.Compiler#compileSources(java.nio.file.Path, java.nio.file.Path)
     */
    @Override
    public void compile(final Path filePath, final Path targetPath) throws IOException {
        // Delay making the target folder to this point so it won't be made unnessecarily
        if (!targetPath.toFile().exists()) {
            Files.createDirectory(targetPath);
        }

        final String moduleName = filePath.getFileName().toString();

        // Build and execute the command
        final String cmd = String.format("pyxbgen --binding-root=%s -m %s -u %s",
                targetPath.toString(),
                moduleName,
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
