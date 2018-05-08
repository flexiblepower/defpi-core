/**
 * File PythonTemplates.java
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

package org.flexiblepower.pythoncodegen;

import java.io.IOException;
import java.text.DateFormat;
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

/**
 * Templates
 *
 * @version 0.1
 * @since Jun 8, 2017
 */
public class PythonTemplates extends Templates {

    /**
     *
     */
    private static final String GENERATOR_NAME = "Python code generator for dEF-Pi";

    public PythonTemplates(final ServiceDescription descr) {
        super(descr);
    }

    public String generateServiceMain() throws IOException {
        return this.generate("ServiceMain", null, null);
    }

    /**
     * @return
     */
    public String generateServiceImplementation() throws IOException {
        return this.generate("ServiceImplementation", null, null);
    }

    public String generateRequirements() throws IOException {
        return this.generate("PipRequirements", null, null);
    }

    /**
     * @param itf
     * @param version
     * @return
     */
    public String generateHandlerInterface(final InterfaceDescription itf, final InterfaceVersionDescription version)
            throws IOException {
        return this.generate("ConnectionHandlerInterface", itf, version);
    }

    /**
     * @param itf
     * @param version
     * @return
     */
    public String generateHandlerImplementation(final InterfaceDescription itf,
            final InterfaceVersionDescription version) throws IOException {
        return this.generate("ConnectionHandlerClass", itf, version);
    }

    /**
     * @param itf
     * @param version
     * @return
     */
    public String generateManagerInterface(final InterfaceDescription itf) throws IOException {
        return this.generate("ManagerInterface", itf, null);
    }

    /**
     * @param itf
     * @param version
     * @return
     */
    public String generateManagerImplementation(final InterfaceDescription itf) throws IOException {
        return this.generate("ManagerClass", itf, null);
    }

