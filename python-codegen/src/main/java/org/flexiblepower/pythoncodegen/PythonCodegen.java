/**
 * File PythonCodegen.java
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import org.flexiblepower.codegen.PluginUtils;
import org.flexiblepower.codegen.model.InterfaceDescription;
import org.flexiblepower.codegen.model.InterfaceVersionDescription;
import org.flexiblepower.codegen.model.InterfaceVersionDescription.Type;
import org.flexiblepower.codegen.model.ServiceDescription;
import org.flexiblepower.pythoncodegen.compiler.ProtoCompiler;
import org.flexiblepower.pythoncodegen.compiler.PyXBCompiler;

import lombok.extern.slf4j.Slf4j;

/**
 * CreateComponentMojo
 *
 * @version 0.1
 * @since Jun 28, 2017
 */
@Slf4j
public class PythonCodegen {

    /**
     *
     */
    private static final String DEFAULT_SERVICE_FILE = "service.json";
    public static final String RESOURCE_LOCATION = "resources";
    public static final String SOURCE_LOCATION = "service";
    public static final String PROTOBUF_VERSION = "3.3.0";
    public static final String PACKAGE_DECLARATION = "__init__.py";
    public static final String MODULE_DECLARATION = "__main__.py";

    // In what subfolders to put all resources
    public static final String PROTO_INPUT_LOCATION = "";
    public static final String PROTO_OUTPUT_LOCATION = "proto";
    public static final String XSD_INPUT_LOCATION = "";
    public static final String XSD_OUTPUT_LOCATION = "xsd";

    public static final String MODEL_SOURCE_LOCATION = "model";

    // private final String xsdInputLocation = "xsd";
    private final String dockerLocation = "docker";
    private final String dockerArmLocation = "docker-arm";

    private final String dockerEntryPoint = "python -m service";
    public static final String REQUIREMENTS_LOCATION = "";

    private final Map<String, InterfaceVersionDescription> hashes = new HashMap<>();

    private ProtoCompiler protoCompiler;

    private Path resourcePath;
    private PythonTemplates templates;
    private PyXBCompiler pyXbCompiler;

    /**
     * Main function of the Maven plugin, will call the several stages of the
     * plugin.
     */
    public void run() throws Exception {
        // this.resourcePath = Paths.get(this.resourceLocation);
        final File serviceDescriptionFile = Paths.get(PythonCodegen.DEFAULT_SERVICE_FILE).toFile();
        if (!PluginUtils.validateServiceDefinition(serviceDescriptionFile)) {
            throw new Exception("Invalid service description, see message log");
        }

        final ServiceDescription service = PluginUtils.readServiceDefinition(serviceDescriptionFile);
        service.setId(PluginUtils.camelCaps(service.getName()));

        this.resourcePath = Paths.get(PythonCodegen.RESOURCE_LOCATION);
        Files.createDirectories(this.resourcePath);

        final Path pythonSourceFolder = Paths.get(PythonCodegen.SOURCE_LOCATION);
        Files.createDirectories(pythonSourceFolder);

        // Add descriptors and related hashes
        this.compileDescriptors(service);

        // Add templates to generate java code and the dockerfile
        this.templates = new PythonTemplates(service);

        this.createPythonFiles(service, pythonSourceFolder);
        this.createDockerfiles(service);
        this.createRequirements();
    }

