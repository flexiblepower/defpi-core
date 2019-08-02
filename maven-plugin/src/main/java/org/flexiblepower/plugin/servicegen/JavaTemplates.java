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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

        final Map<String, String> replaceMap = new HashMap<>();

        // Generic stuff that is the same everywhere
        replaceMap.put("username", System.getProperty("user.name"));
        replaceMap.put("date", DateFormat.getDateTimeInstance().format(new Date()));
        replaceMap.put("generator", JavaTemplates.class.getPackage().getName());

        replaceMap.put("service.package", this.servicePackage);
        replaceMap.put("service.class", JavaPluginUtils.serviceImplClass(this.serviceDescription));
        replaceMap.put("service.version", this.serviceDescription.getVersion());
        replaceMap.put("service.name", this.serviceDescription.getName());

        if (this.serviceDescription.getParameters() == null) {
            replaceMap.put("config.interface", "Void");
        } else {
            boolean importDefaultValue = false;
            replaceMap.put("config.interface", JavaPluginUtils.configInterfaceClass(this.serviceDescription));
            final Set<String> parameterDefinitions = new HashSet<>();

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

            replaceMap.put("config.definitions", String.join("\n\n", parameterDefinitions));
            replaceMap.put("config.imports",
                    importDefaultValue ? "\nimport org.flexiblepower.service.DefaultValue;\n" : "");
        }

        // Build replaceMaps for the manager
        if (itf != null) {
            final String interfacePackage = JavaPluginUtils.getPackageName(itf);
            replaceMap.put("itf.package", interfacePackage);
            replaceMap.put("itf.manager.class", JavaPluginUtils.managerClass(itf));
            replaceMap.put("itf.manager.interface", JavaPluginUtils.managerInterface(itf));

            final Set<String> definitions = new HashSet<>();
            final Set<String> implementations = new HashSet<>();
            final Set<String> itfimports = new HashSet<>();
            final Set<String> clsimports = new HashSet<>();
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

            replaceMap.put("itf.manager.definitions", String.join("\n\n", definitions));
            replaceMap.put("itf.manager.implementations", String.join("\n\n", implementations));

            replaceMap.put("itf.manager.imports.interface", String.join("\n", itfimports));
            replaceMap.put("itf.manager.imports.implementation", String.join("\n", clsimports));
        }

        // Build replaceMaps for the interface versions
        if ((itf != null) && (version != null)) {
            final String packageName = JavaPluginUtils.getPackageName(itf, version);

            replaceMap.put("vitf.handler.interface", JavaPluginUtils.connectionHandlerInterface(itf, version));
            replaceMap.put("vitf.handler.class", JavaPluginUtils.connectionHandlerClass(itf, version));

            replaceMap.put("itf.name", itf.getName());
            replaceMap.put("vitf.version", version.getVersionName());
            replaceMap.put("vitf.package", packageName);
            replaceMap.put("vitf.receivesHash", PluginUtils.getReceiveHash(version));
            replaceMap.put("vitf.sendsHash", PluginUtils.getSendHash(version));

            final Set<String> recvClasses = new HashSet<>();
            for (final String type : version.getReceives()) {
                recvClasses.add(type + ".class");
            }
            replaceMap.put("vitf.receiveClasses", String.join(", ", recvClasses));

            final Set<String> sendClasses = new HashSet<>();
            for (final String type : version.getSends()) {
                sendClasses.add(type + ".class");
            }
            replaceMap.put("vitf.sendClasses", String.join(", ", sendClasses));

            // Add handler definitions and implementations for the connection handlers (and implementations
            // respectively)
            final Set<String> definitions = new HashSet<>();
            final Set<String> implementations = new HashSet<>();
            for (final String type : version.getReceives()) {
                final Map<String, String> handlerReplace = new HashMap<>();
                handlerReplace.put("handle.type", type);
                definitions.add(this.replaceMap(this.getTemplate("HandlerDefinition"), handlerReplace));
                implementations.add(this.replaceMap(this.getTemplate("HandlerImplementation"), handlerReplace));
            }
            replaceMap.put("vitf.handler.definitions", String.join("\n\n", definitions));
            replaceMap.put("vitf.handler.implementations", String.join("\n\n", implementations));

            if (version.getType().equals(Type.XSD)) {
                replaceMap.put("vitf.serializer", "XSDMessageSerializer");
            } else {
                replaceMap.put("vitf.serializer", "ProtobufMessageSerializer");
            }

            // Add imports for the handlers
            final Set<String> handlerImports = new HashSet<>();
            final Set<String> interfaceImports = new HashSet<>();
            for (final String type : version.getReceives()) {
                if (type.equals("RamlRequest")) {
                    handlerImports.add("import org.flexiblepower.proto.RamlProto.RamlRequest;");
                    interfaceImports.add("import org.flexiblepower.proto.RamlProto.RamlRequest;");
                } else if (type.equals("RamlResponse")) {
                    handlerImports.add("import org.flexiblepower.proto.RamlProto.RamlResponse;");
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

            replaceMap.put("vitf.handler.imports", String.join("\n", handlerImports));
            replaceMap.put("vitf.handler.interface.imports", String.join("\n", interfaceImports));
        }

        return this.replaceMap(template, replaceMap);
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
