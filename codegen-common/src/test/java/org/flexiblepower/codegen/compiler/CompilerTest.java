/**
 * File CompilerTest.java
 *
 * Copyright 2018 FAN
 */
package org.flexiblepower.codegen.compiler;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CompilerTest
 *
 * @version 0.1
 * @since 21 jun. 2018
 */
@SuppressWarnings({"static-method", "javadoc"})
public class CompilerTest {

    private final static Logger log = LoggerFactory.getLogger(CompilerTest.class);

    @Test
    public void testGetArchitectures() {
        CompilerTest.log.info("Detected  OS name: {}", ProtoCompiler.getOsName());
        CompilerTest.log.info("Detected architecture: {}", ProtoCompiler.getArchitecture());
    }
}