    /**
     * Create stubs for the service implementation. By using the templates in
     * the Template object.
     *
     * @throws IOException
     */
    private void createPythonFiles(final ServiceDescription serviceDescription, final Path dest) throws IOException {
        PythonCodegen.log.debug("Creating stubs");

        final String ext = ".py";
        Files.createDirectories(dest);

        final Path moduleMain = dest.resolve(PythonCodegen.MODULE_DECLARATION);
        if (moduleMain.toFile().exists()) {
            PythonCodegen.log.debug("Skipping existing file " + moduleMain.toString());
        } else {
            Files.write(moduleMain, this.templates.generateServiceMain().getBytes());
        }

        final Path serviceImpl = dest.resolve(PythonCodegenUtils.serviceImplClass(serviceDescription) + ext);
        if (serviceImpl.toFile().exists()) {
            PythonCodegen.log.debug("Skipping existing file " + serviceImpl.toString());
        } else {
            Files.write(serviceImpl, this.templates.generateServiceImplementation().getBytes());
        }

        for (final InterfaceDescription itf : serviceDescription.getInterfaces()) {
            final Path interfacePath = Files.createDirectories(dest.resolve(itf.getName()));
            if (Files.notExists(interfacePath.resolve(PythonCodegen.PACKAGE_DECLARATION))) {
                Files.createFile(interfacePath.resolve(PythonCodegen.PACKAGE_DECLARATION));
            }
            final Path manager = interfacePath.resolve(PythonCodegenUtils.managerInterface(itf) + ext);
            final Path managerImpl = interfacePath.resolve(PythonCodegenUtils.managerClass(itf) + ext);

            // Write interface files
            Files.write(manager, this.templates.generateManagerInterface(itf).getBytes());

            if (managerImpl.toFile().exists()) {
                PythonCodegen.log.debug("Skipping existing file " + managerImpl.toString());
            } else {
                Files.write(managerImpl, this.templates.generateManagerImplementation(itf).getBytes());
            }

            for (final InterfaceVersionDescription version : itf.getInterfaceVersions()) {
                final Path interfaceVersionPath = Files
                        .createDirectories(interfacePath.resolve(PythonCodegenUtils.getVersion(version)));
                if (Files.notExists(interfaceVersionPath.resolve(PythonCodegen.PACKAGE_DECLARATION))) {
                    Files.createFile(interfaceVersionPath.resolve(PythonCodegen.PACKAGE_DECLARATION));
                }
                // Create intermediate directories
                final Path connectionHandler = interfaceVersionPath
                        .resolve(PythonCodegenUtils.connectionHandlerInterface(itf, version) + ext);
                final Path connectionHandlerImpl = interfaceVersionPath
                        .resolve(PythonCodegenUtils.connectionHandlerClass(itf, version) + ext);

                // Write files
                Files.write(connectionHandler, this.templates.generateHandlerInterface(itf, version).getBytes());

                if (connectionHandlerImpl.toFile().exists()) {
                    PythonCodegen.log.debug("Skipping existing file " + connectionHandlerImpl.toString());
                } else {
                    Files.write(connectionHandlerImpl,
                            this.templates.generateHandlerImplementation(itf, version).getBytes());
                }

            }
        }
    }

    /**
     * Create the Dockerfile
     *
     * @param service
     *                    The current ServiceDescription object
     * @throws IOException
     * @throws HashComputeException
     */
    private void createDockerfiles(final ServiceDescription service) throws IOException {
        PythonCodegen.log.debug("Creating Dockerfiles");

        final Path dockerFolder = Files.createDirectories(this.resourcePath.resolve(this.dockerLocation));
        final Path dockerArmFolder = Files.createDirectories(this.resourcePath.resolve(this.dockerArmLocation));

        Files.write(dockerFolder.resolve("Dockerfile"),
                this.templates.generateDockerfile("x86", this.dockerEntryPoint).getBytes());

        Files.write(dockerArmFolder.resolve("Dockerfile"),
                this.templates.generateDockerfile("arm", this.dockerEntryPoint).getBytes());
    }

    private void createRequirements() throws IOException {
        final Path requirements = Paths.get(PythonCodegen.REQUIREMENTS_LOCATION).resolve("requirements.txt");
        if (requirements.toFile().exists()) {
            PythonCodegen.log.debug("Skipping existing file " + requirements.toString());
        } else {
            Files.write(requirements, this.templates.generateRequirements().getBytes());
        }
    }

    /**
     * @param service
     * @throws IOException
     */
    private void compileDescriptors(final ServiceDescription service) throws IOException {
        PythonCodegen.log.debug("Compiling descriptors definitions to java code");

        this.protoCompiler = new ProtoCompiler(PythonCodegen.PROTOBUF_VERSION);
        this.pyXbCompiler = new PyXBCompiler();

        for (final InterfaceDescription iface : service.getInterfaces()) {
            for (final InterfaceVersionDescription versionDescription : iface.getInterfaceVersions()) {

                if (versionDescription.getType().equals(Type.PROTO)) {
                    this.compileProtoDescriptor(iface, versionDescription);

                } else if (versionDescription.getType().equals(Type.XSD)) {
                    this.compileXSDDescriptor(iface, versionDescription);
                }
            }
        }
    }

