package org.flexiblepower.plugin.servicegen;

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
import org.flexiblepower.model.Parameter;

/**
 * Templates
 *
 * @author coenvl
 * @version 0.1
 * @since Jun 8, 2017
 */
public class PythonTemplates extends Templates {

    public PythonTemplates(final ServiceDescription descr) {
        super(descr);
    }

    /**
     * @return
     */
    public String generateServiceImplementation() throws IOException {
        return this.generate("ServiceImplementation", null, null);
    }

    /**
     * @return
     * @throws IOException
     */
    public String generateConfigInterface() throws IOException {
        return this.generate("ConfigInterface", null, null);
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
        replaceMap.put("generator", PythonTemplates.class.getPackage().getName().toString());

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
                importDefaultValue = (annotation.isEmpty() ? importDefaultValue : true);
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
                // itfimports.add(String.format("import %s.%s.%s.%s;",
                // this.servicePackage,
                // interfacePackage,
                // interfaceVersionPackage,
                // interfaceClass));
                // clsimports.add(String.format("import %s.%s.%s.%s;",
                // this.servicePackage,
                // interfacePackage,
                // interfaceVersionPackage,
                // interfaceClass));
                // clsimports.add(String.format("import %s.%s.%s.%s;",
                // this.servicePackage,
                // interfacePackage,
                // interfaceVersionPackage,
                // implementationClass));
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
            replaceMap.put("vitf.receivesHash", PluginUtils.getHash(version, version.getReceives()));
            replaceMap.put("vitf.sendsHash", PluginUtils.getHash(version, version.getSends()));

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

            // Add imports for the handlers
            final Set<String> imports = new HashSet<>();
            final Set<String> messageSet = new HashSet<>();
            messageSet.addAll(version.getReceives());
            messageSet.addAll(version.getSends());

            if (version.getType().equals(Type.PROTO)) {
                replaceMap.put("vitf.serializer", "ProtobufMessageSerializer");

                for (final String type : messageSet) {
                    imports.add(String.format("import %s.%s;", version.getModelPackageName(), type));
                }
            } else if (version.getType().equals(Type.XSD)) {
                replaceMap.put("vitf.serializer", "XSDMessageSerializer");
                imports.add(String.format("import %s.*;", version.getModelPackageName()));
            }

            replaceMap.put("vitf.handler.imports", String.join("\n", imports));
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
            return "java:alpine";
        } else {
            return "larmog/armhf-alpine-java:jdk-8u73";
        }
    }

}
