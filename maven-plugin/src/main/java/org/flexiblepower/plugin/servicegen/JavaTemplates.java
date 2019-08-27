/*-
 * #%L
 * dEF-Pi service creation maven plugin
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
package org.flexiblepower.plugin.servicegen;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.flexiblepower.codegen.PluginUtils;
import org.flexiblepower.codegen.Templates;
import org.flexiblepower.codegen.model.InterfaceDescription;
import org.flexiblepower.codegen.model.InterfaceVersionDescription;
import org.flexiblepower.codegen.model.InterfaceVersionDescription.Type;
import org.flexiblepower.codegen.model.ServiceDescription;
import org.flexiblepower.model.Parameter;

/**
 * Templates for the java code generation
 *
 * @version 0.1
 * @since Jun 8, 2017
 */
class JavaTemplates extends Templates {

    private final String servicePackage;
    private final List<String> ramlResourceNames;

    /**
     * Create the object that provides the Templates for the java code generation with a specific target java package
     * name, and a service description
     *
     * @param targetPackage The main service package "parent" this is obtained from the maven group id
     * @param descr The service description as parsed from the service.json file
     */
    JavaTemplates(final String targetPackage, final ServiceDescription descr) {
        super(descr);
        this.servicePackage = targetPackage;
        this.ramlResourceNames = new LinkedList<>();
    }

    /**
     * Add raml resources if there are any
     *
     * @param resources A list of raml resource names that need to be added to the templates
     */
    public void addRamlResouceNames(final List<String> resources) {
        this.ramlResourceNames.addAll(resources);
    }

    /**
     * Generate the service implementation code for this project
     *
     * @return The code that implements the service for the project.
     * @throws IOException When an exception occurs while reading the template file
     */
    String generateServiceImplementation() throws IOException {
        return this.generate("ServiceImplementation", null, null);
    }

    /**
     * Generate the service configuration interface for this service.
     *
     * @return The code of the configuration interface for the service
     * @throws IOException When an exception occurs while reading the template file
     */
    String generateConfigInterface() throws IOException {
        return this.generate("ConfigInterface", null, null);
    }

    /**
     * Generate the code of the java interface that defines the connection handler for the specified dEF-Pi interface
     * version.
     *
     * @param itf The interface to generate the handler interface for
     * @param version The version of the interface to generate the code for
     * @return The code of the connection handler interface for the specified version of the interface
     * @throws IOException When an exception occurs while reading the template file
     */
    String generateHandlerInterface(final InterfaceDescription itf, final InterfaceVersionDescription version)
            throws IOException {
        return this.generate("ConnectionHandlerInterface", itf, version);
    }

    /**
     * Generate the code that implements the connection handler for the specified dEF-Pi interface version.
     *
     * @param itf The interface to generate the handler implementation for
     * @param version The version of the interface to generate the code for
     * @return The code of the connection handler implementation for the specified version of the interface
     * @throws IOException When an exception occurs while reading the template file
     */
    String generateHandlerImplementation(final InterfaceDescription itf, final InterfaceVersionDescription version)
            throws IOException {
        return this.generate("ConnectionHandlerClass", itf, version);
    }

    /**
     * Generate the code of the java interface that defines the manager for the specified dEF-Pi interface.
     *
     * @param itf The interface to generate the manager interface for
     * @return The code of the connection manager interface for the specified interface
     * @throws IOException When an exception occurs while reading the template file
     */
    String generateManagerInterface(final InterfaceDescription itf) throws IOException {
        return this.generate("ManagerInterface", itf, null);
    }

    /**
     * Generate the code that implements the manager for the specified dEF-Pi interface.
     *
     * @param itf The interface to generate the manager implementation for
     * @return The code of the connection manager implementation for the specified interface
     * @throws IOException When an exception occurs while reading the template file
     */
    String generateManagerImplementation(final InterfaceDescription itf) throws IOException {
        return this.generate("ManagerClass", itf, null);
    }

