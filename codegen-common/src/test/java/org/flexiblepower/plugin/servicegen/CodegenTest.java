/**
 * File CodegenTest.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.plugin.servicegen;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.flexiblepower.codegen.PluginUtils;
import org.flexiblepower.codegen.model.InterfaceDescription;
import org.flexiblepower.codegen.model.InterfaceVersionDescription;
import org.flexiblepower.codegen.model.ServiceDescription;
import org.junit.Assert;
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
 * CodegenTest
 *
 * @author coenvl
 * @version 0.1
 * @since Oct 4, 2017
 */
@Slf4j
public class CodegenTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testSchemaValidation() throws ProcessingException, IOException {
        final File inputFile = new File("src/test/resources/service.json");
        final URL schemaURL = this.getClass().getClassLoader().getResource("schema.json");

        final JsonNode schemaNode = JsonLoader.fromURL(schemaURL);
        final JsonNode data = JsonLoader.fromFile(inputFile);

        final JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
        final JsonSchema schema = factory.getJsonSchema(schemaNode);
        final ProcessingReport report = schema.validate(data);
        CodegenTest.log.info("report: {}", report);

        final SyntaxValidator syntaxValidator = factory.getSyntaxValidator();
        CodegenTest.log.info("syntax: {}", syntaxValidator.schemaIsValid(schemaNode));
    }

    @Test
    public void testComputeHashes() throws JsonParseException, JsonMappingException, IOException {
        final File hashTestFile = new File("src/test/resources/hashes.json");
        final ServiceDescription descr = this.mapper.readValue(hashTestFile, ServiceDescription.class);

        for (final InterfaceDescription i : descr.getInterfaces()) {
            for (final InterfaceVersionDescription v : i.getInterfaceVersions()) {
                v.setHash("");
                CodegenTest.log.info(PluginUtils.getHash(v, new HashSet<>(Arrays.asList("Stuff"))));
                CodegenTest.log.info(PluginUtils.getHash(v, Collections.emptySet()));
            }
        }
    }

    @Test
    public void capsTest() {
        Assert.assertEquals("this_is_a_test", PluginUtils.snakeCaps("This is a Test"));
        Assert.assertEquals("ThisIsATest", PluginUtils.camelCaps("This is a Test"));
    }
}
