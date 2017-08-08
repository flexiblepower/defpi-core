/**
 * File CompilerTest.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.plugin.servicegen;

import java.io.IOException;
import java.nio.file.Paths;

import org.flexiblepower.plugin.servicegen.compiler.ProtoCompiler;
import org.flexiblepower.plugin.servicegen.compiler.XjcCompiler;
import org.junit.Test;

import lombok.extern.slf4j.Slf4j;

/**
 * CompilerTest
 *
 * @author coenvl
 * @version 0.1
 * @since Jun 27, 2017
 */
@Slf4j
@SuppressWarnings("static-method")
public class CompilerTest {

    @Test
    public void testGetArchitectures() {
        log.info("Detected  OS name: {}", ProtoCompiler.getOsName());
        log.info("Detected architecture: {}", ProtoCompiler.getArchitecture());
    }

    @Test
    public void testXjcCompiler() throws IOException {
        final XjcCompiler compiler = new XjcCompiler();
        compiler.setBasePackageName("org.flexiblepower.test.xml");
        compiler.compile(Paths.get("src/test/resources/books.xsd"), Paths.get("target/xjc-test-results"));
    }

    @Test
    public void testProtoCompiler() throws IOException {
        final ProtoCompiler compiler = new ProtoCompiler("3.3.0");
        compiler.compile(Paths.get("src/test/resources/echoProtocol.proto"), Paths.get("target/protoc-test-results"));
    }

}
