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
package org.flexiblepower.codegen;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
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
 * The Templates objects manages all templates, and filling them in. This abstract base class should be extended by any
 * plugin that generates code from the service definition.
 *
 * @version 0.1
 * @since Oct 4, 2017
 */
public abstract class Templates {

    private final static boolean PRETTY_PRINT_JSON = true;
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * The service description to fill in templates
     */
    protected final ServiceDescription serviceDescription;

    /**
     * Create the Templates object according to the specified service description
     *
     * @param descr The service description to use to fill in the templates
     */
    protected Templates(final ServiceDescription descr) {
        this.serviceDescription = descr;
    }

    /**
     * Produce the docker base image to use for a service depending on the target platform. Depending on the
     * implementing code generator, this will return a different docker base image. For instance the service generator
     * for java services will use a docker image that contains a JRE.
     *
     * @param platform The target platform to use as base image (e.g. x64 or ARM)
     * @return The Docker base image to build the service on
     */
    protected abstract String getDockerBaseImage(String platform);

    /**
     * Depending on the implementing code generator, additional items may be added to the docker template. This function
     * can be used to extends the templates for the dockerfile.
     *
     * @return The additional replace map for the docker file.
     */
    @SuppressWarnings("static-method")
    protected Map<String, String> getAdditionalDockerReplaceMap() {
        return Collections.emptyMap();
    }

    /**
     * Generate the docker file for this service.
     *
     * @param platform The target platform to create the docker file for (e.g. x64 or ARM)
     * @param dockerEntryPoint The command to use when running the docker container
     * @return The dockerfile that implements the dEF-Pi service
     * @throws JsonProcessingException When an exception occurs writing the interfaces to a JSON object
     * @throws IOException When an exception ocurs obtaining the docker file template
     */
    public String generateDockerfile(final String platform, final String dockerEntryPoint)
            throws JsonProcessingException,
            IOException {
        final Map<String, String> replace = new HashMap<>();
        replace.put("from", this.getDockerBaseImage(platform));
        replace.put("service.name", this.serviceDescription.getName());
        replace.put("service.description", this.serviceDescription.getDescription());
        replace.put("service.iconURL", this.serviceDescription.getIconURL());

        final ObjectWriter writer = Templates.PRETTY_PRINT_JSON ? this.mapper.writerWithDefaultPrettyPrinter()
                : this.mapper.writer();

        final Set<Parameter> parameters = this.serviceDescription.getParameters();
        if (parameters == null) {
            replace.put("parameters", "null");
        } else {
            replace.put("parameters", writer.writeValueAsString(parameters).replaceAll("\n", " \\\\\n"));
        }

        final Set<InterfaceDescription> input = this.serviceDescription.getInterfaces();

        final Set<Interface> serviceInterfaces = new HashSet<>();
        for (final InterfaceDescription descr : input) {
            final List<InterfaceVersion> versionList = new ArrayList<>();
            for (final InterfaceVersionDescription ivd : descr.getInterfaceVersions()) {
                final String sendHash = PluginUtils.getSendHash(ivd);
                final String recvHash = PluginUtils.getReceiveHash(ivd);
                versionList.add(new InterfaceVersion(ivd.getVersionName(), recvHash, sendHash));
            }
            serviceInterfaces.add(new Interface(this.serviceDescription.getId() + "/" + descr.getId(),
                    descr.getName(),
                    this.serviceDescription.getId(),
                    versionList,
                    descr.isAllowMultiple(),
                    descr.isAutoConnect()));
        }

        final String interfaces = writer.writeValueAsString(serviceInterfaces);
        replace.put("interfaces", interfaces.replaceAll("\n", " \\\\ \n"));
        replace.put("entrypoint", dockerEntryPoint);
        replace.putAll(this.getAdditionalDockerReplaceMap());

        return this.replaceMap(this.getTemplate("Dockerfile"), replace);
    }

    /**
     * Find a template file by its file name
     *
     * @param name The filename of the template
     * @return The contents of the file
     * @throws IOException when any exception occurs while reading the file
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
     * Replace all keys in the template with their values. Any occurrence of "<i>{{KEY}}</i>" in the template text will
     * be replaced with the corresponding <i>value</i> in the replace map.
     *
     * @param template The code template as a flat piece of text with placeholders as "<i>{{KEY}}</i>"
     * @param replace The map of replacement key/value pairs
     * @return The template with all template markers replaced with the corresponding values from the map
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