    /**
     * Generate a file based on the template and the provided interface description and version description
     *
     * @param templateName The template to use while creating the code
     * @param itf The interface to generate the code for
     * @param version The version of the interface to generate the code for
     * @return The filled-in template, which should provide valid java code
     * @throws IOException When an exception occurs while reading the template file
     */
    private String generate(final String templateName,
            final InterfaceDescription itf,
            final InterfaceVersionDescription version) throws IOException {
        final String template = this.getTemplate(templateName);

        final Map<String, String> templates = new HashMap<>();

        // Generic stuff that is the same everywhere
        templates.put("username", System.getProperty("user.name"));
        templates.put("date", DateFormat.getDateTimeInstance().format(new Date()));
        templates.put("generator", JavaTemplates.class.getPackage().getName());

        templates.put("service.package", this.servicePackage);
        templates.put("service.class", JavaPluginUtils.serviceImplClass(this.serviceDescription));
        templates.put("service.version", this.serviceDescription.getVersion());
        templates.put("service.name", this.serviceDescription.getName());

        if (this.serviceDescription.getParameters() == null) {
            templates.put("config.interface", "Void");
        } else {
            boolean importDefaultValue = false;
            templates.put("config.interface", JavaPluginUtils.configInterfaceClass(this.serviceDescription));
            final Set<String> parameterDefinitions = new TreeSet<>();

            for (final Parameter param : this.serviceDescription.getParameters()) {
                final String javadoc = ((param.getName() == null) || param.getName().isEmpty() ? ""
                        : "    /**\n     * @return " + param.getName() + "\n     */\n");
                final String annotation = (param.getDefaultValue() == null ? ""
                        : "    @DefaultValue(\"" + param.getDefaultValue() + "\")\n");
                final String arraydef = (param.isArray() ? "[]" : "");
                importDefaultValue = importDefaultValue || !annotation.isEmpty();
                parameterDefinitions.add(String.format("%s%s    public %s%s get%s();",
                        javadoc,
                        annotation,
                        param.getType().getJavaTypeName(),
                        arraydef,
                        JavaPluginUtils.getParameterId(param)));
            }

            templates.put("config.definitions", String.join("\n\n", parameterDefinitions));
            templates.put("config.imports",
                    importDefaultValue ? "\nimport org.flexiblepower.service.DefaultValue;\n" : "");
        }

        // Build replaceMaps for the manager
        if (itf != null) {
            final String interfacePackage = JavaPluginUtils.getPackageName(itf);
            templates.put("itf.package", interfacePackage);
            templates.put("itf.manager.class", JavaPluginUtils.managerClass(itf));
            templates.put("itf.manager.interface", JavaPluginUtils.managerInterface(itf));

            final Set<String> definitions = new TreeSet<>();
            final Set<String> implementations = new TreeSet<>();
            final Set<String> itfimports = new TreeSet<>();
            final Set<String> clsimports = new TreeSet<>();
            for (final InterfaceVersionDescription vitf : itf.getInterfaceVersions()) {
                final String interfaceVersionPackage = JavaPluginUtils.getPackageName(vitf);
                final String interfaceClass = JavaPluginUtils.connectionHandlerInterface(itf, vitf);
                final String implementationClass = JavaPluginUtils.connectionHandlerClass(itf, vitf);

                final Map<String, String> handlerReplace = new HashMap<>();
                handlerReplace.put("vitf.handler.interface", interfaceClass);
                handlerReplace.put("vitf.handler.class", implementationClass);
                handlerReplace.put("vitf.version", JavaPluginUtils.getVersion(vitf));

                definitions.add(this.replaceMap(this.getTemplate("BuilderDefinition"), handlerReplace));
                implementations.add(this.replaceMap(this.getTemplate("BuilderImplementation"), handlerReplace));
                itfimports.add(String.format("import %s.%s.%s.%s;",
                        this.servicePackage,
                        interfacePackage,
                        interfaceVersionPackage,
                        interfaceClass));
                clsimports.add(String.format("import %s.%s.%s.%s;",
                        this.servicePackage,
                        interfacePackage,
                        interfaceVersionPackage,
                        interfaceClass));
                clsimports.add(String.format("import %s.%s.%s.%s;",
                        this.servicePackage,
                        interfacePackage,
                        interfaceVersionPackage,
                        implementationClass));
            }

            templates.put("itf.manager.definitions", String.join("\n\n", definitions));
            templates.put("itf.manager.implementations", String.join("\n\n", implementations));

            templates.put("itf.manager.imports.interface", String.join("\n", itfimports));
            templates.put("itf.manager.imports.implementation", String.join("\n", clsimports));
        }

        // Build replaceMaps for the interface versions
        if ((itf != null) && (version != null)) {
            final String packageName = JavaPluginUtils.getPackageName(itf, version);

            templates.put("vitf.handler.interface", JavaPluginUtils.connectionHandlerInterface(itf, version));
            templates.put("vitf.handler.class", JavaPluginUtils.connectionHandlerClass(itf, version));

            templates.put("itf.name", itf.getName());
            templates.put("vitf.version", version.getVersionName());
            templates.put("vitf.package", packageName);
            templates.put("vitf.receivesHash", PluginUtils.getReceiveHash(version));
            templates.put("vitf.sendsHash", PluginUtils.getSendHash(version));

            final Set<String> recvClasses = new TreeSet<>();
            for (final String type : version.getReceives()) {
                recvClasses.add(type + ".class");
            }
            templates.put("vitf.receiveClasses", String.join(", ", recvClasses));

            final Set<String> sendClasses = new TreeSet<>();
            for (final String type : version.getSends()) {
                sendClasses.add(type + ".class");
            }
            templates.put("vitf.sendClasses", String.join(", ", sendClasses));

            // Add handler definitions and implementations for the connection handlers (and implementations
            // respectively)
            final Set<String> definitions = new TreeSet<>();
            final Set<String> implementations = new TreeSet<>();
            for (final String type : version.getReceives()) {
                final Map<String, String> handlerReplace = new HashMap<>();

                handlerReplace.put("handle.type", type);
                if (type.equals("RamlRequest")) {
                    definitions.add(this.replaceMap(this.getTemplate("RamlHandlerImplementation"), handlerReplace));
                } else {
                    definitions.add(this.replaceMap(this.getTemplate("HandlerDefinition"), handlerReplace));
                    implementations.add(this.replaceMap(this.getTemplate("HandlerImplementation"), handlerReplace));
                }
            }

            if (version.getType().equals(Type.RAML)) {
                for (final String resource : this.ramlResourceNames) {
                    final String resourceClass = PluginUtils.camelCaps(resource);
                    final Map<String, String> resourceMap = Collections.singletonMap("resource.type", resourceClass);
                    definitions.add(this.replaceMap(this.getTemplate("RamlResourceProviderDefinition"), resourceMap));
                    implementations
                            .add(this.replaceMap(this.getTemplate("RamlResourceProviderImplementation"), resourceMap));
                }
            }

            templates.put("vitf.handler.definitions", String.join("\n\n", definitions));
            templates.put("vitf.handler.implementations", String.join("\n\n", implementations));

            if (version.getType().equals(Type.XSD)) {
                templates.put("vitf.serializer", "XSDMessageSerializer");
            } else {
                templates.put("vitf.serializer", "ProtobufMessageSerializer");
            }

            // Add imports for the handlers
            final Set<String> handlerImports = new TreeSet<>();
            final Set<String> interfaceImports = new TreeSet<>();
            for (final String type : version.getReceives()) {
                if (type.equals("RamlRequest")) {
                    interfaceImports.add("import org.flexiblepower.raml.server.RamlRequestHandler;");
                    interfaceImports.add("import org.flexiblepower.proto.RamlProto.RamlRequest;");
                } else if (type.equals("RamlResponse")) {
                    handlerImports.add("import org.flexiblepower.proto.RamlProto.RamlResponse;");
                    handlerImports.add("import org.flexiblepower.raml.server.RamlResponseHandler;");
                    interfaceImports.add("import org.flexiblepower.proto.RamlProto.RamlResponse;");
                } else {
                    handlerImports.add(String.format("import %s.%s;", version.getModelPackageName(), type));
                    interfaceImports.add(String.format("import %s.%s;", version.getModelPackageName(), type));
                }
            }
            for (final String type : version.getSends()) {
                if (type.equals("RamlRequest")) {
                    interfaceImports.add("import org.flexiblepower.proto.RamlProto.RamlRequest;");
                } else if (type.equals("RamlResponse")) {
                    interfaceImports.add("import org.flexiblepower.proto.RamlProto.RamlResponse;");
                } else {
                    interfaceImports.add(String.format("import %s.%s;", version.getModelPackageName(), type));
                }
            }

            if (!this.ramlResourceNames.isEmpty()) {
                for (final String resource : this.ramlResourceNames) {
                    interfaceImports.add(String
                            .format("import %s.%s;", version.getModelPackageName(), PluginUtils.camelCaps(resource)));
                    handlerImports.add(String
                            .format("import %s.%s;", version.getModelPackageName(), PluginUtils.camelCaps(resource)));
                }
            }

            templates.put("vitf.handler.imports", String.join("\n", handlerImports));
            templates.put("vitf.handler.interface.imports", String.join("\n", interfaceImports));
        }

        return this.replaceMap(template, templates);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.flexiblepower.plugin.servicegen.Templates#getDockerBaseImage(java.lang.String)
     */
    @Override
    protected String getDockerBaseImage(final String platform) {
        if (platform.equals("x86")) {
            return "openjdk:11-jre-slim"; // "java:alpine";
        } else {
            return "arm32v6/openjdk:8-jre-alpine"; // "larmog/armhf-alpine-java:jdk-8u73";
        }
    }

    @Override
    protected Map<String, String> getAdditionalDockerReplaceMap() {
        final Map<String, String> ret = new HashMap<>();
        ret.put("service.package", this.servicePackage);
        return Collections.unmodifiableMap(ret);
    }

}
