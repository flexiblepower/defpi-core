/**
 * File PluginUtils.java
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
package org.flexiblepower.plugin.servicegen;

import org.flexiblepower.codegen.PluginUtils;
import org.flexiblepower.codegen.model.InterfaceDescription;
import org.flexiblepower.codegen.model.InterfaceVersionDescription;
import org.flexiblepower.codegen.model.ServiceDescription;
import org.flexiblepower.model.Parameter;

/**
 * PluginUtils
 *
 * @version 0.1
 * @since Jun 8, 2017
 */
public class JavaPluginUtils {

    private static final String SERVICE_SUFFIX = "";
    private static final String CONFIG_SUFFIX = "Configuration";
    private static final String HANDLER_SUFFIX = "ConnectionHandler";
    private static final String HANDLER_IMPL_SUFFIX = "ConnectionHandlerImpl";
    private static final String MANAGER_SUFFIX = "ConnectionManager";
    private static final String MANAGER_IMPL_SUFFIX = "ConnectionManagerImpl";

    public static String getPackageName(final InterfaceDescription itf) {
        return JavaPluginUtils.toPackageName(itf.getName());
    }

    public static String getPackageName(final InterfaceVersionDescription vitf) {
        return JavaPluginUtils.toPackageName(vitf.getVersionName());
    }

    public static String getPackageName(final InterfaceDescription itf, final InterfaceVersionDescription vitf) {
        return JavaPluginUtils.getPackageName(itf) + "." + JavaPluginUtils.getPackageName(vitf);
    }

    public static String getVersion(final InterfaceVersionDescription vitf) {
        return PluginUtils.camelCaps(vitf.getVersionName());
    }

    public static String getVersionedName(final InterfaceDescription itf, final InterfaceVersionDescription vitf) {
        return PluginUtils.camelCaps(itf.getName() + "_" + vitf.getVersionName());
    }

    public static String serviceImplClass(final ServiceDescription d) {
        return PluginUtils.camelCaps(d.getName()) + JavaPluginUtils.SERVICE_SUFFIX;
    }

    public static String configInterfaceClass(final ServiceDescription d) {
        return PluginUtils.camelCaps(d.getName()) + JavaPluginUtils.CONFIG_SUFFIX;
    }

    public static String connectionHandlerInterface(final InterfaceDescription itf,
            final InterfaceVersionDescription version) {
        return PluginUtils.camelCaps(itf.getName() + "_" + version.getVersionName()) + JavaPluginUtils.HANDLER_SUFFIX;
    }

    public static String connectionHandlerClass(final InterfaceDescription itf,
            final InterfaceVersionDescription version) {
        return PluginUtils.camelCaps(itf.getName() + "_" + version.getVersionName())
                + JavaPluginUtils.HANDLER_IMPL_SUFFIX;
    }

    public static String managerInterface(final InterfaceDescription itf) {
        return PluginUtils.camelCaps(itf.getName()) + JavaPluginUtils.MANAGER_SUFFIX;
    }

    public static String managerClass(final InterfaceDescription itf) {
        return PluginUtils.camelCaps(itf.getName()) + JavaPluginUtils.MANAGER_IMPL_SUFFIX;
    }

    public static Object getParameterId(final Parameter param) {
        return Character.toUpperCase(param.getId().charAt(0)) + param.getId().substring(1);
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
}
