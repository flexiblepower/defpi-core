package org.flexiblepower.plugin.servicegen;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

public class Templates {

    private final String servicePackage;
    private final ServiceDescription serviceDescription;
    private final Map<String, String> hashes;
    private final ObjectMapper mapper = new ObjectMapper();

    public Templates(final String targetPackage, final ServiceDescription descr, final Map<String, String> hashes) {
        this.servicePackage = targetPackage;
        this.serviceDescription = descr;
        this.hashes = hashes;
    }

    public String generateServiceImplementation() {
        return Templates.replaceMap(this.getTemplate("ServiceImplementation"), this.getReplaceMap(null, null));
    }

    public String generateConnectionHandler(final InterfaceDescription itf, final InterfaceVersionDescription version) {
        return this.generate("ConnectionHandler", itf, version);
    }

    public String generateConnectionHandlerImplementation(final InterfaceDescription itf,
            final InterfaceVersionDescription version) {
        return Templates.replaceMap(this.getTemplate("ConnectionHandlerImpl"), this.getReplaceMap(itf, version));
    }

    public String generateFactory(final InterfaceDescription itf, final InterfaceVersionDescription version) {
        return Templates.replaceMap(this.getTemplate("Factory"), this.getReplaceMap(itf, version));
    }

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

        replace.put("name", service.getName());
        replace.put("interfaces", this.mapper.writeValueAsString(serviceInterfaces));

        return Templates.replaceMap(this.getTemplate("Dockerfile"), replace);
    }

    private String generate(final String templateName,
            final InterfaceDescription itf,
            final InterfaceVersionDescription version) {
        final String template = this.getTemplate(templateName);
        final Map<String, String> replaceMap = this.getReplaceMap(itf, version);

        return Templates.replaceMap(template, replaceMap);
    }

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

    private Map<String, String> getReplaceMap(final InterfaceDescription itf,
            final InterfaceVersionDescription version) {
        final Map<String, String> replace = new HashMap<>();

        // Generic stuff that is the same everywhere
        replace.put("package", this.servicePackage);
        replace.put("username", System.getProperty("user.name"));
        replace.put("date", DateFormat.getDateInstance().format(new Date()));
        replace.put("generator", Templates.class.getPackage().toString());

        replace.put("service.class", Templates.serviceImplClass(this.serviceDescription));
        replace.put("service.version", this.serviceDescription.getVersion());
        replace.put("service.name", this.serviceDescription.getName());

        if ((itf != null) && (version != null)) {
            final String versionedName = Templates.camelCaps(itf.getName() + version.getVersionName());
            replace.put("handler.class", Templates.connectionHandlerClass(itf, version));
            replace.put("handlerimpl.class", Templates.connectionHandlerImplClass(itf, version));
            replace.put("factory.class", Templates.factoryClass(itf, version));

            replace.put("itf.name", itf.getName());
            replace.put("itf.version", version.getVersionName());
            replace.put("itf.receivesHash", this.getHash(itf, version, version.getReceives()));
            replace.put("itf.receiveClasses", String.join(".class ", version.getReceives()));
            replace.put("itf.sendClasses", String.join(".class ", version.getSends()));

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
            replace.put("handlers", handlers);
            replace.put("handlerImpls", handlerImpls);

            // Add imports for the handlers
            String imports = "";
            if (version.getType().equals(Type.PROTO)) {
                replace.put("itf.serializer", "ProtobufMessageSerializer");

                for (final String type : version.getReceives()) {
                    imports += String.format("import %s.protobuf.%sProto.%s;\n",
                            this.servicePackage,
                            versionedName,
                            type);
                }
            } else if (version.getType().equals(Type.XSD)) {
                replace.put("itf.serializer", "XSDMessageSerializer");

                for (final String type : version.getReceives()) {
                    imports += String.format("import %s.xml.%s;\n", this.servicePackage, versionedName, type);
                }
            }
            replace.put("handler.imports", imports);
        }

        return replace;
    }

    private static String replaceMap(final String template, final Map<String, String> replace) {
        String ret = template;
        for (final Entry<String, String> entry : replace.entrySet()) {
            if (entry.getValue() != null) {
                ret = ret.replace("{{" + entry.getKey() + "}}", entry.getValue());
            }
        }
        return ret;
    }

    private String getHash(final InterfaceDescription itf,
            final InterfaceVersionDescription vitf,
            final Set<String> set) {
        final String versionedName = itf.getName() + vitf.getVersionName();
        if (this.hashes.containsKey(versionedName)) {
            String baseHash = this.hashes.get(itf);
            for (final String key : set) {
                baseHash += ";" + key;
            }
            return Templates.SHA256(baseHash);
        }
        return "";
    }

    public static String serviceImplClass(final ServiceDescription d) {
        return Templates.camelCaps(d.getName());
    }

    public static String connectionHandlerClass(final InterfaceDescription itf,
            final InterfaceVersionDescription version) {
        return Templates.camelCaps(itf.getName() + version.getVersionName() + "ConnectionHandler");
    }

    public static String connectionHandlerImplClass(final InterfaceDescription itf,
            final InterfaceVersionDescription version) {
        return Templates.camelCaps(itf.getName() + version.getVersionName() + "ConnectionHandlerImpl");
    }

    public static String factoryClass(final InterfaceDescription itf, final InterfaceVersionDescription version) {
        return Templates.camelCaps(itf.getName() + version.getVersionName() + "ConnectionHandlerFactory");
    }

    /**
     * @param i
     * @return
     */
    static String camelCaps(final String str) {
        final StringBuilder ret = new StringBuilder();

        for (final String word : str.split(" ")) {
            if (!word.isEmpty()) {
                ret.append(Character.toUpperCase(word.charAt(0)));
                ret.append(word.substring(1).toLowerCase());
            }
        }

        return ret.toString();
    }

    public static String SHA256(final String body) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");

            md.update(body.getBytes("UTF-8"));
            final byte[] mdbytes = md.digest();

            final StringBuffer sb = new StringBuffer();
            for (final byte mdbyte : mdbytes) {
                sb.append(Integer.toString((mdbyte & 0xff) + 0x100, 16).substring(1));
            }
            return sb.toString();
        } catch (final NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (final UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return "";
    }
}
