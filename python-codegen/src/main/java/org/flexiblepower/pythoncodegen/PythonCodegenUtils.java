/**
 * File PythonCodegenUtils.java
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

    public static String getVersion(final InterfaceVersionDescription vitf) {
        return PythonCodegenUtils.VERSION_PREFIX + PluginUtils.camelCaps(vitf.getVersionName());
    }

    public static String getVersionedName(final InterfaceDescription itf, final InterfaceVersionDescription vitf) {
        return PluginUtils.camelCaps(itf.getName() + " " + vitf.getVersionName());
    }

    public static String serviceImplClass(final ServiceDescription d) {
        return PluginUtils.camelCaps(d.getName()) + PythonCodegenUtils.SERVICE_SUFFIX;
    }

    public static String connectionHandlerInterface(final InterfaceDescription itf,
            final InterfaceVersionDescription version) {
        return PluginUtils.camelCaps(itf.getName() + " " + version.getVersionName())
                + PythonCodegenUtils.HANDLER_SUFFIX;
    }

    public static String connectionHandlerClass(final InterfaceDescription itf,
            final InterfaceVersionDescription version) {
        return PluginUtils.camelCaps(itf.getName() + " " + version.getVersionName())
                + PythonCodegenUtils.HANDLER_IMPL_SUFFIX;
    }

    public static String managerInterface(final InterfaceDescription itf) {
        return PluginUtils.camelCaps(itf.getName()) + PythonCodegenUtils.MANAGER_SUFFIX;
    }

    public static String managerClass(final InterfaceDescription itf) {
        return PluginUtils.camelCaps(itf.getName()) + PythonCodegenUtils.MANAGER_IMPL_SUFFIX;
    }

    public static String builderFunctionName(final InterfaceVersionDescription vitf) {
        return PythonCodegenUtils.BUILDER_FUNCTION_PREFIX + PluginUtils.snakeCaps(vitf.getVersionName());
    }

    public static String typeHandlerFunction(final String type) {
        return PythonCodegenUtils.HANDLER_FUNCTION_PREFIX + type;
    }

    public static String getInterfacePackage(final InterfaceDescription itf) {
        return PluginUtils.camelCaps(itf.getName());
    }

}
