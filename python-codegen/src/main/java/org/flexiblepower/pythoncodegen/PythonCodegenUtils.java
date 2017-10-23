/**
 * File PluginUtils.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.pythoncodegen;

import org.flexiblepower.codegen.PluginUtils;
import org.flexiblepower.codegen.model.InterfaceDescription;
import org.flexiblepower.codegen.model.InterfaceVersionDescription;
import org.flexiblepower.codegen.model.ServiceDescription;

/**
 * PluginUtils
 *
 * @author coenvl
 * @version 0.1
 * @since Jun 8, 2017
 */
public class PythonCodegenUtils {

    private static final String SERVICE_SUFFIX = "";
    private static final String HANDLER_SUFFIX = "ConnectionHandler";
    private static final String HANDLER_IMPL_SUFFIX = "ConnectionHandlerImpl";
    private static final String MANAGER_SUFFIX = "ConnectionManager";
    private static final String MANAGER_IMPL_SUFFIX = "ConnectionManagerImpl";

    public static String getVersion(final InterfaceVersionDescription vitf) {
        return PluginUtils.snakeCaps(vitf.getVersionName());
    }

    public static String getVersionedName(final InterfaceDescription itf, final InterfaceVersionDescription vitf) {
        return PluginUtils.snakeCaps(itf.getName() + "_" + vitf.getVersionName());
    }

    public static String serviceImplClass(final ServiceDescription d) {
        return PluginUtils.camelCaps(d.getName()) + PythonCodegenUtils.SERVICE_SUFFIX;
    }

    public static String connectionHandlerInterface(final InterfaceDescription itf,
            final InterfaceVersionDescription version) {
        return PluginUtils.camelCaps(itf.getName() + "_" + version.getVersionName())
                + PythonCodegenUtils.HANDLER_SUFFIX;
    }

    public static String connectionHandlerClass(final InterfaceDescription itf,
            final InterfaceVersionDescription version) {
        return PluginUtils.camelCaps(itf.getName() + "_" + version.getVersionName())
                + PythonCodegenUtils.HANDLER_IMPL_SUFFIX;
    }

    public static String managerInterface(final InterfaceDescription itf) {
        return PluginUtils.camelCaps(itf.getName()) + PythonCodegenUtils.MANAGER_SUFFIX;
    }

    public static String managerClass(final InterfaceDescription itf) {
        return PluginUtils.camelCaps(itf.getName()) + PythonCodegenUtils.MANAGER_IMPL_SUFFIX;
    }

}