    /**
     * @param itf
     * @param versionDescription
     * @throws IOException
     */
    private void compileXSDDescriptor(final InterfaceDescription itf, final InterfaceVersionDescription vitf)
            throws IOException {
        // First get the hash of the input file
        final Path xsdSourceFilePath = PluginUtils.downloadFileOrResolve(vitf.getLocation(),
                Paths.get(PythonCodegen.XSD_INPUT_LOCATION));

        // Compute hash and store in interface
        final String interfaceHash = PluginUtils.SHA256(xsdSourceFilePath);
        vitf.setHash(interfaceHash);

        if (this.hashes.containsKey(interfaceHash)) {
            vitf.setModelPackageName(this.hashes.get(interfaceHash).getModelPackageName());
            return;
        }

        // Get the package name and add the hash
        final String versionedName = PythonCodegenUtils.getVersionedName(itf, vitf);

        // Store for later reference
        vitf.setModelPackageName(PythonCodegen.SOURCE_LOCATION + "." + PythonCodegen.MODEL_SOURCE_LOCATION + "."
                + versionedName + ".xsd");
        this.hashes.put(interfaceHash, vitf);

        // Append additional compilation info to the proto file and compile the java code
        final Path xsdResourceFolder = Files
                .createDirectories(this.resourcePath.resolve(PythonCodegen.XSD_OUTPUT_LOCATION));
        final Path xsdDestFilePath = xsdResourceFolder.resolve(versionedName + ".xsd");

        // Copy the descriptor and start compilation
        Files.copy(xsdSourceFilePath, xsdDestFilePath, StandardCopyOption.REPLACE_EXISTING);

        this.pyXbCompiler.compile(xsdDestFilePath,
                Paths.get(PythonCodegen.SOURCE_LOCATION).resolve(PythonCodegen.MODEL_SOURCE_LOCATION));
    }

    /**
     * @param itf
     * @param vitf
     * @throws IOException
     */
    private void compileProtoDescriptor(final InterfaceDescription itf, final InterfaceVersionDescription vitf)
            throws IOException {
        final Path protoSourceFilePath = PluginUtils.downloadFileOrResolve(vitf.getLocation(),
                Paths.get(PythonCodegen.PROTO_INPUT_LOCATION));

        // Compute hash and store in interface
        final String interfaceHash = PluginUtils.SHA256(protoSourceFilePath);
        vitf.setHash(interfaceHash);

        if (this.hashes.containsKey(interfaceHash)) {
            // If we already have it, just copy the package name
            vitf.setModelPackageName(this.hashes.get(interfaceHash).getModelPackageName());
            return;
        }

        // Get the package name and add the hash
        final String packagePath = Paths.get(PythonCodegen.SOURCE_LOCATION)
                .resolve(PythonCodegen.MODEL_SOURCE_LOCATION)
                .toString();
        final String versionedName = PythonCodegenUtils.getVersionedName(itf, vitf);
        final String protoClassName = versionedName + "_pb2";

        // Store for later reference
        vitf.setModelPackageName(packagePath + "." + protoClassName);
        this.hashes.put(interfaceHash, vitf);

        // Append additional compilation info to the proto file and compile the java code
        final Path protoDestFilePath = Files
                .createDirectories(this.resourcePath.resolve(PythonCodegen.PROTO_OUTPUT_LOCATION))
                .resolve(versionedName + ".proto");
        Files.copy(protoSourceFilePath, protoDestFilePath, StandardCopyOption.REPLACE_EXISTING);

        try (final Scanner scanner = new Scanner(new FileInputStream(protoSourceFilePath.toFile()), "UTF-8")) {
            Files.write(protoDestFilePath,
                    ("syntax = \"proto2\";\n\npackage " + versionedName + ";\n" + scanner.useDelimiter("\\A").next())
                            .getBytes());
        }

        this.protoCompiler.compile(protoDestFilePath,
                Paths.get(PythonCodegen.SOURCE_LOCATION).resolve(PythonCodegen.MODEL_SOURCE_LOCATION));
    }

}