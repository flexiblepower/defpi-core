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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import org.flexiblepower.codegen.PluginUtils;
import org.flexiblepower.codegen.model.InterfaceDescription;
import org.flexiblepower.codegen.model.InterfaceVersionDescription;
import org.flexiblepower.codegen.model.ServiceDescription;
import org.flexiblepower.model.Parameter;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * PluginTest
 *
 * @version 0.1
 * @since Jun 8, 2017
 */
@SuppressWarnings({"static-method", "javadoc"})
public class PluginTest {

    private final static Logger log = LoggerFactory.getLogger(PluginTest.class);

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testGenerate() throws JsonParseException, JsonMappingException, IOException {
        final File inputFile = new File("src/test/resources/service.json");
        final ServiceDescription descr = this.mapper.readValue(inputFile, ServiceDescription.class);

        Assert.assertEquals("Echo Service", descr.getName());
        Assert.assertEquals("0.0.1-SNAPSHOT", descr.getVersion());

        final JavaTemplates t = new JavaTemplates("target.package", descr);
        for (final InterfaceDescription itf : descr.getInterfaces()) {
            for (final InterfaceVersionDescription vitf : itf.getInterfaceVersions()) {
                vitf.setHash("1234");
            }
            PluginTest.log.info(t.generateManagerInterface(itf));
        }
        PluginTest.log.info(t.generateDockerfile("x86", "run-java.sh"));
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

    @Test
    public void steadyHashTest() throws JsonParseException, JsonMappingException, IOException {
        final Path testFile = Files.createTempFile("http", ".proto");
        PluginUtils.downloadFile(
                "https://raw.githubusercontent.com/defpi/interfaces/6098df232adb24fe17612857ecc23597d5174680/defpi/HTTP.proto",
                testFile.toFile());

        final String fileHash = "e00da70ae21e257e79e23df20461e28edb5c6e4c16f6675b8dc4c40e574ebc06";
        Assert.assertEquals(fileHash, PluginUtils.SHA256(testFile));
        Assert.assertEquals("aafa92f5e8c919cc004f017d0c7706bf5e72594e656cf04cd67dd47b97cf7c6c",
                PluginUtils.SHA256(fileHash + ";HTTPResponse"));
        Assert.assertEquals("c46d5961b42774f80194e8308e4a1bec450881925f8d20a08a1f764acf22ed24",
                PluginUtils.SHA256(fileHash + ";HTTPRequest"));
    }

}
