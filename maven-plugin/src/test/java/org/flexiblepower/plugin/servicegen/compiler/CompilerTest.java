/**
 * File CompilerTest.java
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

import java.io.IOException;
import java.nio.file.Paths;

import org.junit.Test;

/**
 * CompilerTest
 *
 * @version 0.1
 * @since Jun 27, 2017
 */
@SuppressWarnings({"static-method", "javadoc"})
public class CompilerTest {

    @Test
    public void testXjcCompiler() throws IOException {
        final XjcCompiler compiler = new XjcCompiler();
        compiler.setBasePackageName("org.flexiblepower.test.xml");
        compiler.compile(Paths.get("src/test/resources/books.xsd"), Paths.get("target/xjc-test-results"));
    }

    @Test
    public void testProtoCompiler() throws IOException {
        final JavaProtoCompiler compiler = new JavaProtoCompiler("3.3.0");
        compiler.compile(Paths.get("src/test/resources/echoProtocol.proto"), Paths.get("target/protoc-test-results"));
    }

}
