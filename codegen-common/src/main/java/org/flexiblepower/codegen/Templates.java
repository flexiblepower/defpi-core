/**
 * File Templates.java
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

package org.flexiblepower.codegen;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;

import org.flexiblepower.codegen.model.InterfaceDescription;
import org.flexiblepower.codegen.model.InterfaceVersionDescription;
import org.flexiblepower.codegen.model.ServiceDescription;
import org.flexiblepower.model.Interface;
import org.flexiblepower.model.InterfaceVersion;
import org.flexiblepower.model.Parameter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

/**
 * Templates
 *
 * @version 0.1
 * @since Oct 4, 2017
 */
public abstract class Templates {

    private final static boolean PRETTY_PRINT_JSON = true;
    private final ObjectMapper mapper = new ObjectMapper();

    protected final ServiceDescription serviceDescription;

    public Templates(final ServiceDescription descr) {
        this.serviceDescription = descr;
    }

    protected abstract String getDockerBaseImage(String platform);

    /**
     * Generate the docker file for this service
     *
     * @param platform
     * @param service
     * @param dockerEntryPoint
     * @return
     * @throws JsonProcessingException
     */
    public String
            generateDockerfile(final String platform, final ServiceDescription service, final String dockerEntryPoint)
                    throws JsonProcessingException,
                    IOException {
        final Map<String, String> replace = new HashMap<>();
        replace.put("from", this.getDockerBaseImage(platform));
        replace.put("service.name", service.getName());

        final ObjectWriter writer = Templates.PRETTY_PRINT_JSON ? this.mapper.writerWithDefaultPrettyPrinter()
                : this.mapper.writer();

        final Set<Parameter> parameters = service.getParameters();
        if (parameters == null) {
            replace.put("parameters", "null");
        } else {
            replace.put("parameters", writer.writeValueAsString(parameters).replaceAll("\n", " \\\\\n"));
        }

        final Set<InterfaceDescription> input = service.getInterfaces();

        final Set<Interface> serviceInterfaces = new HashSet<>();
        for (final InterfaceDescription descr : input) {
            final List<InterfaceVersion> versionList = new ArrayList<>();
            for (final InterfaceVersionDescription ivd : descr.getInterfaceVersions()) {
                final String sendHash = PluginUtils.getHash(ivd, ivd.getSends());
                final String recvHash = PluginUtils.getHash(ivd, ivd.getReceives());
                versionList.add(new InterfaceVersion(ivd.getVersionName(), recvHash, sendHash));
            }
            serviceInterfaces.add(new Interface(service.getId() + "/" + descr.getId(),
                    descr.getName(),
                    service.getId(),
                    versionList,
                    descr.isAllowMultiple(),
                    descr.isAutoConnect()));
        }

        final String interfaces = writer.writeValueAsString(serviceInterfaces);
        // final String encoded = Base64.getEncoder().encodeToString(interfaces.getBytes());
        replace.put("interfaces", interfaces.replaceAll("\n", " \\\\ \n"));

        replace.put("entrypoint", dockerEntryPoint);

        return this.replaceMap(this.getTemplate("Dockerfile"), replace);
    }

    /**
     * Find a template file
     *
     * @param name
     * @return
     * @throws IOException
     */
    protected String getTemplate(final String name) throws IOException {
        String result = "";
        final URL url = this.getClass().getClassLoader().getResource("templates/" + name + ".tpl");
        try (final Scanner scanner = new Scanner(url.openStream())) {
            result = scanner.useDelimiter("\\A").next();
        }
        return result;
    }

    /**
     * Replace all keys in the template with their values
     *
     * @param template
     * @param replace
     * @return
     */
    @SuppressWarnings("static-method")
    protected String replaceMap(final String template, final Map<String, String> replace) {
        String ret = template;
        for (final Entry<String, String> entry : replace.entrySet()) {
            if (entry.getValue() != null) {
                ret = ret.replace("{{" + entry.getKey() + "}}", entry.getValue());
            }
        }
        return ret;
    }

}
