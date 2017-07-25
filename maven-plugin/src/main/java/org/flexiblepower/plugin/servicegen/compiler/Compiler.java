/**
 * File Compiler.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.plugin.servicegen.compiler;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Compiler
 *
 * @author coenvl
 * @version 0.1
 * @since Jun 28, 2017
 */
public interface Compiler {

    public void compile(Path sourceFile, Path targetPath) throws IOException;

}
