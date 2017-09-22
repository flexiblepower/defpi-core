/**
 * File PluginUtils.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.plugin.servicegen;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Set;

import org.flexiblepower.plugin.servicegen.model.InterfaceDescription;
import org.flexiblepower.plugin.servicegen.model.InterfaceVersionDescription;
import org.flexiblepower.plugin.servicegen.model.ServiceDescription;

/**
 * PluginUtils
 *
 * @author coenvl
 * @version 0.1
 * @since Jun 8, 2017
 */
public class PluginUtils {

    private static final String SERVICE_SUFFIX = "";
    private static final String CONFIG_SUFFIX = "Configuration";
    private static final String HANDLER_SUFFIX = "ConnectionHandler";
    private static final String HANDLER_IMPL_SUFFIX = "ConnectionHandlerImpl";
    private static final String MANAGER_SUFFIX = "ConnectionManager";
    private static final String MANAGER_IMPL_SUFFIX = "ConnectionManagerImpl";

    public static String getPackageName(final InterfaceDescription itf) {
        return PluginUtils.toPackageName(itf.getName());
    }

    public static String getPackageName(final InterfaceVersionDescription vitf) {
        return PluginUtils.toPackageName(vitf.getVersionName());
    }

    public static String getPackageName(final InterfaceDescription itf, final InterfaceVersionDescription vitf) {
        return PluginUtils.getPackageName(itf) + "." + PluginUtils.getPackageName(vitf);
    }

    public static String getVersion(final InterfaceVersionDescription vitf) {
        return PluginUtils.camelCaps(vitf.getVersionName());
    }

    public static String getVersionedName(final InterfaceDescription itf, final InterfaceVersionDescription vitf) {
        return PluginUtils.camelCaps(itf.getName() + "_" + vitf.getVersionName());
    }

    public static String serviceImplClass(final ServiceDescription d) {
        return PluginUtils.camelCaps(d.getName()) + PluginUtils.SERVICE_SUFFIX;
    }

    public static String configInterfaceClass(final ServiceDescription d) {
        return PluginUtils.camelCaps(d.getName()) + PluginUtils.CONFIG_SUFFIX;
    }

    public static String connectionHandlerInterface(final InterfaceDescription itf,
            final InterfaceVersionDescription version) {
        return PluginUtils.camelCaps(itf.getName() + "_" + version.getVersionName()) + PluginUtils.HANDLER_SUFFIX;
    }

    public static String connectionHandlerClass(final InterfaceDescription itf,
            final InterfaceVersionDescription version) {
        return PluginUtils.camelCaps(itf.getName() + "_" + version.getVersionName()) + PluginUtils.HANDLER_IMPL_SUFFIX;
    }

    public static String managerInterface(final InterfaceDescription itf) {
        return PluginUtils.camelCaps(itf.getName()) + PluginUtils.MANAGER_SUFFIX;
    }

    public static String managerClass(final InterfaceDescription itf) {
        return PluginUtils.camelCaps(itf.getName()) + PluginUtils.MANAGER_IMPL_SUFFIX;
    }

    /**
     * Create a valid SINGLE package name out of any string. It will also remove all points
     *
     * @param str
     * @return
     * @see {@link http://docs.oracle.com/javase/tutorial/java/package/namingpkgs.html}
     */
    public static String toPackageName(final String str) {
        String ret = str.toLowerCase(); // Make lowercase
        ret = ret.replaceAll("[- ]", "_"); // Replace spaces and hyphens by underscores
        ret = ret.replaceAll("[^a-z0-9_]", ""); // Remove any unexpected characters
        if ((ret.charAt(0) < 60) && (ret.charAt(0) > 45)) {
            // Add a leading underscore if package starts with digit or .
            ret = "_" + ret;
        }
        return ret;
    }

    /**
     * @param i
     * @return
     */
    private static String camelCaps(final String str) {
        final StringBuilder ret = new StringBuilder();

        for (final String word : str.split(" ")) {
            if (!word.isEmpty()) {
                ret.append(Character.toUpperCase(word.charAt(0)));
                ret.append(word.substring(1).toLowerCase());
            }
        }

        // Return a cleaned-up string
        return ret.toString().replaceAll("[^a-zA-Z0-9_]", "");
    }

    public static String getHash(final InterfaceVersionDescription vitf, final Set<String> messageSet) {
        String baseHash = vitf.getHash();
        for (final String key : messageSet) {
            baseHash += ";" + key;
        }
        return PluginUtils.SHA256(baseHash);
    }

    public static String SHA256(final String body) {
        return PluginUtils.SHA256(body.getBytes(StandardCharsets.UTF_8));
    }

    public static String SHA256(final Path path) throws IOException {
        return PluginUtils.SHA256(Files.readAllBytes(path));
    }

    private static String SHA256(final byte[] barr) {
        try {
            final MessageDigest md = MessageDigest.getInstance("SHA-256");
            final byte[] mdbytes = md.digest(barr);
            return String.format("%x", new BigInteger(1, mdbytes));
            // return Base64.getEncoder().encodeToString(mdbytes);
        } catch (final Exception e) {
            throw new RuntimeException("Error computing hash: " + e.getMessage());
        }
    }

}
