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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.flexiblepower.model.Parameter;
import org.flexiblepower.plugin.servicegen.model.InterfaceDescription;
import org.flexiblepower.plugin.servicegen.model.InterfaceVersionDescription;
import org.flexiblepower.plugin.servicegen.model.ServiceDescription;
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
 * PluginTest
 *
 * @author coenvl
 * @version 0.1
 * @since Jun 8, 2017
 */
@Slf4j
public class PluginTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testGenerate() throws JsonParseException, JsonMappingException, IOException {
        final File inputFile = new File("src/test/resources/service.json");
        final ServiceDescription descr = this.mapper.readValue(inputFile, ServiceDescription.class);

        final Templates t = new Templates("target.package", "", "", descr);
        for (final InterfaceDescription itf : descr.getInterfaces()) {
            for (final InterfaceVersionDescription vitf : itf.getInterfaceVersions()) {
                vitf.setHash("1234");
            }
            PluginTest.log.info(t.generateManagerInterface(itf));
        }
        PluginTest.log.info(t.generateDockerfile("x86", descr));
    }

    @Test
    public void testSchemaValidation() throws ProcessingException, IOException {
        final File inputFile = new File("src/test/resources/service.json");
        final URL schemaURL = this.getClass().getClassLoader().getResource("schema.json");

        final JsonNode schemaNode = JsonLoader.fromURL(schemaURL);
        final JsonNode data = JsonLoader.fromFile(inputFile);

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
        final ServiceDescription descr = this.mapper.readValue(hashTestFile, ServiceDescription.class);

        for (final InterfaceDescription i : descr.getInterfaces()) {
            for (final InterfaceVersionDescription v : i.getInterfaceVersions()) {
                v.setHash("");
                PluginTest.log.info(PluginUtils.getHash(v, new HashSet<>(Arrays.asList("Stuff"))));
                PluginTest.log.info(PluginUtils.getHash(v, Collections.emptySet()));
            }
        }
    }

    @Test
    public void testPackageName() {
        Assert.assertEquals("service", PluginUtils.toPackageName("sErvI*ce"));
        Assert.assertEquals("twee_woorden", PluginUtils.toPackageName("Twee Woorden"));
        Assert.assertEquals("_00zomaar_iets", PluginUtils.toPackageName("00*zomaar iets"));
        Assert.assertEquals("_raar", PluginUtils.toPackageName("_ra@(>=<)ar)"));
    }

    @Test
    public void testConfiguration() throws Exception {
        final File inputFile = new File("src/test/resources/config.json");
        final ServiceDescription descr = this.mapper.readValue(inputFile, ServiceDescription.class);
        final Set<Parameter> config = descr.getParameters();
        PluginTest.log.info(config.toString());

        final Map<String, String> hashes = Collections.singletonMap("ConfigurableService_004", "987");
        final Templates t = new Templates("test.config", "", "", descr);
        PluginTest.log.info(t.generateConfigInterface());
    }

}
