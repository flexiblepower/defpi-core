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
package org.flexiblepower.plugin.servicegen;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.flexiblepower.codegen.PluginUtils;
import org.flexiblepower.codegen.model.InterfaceDescription;
import org.flexiblepower.codegen.model.InterfaceVersionDescription;
import org.flexiblepower.codegen.model.ServiceDescription;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.github.fge.jsonschema.processors.syntax.SyntaxValidator;

/**
 * CodegenTest
 *
 * @version 0.1
 * @since Oct 4, 2017
 */
@SuppressWarnings({"static-method", "javadoc"})
public class CodegenTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testSchemaValidation() throws ProcessingException, IOException {
        final File inputFile = new File("src/test/resources/service.json");
        final URL schemaURL = PluginUtils.class.getClassLoader().getResource("schema.json");

        final JsonNode schemaNode = JsonLoader.fromURL(schemaURL);
        final JsonNode data = JsonLoader.fromFile(inputFile);

        final JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
        final JsonSchema schema = factory.getJsonSchema(schemaNode);
        final ProcessingReport report = schema.validate(data);
        Assertions.assertTrue(report.isSuccess());

        final SyntaxValidator syntaxValidator = factory.getSyntaxValidator();
        Assertions.assertTrue(syntaxValidator.schemaIsValid(schemaNode));
    }

    @Test
    public void testReadServiceValidation() throws IOException {
        final ServiceDescription descr = PluginUtils.readServiceDefinition(new File("src/test/resources/service.json"));
        for (final InterfaceDescription i : descr.getInterfaces()) {
            for (final InterfaceVersionDescription v : i.getInterfaceVersions()) {
                Assertions.assertNotNull(v.getType());
            }
        }
    }

    @Test
    public void testComputeHashes() throws JsonParseException, JsonMappingException, IOException {
        final File hashTestFile = new File("src/test/resources/hashes.json");
        final ServiceDescription descr = this.mapper.readValue(hashTestFile, ServiceDescription.class);

        for (final InterfaceDescription i : descr.getInterfaces()) {
            for (final InterfaceVersionDescription v : i.getInterfaceVersions()) {
                v.setHash("");
                Assertions.assertEquals("c6ea70559295ffc6aba33ea620642d86199bc36521311215a01f19d8dc246721",
                        PluginUtils.getSendHash(v));
                Assertions.assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                        PluginUtils.getReceiveHash(v));
            }
        }
    }

    @Test
    public void testDownload() throws IOException {
        final Path dst = Files.createTempFile("http", "proto");
        PluginUtils.downloadFile(new URL("https://raw.githubusercontent.com/defpi/interfaces/17.11/defpi/HTTP.proto"),
                dst.toFile());
        Assertions.assertEquals("e00da70ae21e257e79e23df20461e28edb5c6e4c16f6675b8dc4c40e574ebc06",
                PluginUtils.SHA256(dst));

        String baseHash = "1";
        final Set<String> messageSet = new HashSet<>(Arrays.asList("Something", "Another thing", "Cool stuff"));
        for (final String key : messageSet) {
            baseHash += ";" + key;
        }
        System.out.println(baseHash);

        System.out.println(String.join(";", messageSet));
    }

    @Test
    public void capsTest() {
        Assertions.assertEquals("this_is_a_test", PluginUtils.snakeCaps("This is a Test"));
        Assertions.assertEquals("this_is_a_test", PluginUtils.snakeCaps("Th%iS I=s a &Test.."));
        Assertions.assertEquals("ThisIsATest", PluginUtils.camelCaps("This is a Test"));
        Assertions.assertEquals("ThisIsATest", PluginUtils.camelCaps("++thIs i*S A- teST++"));
    }
}
