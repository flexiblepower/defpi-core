/*-
 * #%L
 * dEF-Pi python service creation
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
package org.flexiblepower.pythoncodegen;

import org.flexiblepower.codegen.PluginUtils;
import org.flexiblepower.codegen.model.InterfaceDescription;
import org.flexiblepower.codegen.model.InterfaceVersionDescription;
import org.flexiblepower.codegen.model.ServiceDescription;

/**
 * PluginUtils
 *
 * @version 0.1
 * @since Jun 8, 2017
 */
public class PythonCodegenUtils {

    private static final String SERVICE_SUFFIX = "";
    private static final String HANDLER_SUFFIX = "ConnectionHandler";
    private static final String HANDLER_IMPL_SUFFIX = "ConnectionHandlerImpl";
    private static final String MANAGER_SUFFIX = "ConnectionManager";
    private static final String MANAGER_IMPL_SUFFIX = "ConnectionManagerImpl";
    private static final String BUILDER_FUNCTION_PREFIX = "build";
    private static final String HANDLER_FUNCTION_PREFIX = "handle";
    private static final String VERSION_PREFIX = "v";

    /**
     * Get the name of the version of the interface
     *
     * @param vitf The interface version description
     * @return The name of the version to use in the code generator
     */
    public static String getVersion(final InterfaceVersionDescription vitf) {
        return PythonCodegenUtils.VERSION_PREFIX + PluginUtils.camelCaps(vitf.getVersionName());
    }

    /**
     * Get the fully qualified name of the version of the interface, including the name of the interface
     *
     * @param itf The interface description
     * @param vitf The interface version description
     * @return The fully qualified name of the interface version
     */
    public static String getVersionedName(final InterfaceDescription itf, final InterfaceVersionDescription vitf) {
        return PluginUtils.camelCaps(itf.getName() + " " + vitf.getVersionName());
    }

    /**
     * Get the python service class name that implements the service
     *
     * @param d The service description from the user service.json file
     * @return A String containing the name of the java class that implements the service
     */
    public static String serviceImplClass(final ServiceDescription d) {
        return PluginUtils.camelCaps(d.getName()) + PythonCodegenUtils.SERVICE_SUFFIX;
    }

    /**
     * Get the abstract base class name of the connection handler for the specific version of the (def-pi) interface
     *
     * @param itf The interface description
     * @param version The interface version description
     * @return The type name of the abstract base class that specifies the ConnectionHandler interface
     */
    public static String connectionHandlerInterface(final InterfaceDescription itf,
            final InterfaceVersionDescription version) {
        return PluginUtils.camelCaps(itf.getName() + " " + version.getVersionName())
                + PythonCodegenUtils.HANDLER_SUFFIX;
    }

    /**
     * Get the class name of the connection handler for the specific version of the (def-pi) interface
     *
     * @param itf The interface description
     * @param version The interface version description
     * @return The type name of the python class that implements the ConnectionHandler interface
     */
    public static String connectionHandlerClass(final InterfaceDescription itf,
            final InterfaceVersionDescription version) {
        return PluginUtils.camelCaps(itf.getName() + " " + version.getVersionName())
                + PythonCodegenUtils.HANDLER_IMPL_SUFFIX;
    }

    /**
     * Get the abstract base class name of the connection manager for the (def-pi) interface
     *
     * @param itf The interface description
     * @return The type name of the abstract base class that specifies the ConnectionManager interface
     */
    public static String managerInterface(final InterfaceDescription itf) {
        return PluginUtils.camelCaps(itf.getName()) + PythonCodegenUtils.MANAGER_SUFFIX;
    }

    /**
     * Get the class name of the connection manager for the (def-pi) interface
     *
     * @param itf The interface description
     * @return The type name of the python class that implements the ConnectionManager interface
     */
    public static String managerClass(final InterfaceDescription itf) {
        return PluginUtils.camelCaps(itf.getName()) + PythonCodegenUtils.MANAGER_IMPL_SUFFIX;
    }

    /**
     * Get the name of the function that builds a specific version of a def-pi interface
     *
     * @param vitf The interface description
     * @return The function name of the builder
     */
    public static String builderFunctionName(final InterfaceVersionDescription vitf) {
        return PythonCodegenUtils.BUILDER_FUNCTION_PREFIX + PluginUtils.snakeCaps(vitf.getVersionName());
    }

    /**
     * Get the name of the function that handles a message of a specific type
     *
     * @param type A message type
     * @return The function name of the handler
     */
    public static String typeHandlerFunction(final String type) {
        return PythonCodegenUtils.HANDLER_FUNCTION_PREFIX + type;
    }

    /**
     * Get the python package where the interfaces are placed in
     *
     * @param itf A def-pi interface
     * @return The name of the package where the files belonging to this interface can be found
     */
    public static String getInterfacePackage(final InterfaceDescription itf) {
        return PluginUtils.camelCaps(itf.getName());
    }

}
