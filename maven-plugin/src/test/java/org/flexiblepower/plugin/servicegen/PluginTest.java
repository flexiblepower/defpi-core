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

import lombok.extern.slf4j.Slf4j;

/**
 * PluginTest
 *
 * @author coenvl
 * @version 0.1
 * @since Jun 8, 2017
 */
@Slf4j
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
        hashes.put("EchoInterface_v001", "1");
        hashes.put("DropbackInterface_v001", "2");
        hashes.put("DropbackInterface_v002", "3");
        final Templates t = new Templates("target.package", "", "", descr, hashes);
        for (final InterfaceDescription itf : descr.getInterfaces()) {
            for (final InterfaceVersionDescription version : itf.getInterfaceVersions()) {
                PluginTest.log.info(t.generateFactory(itf, version));
            }
        }
        PluginTest.log.info(t.generateDockerfile("x86", descr));
    }

    @Test
    public void testSchemaValidation() throws ProcessingException, IOException {
        final URL schemaURL = this.getClass().getClassLoader().getResource("schema.json");

        final JsonNode schemaNode = JsonLoader.fromURL(schemaURL);
        final JsonNode data = JsonLoader.fromFile(this.inputFile);

        final JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
        final JsonSchema schema = factory.getJsonSchema(schemaNode);
        final ProcessingReport report = schema.validate(data);
        PluginTest.log.info("report: {}", report);

        final SyntaxValidator syntaxValidator = factory.getSyntaxValidator();
        PluginTest.log.info("syntax: {}", syntaxValidator.schemaIsValid(schemaNode));
    }

    @Test
    public void testComputeHashes() throws JsonParseException, JsonMappingException, IOException {
        final File hashTestFile = new File("src/test/resources/hashes.json");
        final ServiceDescription descr = PluginTest.mapper.readValue(hashTestFile, ServiceDescription.class);

        final Map<String, String> protoHash = new HashMap<>();
        protoHash.put("EchoInterface_v001", "123");
        protoHash.put("EchoInterface_v002", "123");

        final Templates t = new Templates("test.package", "", "", descr, protoHash);
        for (final InterfaceDescription i : descr.getInterfaces()) {
            for (final InterfaceVersionDescription v : i.getInterfaceVersions()) {
                PluginTest.log.info(t.getHash(i, v, new HashSet<>(Arrays.asList("Stuff"))));
                PluginTest.log.info(t.getHash(i, v, Collections.emptySet()));
            }
        }
    }

}
