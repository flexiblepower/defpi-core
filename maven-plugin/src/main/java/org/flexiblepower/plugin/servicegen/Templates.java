package org.flexiblepower.plugin.servicegen;

import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;

import org.flexiblepower.model.Interface;
import org.flexiblepower.model.InterfaceVersion;
import org.flexiblepower.model.Parameter;
import org.flexiblepower.plugin.servicegen.model.InterfaceDescription;
import org.flexiblepower.plugin.servicegen.model.InterfaceVersionDescription;
import org.flexiblepower.plugin.servicegen.model.InterfaceVersionDescription.Type;
import org.flexiblepower.plugin.servicegen.model.ServiceDescription;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

/**
 * Templates
 *
 * @author coenvl
 * @version 0.1
 * @since Jun 8, 2017
 */
public class Templates {

    private final static boolean PRETTY_PRINT_JSON = true;
    private final String servicePackage;
    private final ServiceDescription serviceDescription;
    private final ObjectMapper mapper = new ObjectMapper();

    public Templates(final String targetPackage, final ServiceDescription descr) {
        this.servicePackage = targetPackage;
        this.serviceDescription = descr;
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
     * Generate the docker file for this service
     *
     * @param platform
     * @param service
     * @return
     * @throws JsonProcessingException
     */
    public String generateDockerfile(final String platform, final ServiceDescription service)
            throws JsonProcessingException,
            IOException {
        final Map<String, String> replace = new HashMap<>();
        if (platform.equals("x86")) {
            replace.put("from", "java:alpine");
        } else {
            replace.put("from", "larmog/armhf-alpine-java:jdk-8u73");
        }

        replace.put("service.name", service.getName());

        final ObjectWriter writer = Templates.PRETTY_PRINT_JSON ? this.mapper.writerWithDefaultPrettyPrinter()
                : this.mapper.writer();

        final Set<Parameter> parameters = service.getParameters();
        if (parameters == null) {
            replace.put("parameters", "null");
        } else {
            replace.put("parameters", writer.writeValueAsString(parameters).replaceAll("\n", " \\\\ \n"));
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

        return Templates.replaceMap(this.getTemplate("Dockerfile"), replace);
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
        replaceMap.put("generator", Templates.class.getPackage().getName().toString());

        replaceMap.put("service.package", this.servicePackage);
        replaceMap.put("service.class", PluginUtils.serviceImplClass(this.serviceDescription));
        replaceMap.put("service.version", this.serviceDescription.getVersion());
        replaceMap.put("service.name", this.serviceDescription.getName());

        if (this.serviceDescription.getParameters() == null) {
            replaceMap.put("config.interface", "Void");
        } else {
            boolean importDefaultValue = false;
            replaceMap.put("config.interface", PluginUtils.configInterfaceClass(this.serviceDescription));
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
                        param.getId()));
            }

            replaceMap.put("config.definitions", String.join("\n\n", parameterDefinitions));
            replaceMap.put("config.imports",
                    importDefaultValue ? "\nimport org.flexiblepower.service.DefaultValue;\n" : "");
        }

        // Build replaceMaps for the manager
        if (itf != null) {
            final String interfacePackage = PluginUtils.getPackageName(itf);
            replaceMap.put("itf.package", interfacePackage);
            replaceMap.put("itf.manager.class", PluginUtils.managerClass(itf));
            replaceMap.put("itf.manager.interface", PluginUtils.managerInterface(itf));

            final Set<String> definitions = new HashSet<>();
            final Set<String> implementations = new HashSet<>();
            final Set<String> itfimports = new HashSet<>();
            final Set<String> clsimports = new HashSet<>();
            for (final InterfaceVersionDescription vitf : itf.getInterfaceVersions()) {
                final String interfaceVersionPackage = PluginUtils.getPackageName(vitf);
                final String interfaceClass = PluginUtils.connectionHandlerInterface(itf, vitf);
                final String implementationClass = PluginUtils.connectionHandlerClass(itf, vitf);

                final Map<String, String> handlerReplace = new HashMap<>();
                handlerReplace.put("vitf.handler.interface", interfaceClass);
                handlerReplace.put("vitf.handler.class", implementationClass);
                handlerReplace.put("vitf.version", PluginUtils.getVersion(vitf));

                definitions.add(Templates.replaceMap(this.getTemplate("BuilderDefinition"), handlerReplace));
                implementations.add(Templates.replaceMap(this.getTemplate("BuilderImplementation"), handlerReplace));
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
            final String packageName = PluginUtils.getPackageName(itf, version);

            replaceMap.put("vitf.handler.interface", PluginUtils.connectionHandlerInterface(itf, version));
            replaceMap.put("vitf.handler.class", PluginUtils.connectionHandlerClass(itf, version));

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
                definitions.add(Templates.replaceMap(this.getTemplate("HandlerDefinition"), handlerReplace));
                implementations.add(Templates.replaceMap(this.getTemplate("HandlerImplementation"), handlerReplace));
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

        return Templates.replaceMap(template, replaceMap);
    }

    /**
     * Find a template file
     *
     * @param name
     * @return
     * @throws IOException
     */
    private String getTemplate(final String name) throws IOException {
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
    private static String replaceMap(final String template, final Map<String, String> replace) {
        String ret = template;
        for (final Entry<String, String> entry : replace.entrySet()) {
            if (entry.getValue() != null) {
                ret = ret.replace("{{" + entry.getKey() + "}}", entry.getValue());
            }
        }
        return ret;
    }

}
