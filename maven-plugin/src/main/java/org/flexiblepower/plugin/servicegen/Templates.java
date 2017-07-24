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
    private final String protobufOutputPackage;
    private final String xsdOutputPackage;
    private final ServiceDescription serviceDescription;
    private final Map<String, String> hashes;
    private final ObjectMapper mapper = new ObjectMapper();

    public Templates(final String targetPackage,
            final String protobufOutputPackage,
            final String xsdOutputPackage,
            final ServiceDescription descr,
            final Map<String, String> hashes) {
        this.servicePackage = targetPackage;
        this.protobufOutputPackage = protobufOutputPackage;
        this.xsdOutputPackage = xsdOutputPackage;
        this.serviceDescription = descr;
        this.hashes = hashes;
    }

    /**
     * @return
     */
    public String generateServiceImplementation() {
        return this.generate("ServiceImplementation", null, null);
    }

    /**
     * @param itf
     * @param version
     * @return
     */
    public String generateConnectionHandler(final InterfaceDescription itf, final InterfaceVersionDescription version) {
        return this.generate("ConnectionHandler", itf, version);
    }

    /**
     * @param itf
     * @param version
     * @return
     */
    public String generateConnectionHandlerImplementation(final InterfaceDescription itf,
            final InterfaceVersionDescription version) {
        return this.generate("ConnectionHandlerImpl", itf, version);
    }

    /**
     * @param itf
     * @param version
     * @return
     */
    public String generateFactory(final InterfaceDescription itf, final InterfaceVersionDescription version) {
        return this.generate("Factory", itf, version);
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
            throws JsonProcessingException {
        final Set<InterfaceDescription> input = service.getInterfaces();

        final Set<Interface> serviceInterfaces = new HashSet<>();
        for (final InterfaceDescription descr : input) {
            final List<InterfaceVersion> versionList = new ArrayList<>();
            for (final InterfaceVersionDescription ivd : descr.getInterfaceVersions()) {
                final String sendHash = this.getHash(descr, ivd, ivd.getSends());
                final String recvHash = this.getHash(descr, ivd, ivd.getReceives());
                versionList.add(new InterfaceVersion(ivd.getVersionName(), recvHash, sendHash));
            }
            serviceInterfaces
                    .add(new Interface(descr.getName(), versionList, descr.isAllowMultiple(), descr.isAutoConnect()));
        }

        final Map<String, String> replace = new HashMap<>();
        if (platform.equals("x86")) {
            replace.put("from", "java:alpine");
        } else {
            replace.put("from", "larmog/armhf-alpine-java:jdk-8u73");
        }

        replace.put("service.name", service.getName());
        final ObjectWriter writer = Templates.PRETTY_PRINT_JSON ? this.mapper.writerWithDefaultPrettyPrinter()
                : this.mapper.writer();
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
            final InterfaceVersionDescription version) {
        final String template = this.getTemplate(templateName);

        final Map<String, String> replaceMap = new HashMap<>();

        // Generic stuff that is the same everywhere
        replaceMap.put("package", this.servicePackage);
        replaceMap.put("username", System.getProperty("user.name"));
        replaceMap.put("date", DateFormat.getDateTimeInstance().format(new Date()));
        replaceMap.put("generator", Templates.class.getPackage().getName().toString());

        replaceMap.put("service.class", PluginUtils.serviceImplClass(this.serviceDescription));
        replaceMap.put("service.version", this.serviceDescription.getVersion());
        replaceMap.put("service.name", this.serviceDescription.getName());

        if ((itf != null) && (version != null)) {
            final String versionedName = PluginUtils.getVersionedName(itf, version);
            final String packageName = versionedName.toLowerCase();

            replaceMap.put("handler.class", PluginUtils.connectionHandlerClass(itf, version));
            replaceMap.put("handlerImpl.class", PluginUtils.connectionHandlerImplClass(itf, version));
            replaceMap.put("factory.class", PluginUtils.factoryClass(itf, version));

            replaceMap.put("itf.name", itf.getName());
            replaceMap.put("itf.version", version.getVersionName());
            replaceMap.put("itf.packagename", packageName);
            replaceMap.put("itf.receivesHash", this.getHash(itf, version, version.getReceives()));
            replaceMap.put("itf.sendsHash", this.getHash(itf, version, version.getSends()));

            final Set<String> recvClasses = new HashSet<>();
            for (final String type : version.getReceives()) {
                recvClasses.add(type + ".class");
            }
            replaceMap.put("itf.receiveClasses", String.join(", ", recvClasses));

            final Set<String> sendClasses = new HashSet<>();
            for (final String type : version.getSends()) {
                sendClasses.add(type + ".class");
            }
            replaceMap.put("itf.sendClasses", String.join("., ", sendClasses));

            // Add handler definitions and implementations for the connection handlers (and implementations
            // respectively)
            String handlers = "";
            String handlerImpls = "";
            for (final String type : version.getReceives()) {
                final Map<String, String> handlerReplace = new HashMap<>();
                handlerReplace.put("handle.type", type);
                handlers += Templates.replaceMap(this.getTemplate("Handler"), handlerReplace);
                handlerImpls += Templates.replaceMap(this.getTemplate("HandlerImpl"), handlerReplace);
            }
            replaceMap.put("handlers", handlers);
            replaceMap.put("handlerImpls", handlerImpls);

            // Add imports for the handlers
            final Set<String> imports = new HashSet<>();
            if (version.getType().equals(Type.PROTO)) {
                replaceMap.put("itf.serializer", "ProtobufMessageSerializer");

                for (final String type : version.getReceives()) {
                    imports.add(String.format("import %s.%s.%s.%sProto.%s;",
                            this.servicePackage,
                            packageName,
                            this.protobufOutputPackage,
                            versionedName,
                            type));
                }
            } else if (version.getType().equals(Type.XSD)) {
                replaceMap.put("itf.serializer", "XSDMessageSerializer");

                for (final String type : version.getReceives()) {
                    imports.add(String.format("import %s.%s.%s.*;",
                            this.servicePackage,
                            packageName,
                            this.xsdOutputPackage,
                            type));
                }
            }

            replaceMap.put("handler.imports", String.join("\n", imports));
        }

        return Templates.replaceMap(template, replaceMap);
    }

    /**
     * Find a template file
     *
     * @param name
     * @return
     */
    private String getTemplate(final String name) {
        String result = "";
        try {
            final URL url = this.getClass().getClassLoader().getResource("templates/" + name + ".tpl");
            try (final Scanner scanner = new Scanner(url.openStream())) {
                result = scanner.useDelimiter("\\A").next();
            }
        } catch (final IOException e) {
            e.printStackTrace();
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

    String getHash(final InterfaceDescription itf, final InterfaceVersionDescription vitf, final Set<String> set) {
        final String versionedName = PluginUtils.getVersionedName(itf, vitf);
        if (this.hashes.containsKey(versionedName)) {
            String baseHash = this.hashes.get(versionedName);
            for (final String key : set) {
                baseHash += ";" + key;
            }
            return PluginUtils.SHA256(baseHash);
        }
        throw new RuntimeException("Could not get hash for " + versionedName);
    }

}
