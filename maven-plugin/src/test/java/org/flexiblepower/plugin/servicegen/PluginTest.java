/**
 * File PluginTest.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.plugin.servicegen;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.flexiblepower.plugin.servicegen.model.InterfaceDescription;
import org.flexiblepower.plugin.servicegen.model.InterfaceVersionDescription;
import org.flexiblepower.plugin.servicegen.model.ServiceDescription;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * PluginTest
 *
 * @author coenvl
 * @version 0.1
 * @since Jun 8, 2017
 */
public class PluginTest {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static ServiceDescription descr;

    @BeforeClass
    public static void load() throws JsonParseException, JsonMappingException, IOException {
        final File inputFile = new File("src/test/resources/service.json");

        PluginTest.descr = PluginTest.mapper.readValue(inputFile, ServiceDescription.class);
    }

    @Test
    public void testGenerate() {
        final Map<String, String> hashes = new HashMap<>();
        hashes.put("EchoInterfacev001", "1");
        hashes.put("DropbackInterfacev001", "2");
        hashes.put("DropbackInterfacev002", "3");
        final Templates t = new Templates("target.package", PluginTest.descr, hashes);
        for (final InterfaceDescription itf : PluginTest.descr.getInterfaces()) {
            for (final InterfaceVersionDescription version : itf.getInterfaceVersions()) {
                System.out.println(t.generateFactory(itf, version));
            }
        }
    }

}
