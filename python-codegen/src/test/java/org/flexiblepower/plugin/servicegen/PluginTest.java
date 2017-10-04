/**
 * File PluginTest.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.plugin.servicegen;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.flexiblepower.codegen.model.InterfaceDescription;
import org.flexiblepower.codegen.model.InterfaceVersionDescription;
import org.flexiblepower.codegen.model.ServiceDescription;
import org.flexiblepower.model.Parameter;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * PluginTest
 *
 * @author coenvl
 * @version 0.1
 * @since Jun 8, 2017
 */
@Slf4j
@SuppressWarnings("static-method")
public class PluginTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testGenerate() throws JsonParseException, JsonMappingException, IOException {
        final File inputFile = new File("src/test/resources/service.json");
        final ServiceDescription descr = this.mapper.readValue(inputFile, ServiceDescription.class);

        final PythonTemplates t = new PythonTemplates(descr);
        for (final InterfaceDescription itf : descr.getInterfaces()) {
            for (final InterfaceVersionDescription vitf : itf.getInterfaceVersions()) {
                vitf.setHash("1234");
            }
        }
        PluginTest.log.info(t.generateServiceImplementation());
        PluginTest.log.info(t.generateDockerfile("x86", descr, "run-java.sh"));
    }

    @Test
    public void testPackageName() {
        Assert.assertEquals("service", JavaPluginUtils.toPackageName("sErvI*ce"));
        Assert.assertEquals("twee_woorden", JavaPluginUtils.toPackageName("Twee Woorden"));
        Assert.assertEquals("_00zomaar_iets", JavaPluginUtils.toPackageName("00*zomaar iets"));
        Assert.assertEquals("_raar", JavaPluginUtils.toPackageName("_ra@(>=<)ar)"));
    }

    @Test
    public void testConfiguration() throws Exception {
        final File inputFile = new File("src/test/resources/config.json");
        final ServiceDescription descr = this.mapper.readValue(inputFile, ServiceDescription.class);
        final Set<Parameter> config = descr.getParameters();
        PluginTest.log.info(config.toString());

        final PythonTemplates t = new PythonTemplates(descr);
        PluginTest.log.info(t.generateConfigInterface());
    }

}
