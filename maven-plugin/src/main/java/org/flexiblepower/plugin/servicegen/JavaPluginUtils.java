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

    /**
     * Get the target java package name from a interface description
     *
     * @param itf The interface description
     * @return The endpoint package name for the interface description (i.e. not fully qualified)
     */
    static String getPackageName(final InterfaceDescription itf) {
        return JavaPluginUtils.toPackageName(itf.getName());
    }

    /**
     * Get the target java package name from a interface version description
     *
     * @param vitf The interface version description
     * @return The endpoint package name for the interface version description (i.e. not fully qualified)
     */
    static String getPackageName(final InterfaceVersionDescription vitf) {
        return JavaPluginUtils.toPackageName(vitf.getVersionName());
    }

    /**
     * Get the java package name from a interface and its version description. This is just a shortcut to:
     *
     * <pre>
     * {@link #getPackageName(InterfaceDescription)} + "." + {@link #getPackageName(InterfaceVersionDescription)}
     * </pre>
     *
     * @param itf The interface description
     * @param vitf The interface version description
     * @return The package name for the combined interface and version description.
     */
    static String getPackageName(final InterfaceDescription itf, final InterfaceVersionDescription vitf) {
        return JavaPluginUtils.getPackageName(itf) + "." + JavaPluginUtils.getPackageName(vitf);
    }

    /**
     * Get the name of the version of the interface
     *
     * @param vitf The interface version description
     * @return The name of the version to use in the code generator
     */
    static String getVersion(final InterfaceVersionDescription vitf) {
        return PluginUtils.camelCaps(vitf.getVersionName());
    }

    /**
     * Get the fully qualified name of the version of the interface, including the name of the interface
     *
     * @param itf The interface description
     * @param vitf The interface version description
     * @return The fully qualified name of the interface version
     */
    static String getVersionedName(final InterfaceDescription itf, final InterfaceVersionDescription vitf) {
        return PluginUtils.camelCaps(itf.getName() + "_" + vitf.getVersionName());
    }

    /**
     * Get the java service class name that implements the service
     *
     * @param d The service description from the user service.json file
     * @return A String containing the name of the java class that implements the service
     */
    static String serviceImplClass(final ServiceDescription d) {
        return PluginUtils.camelCaps(d.getName()) + JavaPluginUtils.SERVICE_SUFFIX;
    }

    /**
     * Get the java interface name that defines the configuration of the service
     *
     * @param d The service description from the user service.json file
     * @return A String containing the name of the java interface that defines the service configuration
     */
    static String configInterfaceClass(final ServiceDescription d) {
        return PluginUtils.camelCaps(d.getName()) + JavaPluginUtils.CONFIG_SUFFIX;
    }

    /**
     * Get the (java) interface name of the connection handler for the specific version of the (def-pi) interface
     *
     * @param itf The interface description
     * @param vitf The interface version description
     * @return The type name of the java interface that specifies the ConnectionHandler interface
     */
    static String connectionHandlerInterface(final InterfaceDescription itf, final InterfaceVersionDescription vitf) {
        return PluginUtils.camelCaps(itf.getName() + "_" + vitf.getVersionName()) + JavaPluginUtils.HANDLER_SUFFIX;
    }

    /**
     * Get the class name of the connection handler for the specific version of the (def-pi) interface
     *
     * @param itf The interface description
     * @param vitf The interface version description
     * @return The type name of the java class that implements the ConnectionHandler interface
     */
    static String connectionHandlerClass(final InterfaceDescription itf, final InterfaceVersionDescription vitf) {
        return PluginUtils.camelCaps(itf.getName() + "_" + vitf.getVersionName()) + JavaPluginUtils.HANDLER_IMPL_SUFFIX;
    }

    /**
     * Get the (java) interface name of the connection manager for the (def-pi) interface
     *
     * @param itf The interface description
     * @return The type name of the java interface that specifies the ConnectionManager interface
     */
    static String managerInterface(final InterfaceDescription itf) {
        return PluginUtils.camelCaps(itf.getName()) + JavaPluginUtils.MANAGER_SUFFIX;
    }

    /**
     * Get the class name of the connection manager for the (def-pi) interface
     *
     * @param itf The interface description
     * @return The type name of the java class that implements the ConnectionManager interface
     */
    static String managerClass(final InterfaceDescription itf) {
        return PluginUtils.camelCaps(itf.getName()) + JavaPluginUtils.MANAGER_IMPL_SUFFIX;
    }

    /**
     * Get the identifier for the service parameter
     *
     * @param param The parameter to get the id from
     * @return A String with the parameter ID to use in the configuration class
     */
    static String getParameterId(final Parameter param) {
        return Character.toUpperCase(param.getId().charAt(0)) + param.getId().substring(1);
    }

    /**
     * Create a valid SINGLE package name out of any string. i.e. it will also remove all intermediate points
     *
     * @param str The input string to create a package name from
     * @return A valid java package name
     * @see <a href=
     *      "http://docs.oracle.com/javase/tutorial/java/package/namingpkgs.html">
     *      http://docs.oracle.com/javase/tutorial/java/package/namingpkgs.html</a>
     */
    static String toPackageName(final String str) {
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