    /**
     * Generate a file based on the template and the provided interface description and version description
     *
     * @param templateName
     * @param itf
     * @param version
     * @return
     */
    private String generate(final String templateName,
            final InterfaceDescription itf,
            final InterfaceVersionDescription version) throws IOException {
        final String template = this.getTemplate(templateName);

        final Map<String, String> replaceMap = new HashMap<>();

        // Generic stuff that is the same everywhere
        replaceMap.put("username", System.getProperty("user.name"));
        replaceMap.put("date", DateFormat.getDateTimeInstance().format(new Date()));
        replaceMap.put("generator", PythonTemplates.GENERATOR_NAME);

        replaceMap.put("service.class", PythonCodegenUtils.serviceImplClass(this.serviceDescription));
        replaceMap.put("service.version", this.serviceDescription.getVersion());
        replaceMap.put("service.name", this.serviceDescription.getName());

        // Build replaceMaps for the manager
        if (itf != null) {
            replaceMap.put("itf.manager.class", PythonCodegenUtils.managerClass(itf));
            replaceMap.put("itf.manager.interface", PythonCodegenUtils.managerInterface(itf));

            final Set<String> definitions = new HashSet<>();
            final Set<String> implementations = new HashSet<>();
            final Set<String> itfimports = new HashSet<>();
            final Set<String> itfitfimports = new HashSet<>();
            for (final InterfaceVersionDescription vitf : itf.getInterfaceVersions()) {
                final String interfaceClass = PythonCodegenUtils.connectionHandlerInterface(itf, vitf);
                final String implementationClass = PythonCodegenUtils.connectionHandlerClass(itf, vitf);
                final String interfaceVersionModule = PythonCodegenUtils.getVersion(vitf);

                final Map<String, String> handlerReplace = new HashMap<>();
                handlerReplace.put("vitf.handler.interface", interfaceClass);
                handlerReplace.put("vitf.handler.class", implementationClass);
                handlerReplace.put("vitf.version", interfaceVersionModule);
                handlerReplace.put("vitf.version.builder", PythonCodegenUtils.builderFunctionName(itf, vitf));

                definitions.add(this.replaceMap(this.getTemplate("BuilderDefinition"), handlerReplace));
                implementations.add(this.replaceMap(this.getTemplate("BuilderImplementation"), handlerReplace));

                itfimports.add(String.format("from .%s.%s import %s",
                        interfaceVersionModule,
                        implementationClass,
                        implementationClass));
                itfitfimports.add(
                        String.format("from .%s.%s import %s", interfaceVersionModule, interfaceClass, interfaceClass));
            }

            replaceMap.put("itf.manager.definitions", String.join("\n\n", definitions));
            replaceMap.put("itf.manager.implementations", String.join("\n\n", implementations));
            replaceMap.put("itf.manager.imports.interface", String.join("\n", itfitfimports));
            replaceMap.put("itf.manager.imports.implementation", String.join("\n", itfimports));
        } else {
            final Set<String> managerImports = new HashSet<>();
            for (final InterfaceDescription descr : this.serviceDescription.getInterfaces()) {
                managerImports.add(String.format("from .%s.%s import %s",
                        PythonCodegenUtils.getInterfacePackage(descr),
                        PythonCodegenUtils.managerClass(descr),
                        PythonCodegenUtils.managerClass(descr)));

            }
            replaceMap.put("service.managerimports", String.join("\n", managerImports));

        }
        // Build replaceMaps for the interface versions
        if ((itf != null) && (version != null)) {
            replaceMap.put("vitf.handler.interface", PythonCodegenUtils.connectionHandlerInterface(itf, version));
            replaceMap.put("vitf.handler.class", PythonCodegenUtils.connectionHandlerClass(itf, version));

            replaceMap.put("itf.name", itf.getName());
            replaceMap.put("vitf.version", version.getVersionName());
            replaceMap.put("vitf.receivesHash", PluginUtils.getReceiveHash(version));
            replaceMap.put("vitf.sendsHash", PluginUtils.getSendHash(version));

            final Set<String> recvClasses = new HashSet<>();
            for (final String type : version.getReceives()) {
                recvClasses.add(type);
            }
            replaceMap.put("vitf.receiveClasses", String.join(", ", recvClasses));

            final Set<String> sendClasses = new HashSet<>();
            for (final String type : version.getSends()) {
                sendClasses.add(type);
            }
            replaceMap.put("vitf.sendClasses", String.join(", ", sendClasses));

            // Add handler definitions and implementations for the connection handlers (and implementations
            // respectively)
            final Set<String> definitions = new HashSet<>();
            final Set<String> implementations = new HashSet<>();
            for (final String type : version.getReceives()) {
                final Map<String, String> handlerReplace = new HashMap<>();
                handlerReplace.put("handle.type", type);
                handlerReplace.put("handler.function", PythonCodegenUtils.typeHandlerFunction(type));

                definitions.add(this.replaceMap(this.getTemplate("HandlerDefinition"), handlerReplace));
                implementations.add(this.replaceMap(this.getTemplate("HandlerImplementation"), handlerReplace));
            }
            replaceMap.put("vitf.handler.definitions", String.join("\n\n", definitions));
            replaceMap.put("vitf.handler.implementations", String.join("\n\n", implementations));

            if (version.getType().equals(Type.PROTO)) {
                replaceMap.put("vitf.serializer", "ProtobufMessageSerializer");
            } else if (version.getType().equals(Type.XSD)) {
                replaceMap.put("vitf.serializer", "XSDMessageSerializer");
            }

            // Add imports for the handlers
            final Set<String> handlerImports = new HashSet<>();
            final Set<String> interfaceImports = new HashSet<>();
            final String versionPackage = version.getModelPackageName().replaceAll("/", ".");
            for (final String type : version.getReceives()) {
                handlerImports.add(String.format("from %s import %s", versionPackage, type));
                interfaceImports.add(String.format("from %s import %s", versionPackage, type));
            }
            for (final String type : version.getSends()) {
                interfaceImports.add(String.format("from %s import %s", versionPackage, type));
            }

            replaceMap.put("vitf.handler.imports", String.join("\n", handlerImports));
            replaceMap.put("vitf.handler.interface.imports", String.join("\n", interfaceImports));
        }

        return this.replaceMap(template, replaceMap);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.flexiblepower.pythoncodegen.Templates#getDockerBaseImage(java.lang.String)
     */
    @Override
    protected String getDockerBaseImage(final String platform) {
        if (platform.equals("x86")) {
            return "python:3.5-slim";
        } else {
            return "armhf/python:3.5-alpine";
        }
    }

}
