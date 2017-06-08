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
    private static final String HANDLER_SUFFIX = "ConnectionHandler";
    private static final String HANDLER_IMPL_SUFFIX = "ConnectionHandlerImpl";
    private static final String HANDLER_FACTORY_SUFFIX = "ConnectionHandlerFactory";

    public static String getVersionedName(final InterfaceDescription iface,
            final InterfaceVersionDescription versionDescription) {
        return PluginUtils.camelCaps(iface.getName() + versionDescription.getVersionName());
    }

    public static String serviceImplClass(final ServiceDescription d) {
        return PluginUtils.camelCaps(d.getName()) + PluginUtils.SERVICE_SUFFIX;
    }

    public static String connectionHandlerClass(final InterfaceDescription itf,
            final InterfaceVersionDescription version) {
        return PluginUtils.camelCaps(itf.getName() + "_" + version.getVersionName()) + PluginUtils.HANDLER_SUFFIX;
    }

    public static String connectionHandlerImplClass(final InterfaceDescription itf,
            final InterfaceVersionDescription version) {
        return PluginUtils.camelCaps(itf.getName() + "_" + version.getVersionName()) + PluginUtils.HANDLER_IMPL_SUFFIX;
    }

    public static String factoryClass(final InterfaceDescription itf, final InterfaceVersionDescription version) {
        return PluginUtils.camelCaps(itf.getName() + "_" + version.getVersionName())
                + PluginUtils.HANDLER_FACTORY_SUFFIX;
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

        return ret.toString().replace(".", "");
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
