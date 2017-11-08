/**
 * File PluginTest.java
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

        final JavaTemplates t = new JavaTemplates("target.package", descr);
        for (final InterfaceDescription itf : descr.getInterfaces()) {
            for (final InterfaceVersionDescription vitf : itf.getInterfaceVersions()) {
                vitf.setHash("1234");
            }
            PluginTest.log.info(t.generateManagerInterface(itf));
        }
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

        final JavaTemplates t = new JavaTemplates("test.config", descr);
        PluginTest.log.info(t.generateConfigInterface());
    }

}
