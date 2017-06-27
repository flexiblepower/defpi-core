/**
 * File PluginTest.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.plugin.servicegen;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.flexiblepower.plugin.servicegen.model.InterfaceDescription;
import org.flexiblepower.plugin.servicegen.model.InterfaceVersionDescription;
import org.flexiblepower.plugin.servicegen.model.ServiceDescription;
import org.junit.Test;

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
 * PluginTest
 *
 * @author coenvl
 * @version 0.1
 * @since Jun 8, 2017
 */
public class PluginTest {

    private static final ObjectMapper mapper = new ObjectMapper();
    private final File inputFile;

    public PluginTest() {
        this.inputFile = new File("src/test/resources/service.json");
    }

    @Test
    public void testGenerate() throws JsonParseException, JsonMappingException, IOException {
        final ServiceDescription descr = PluginTest.mapper.readValue(this.inputFile, ServiceDescription.class);

        final Map<String, String> hashes = new HashMap<>();
        hashes.put("EchoInterfacev001", "1");
        hashes.put("DropbackInterfacev001", "2");
        hashes.put("DropbackInterfacev002", "3");
        final Templates t = new Templates("target.package", descr, hashes);
        for (final InterfaceDescription itf : descr.getInterfaces()) {
            for (final InterfaceVersionDescription version : itf.getInterfaceVersions()) {
                System.out.println(t.generateFactory(itf, version));
            }
        }
        System.out.println(t.generateDockerfile("x86", descr));
    }

    @Test
    public void testSchemaValidation() throws ProcessingException, IOException {
        final URL schemaURL = this.getClass().getClassLoader().getResource("schema.json");

        final JsonNode schemaNode = JsonLoader.fromURL(schemaURL);
        final JsonNode data = JsonLoader.fromFile(this.inputFile);

        final JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
        final JsonSchema schema = factory.getJsonSchema(schemaNode);
        final ProcessingReport report = schema.validate(data);
        System.out.println(report);

        final SyntaxValidator syntaxValidator = factory.getSyntaxValidator();
        System.out.println(syntaxValidator.schemaIsValid(schemaNode));
    }

    @Test
    public void testComputeHashes() throws JsonParseException, JsonMappingException, IOException {
        final File hashTestFile = new File("src/test/resources/hashes.json");
        final ServiceDescription descr = PluginTest.mapper.readValue(hashTestFile, ServiceDescription.class);

        final Map<String, String> protoHash = new HashMap<>();
        protoHash.put("EchoInterfacev001", "123");
        protoHash.put("EchoInterfacev002", "123");

        final Templates t = new Templates("test.package", descr, protoHash);
        for (final InterfaceDescription i : descr.getInterfaces()) {
            for (final InterfaceVersionDescription v : i.getInterfaceVersions()) {
                System.out.println(t.getHash(i, v, new HashSet<>(Arrays.asList("Stuff"))));
                System.out.println(t.getHash(i, v, Collections.emptySet()));
            }
        }
    }

}
