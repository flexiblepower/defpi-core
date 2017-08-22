package org.flexiblepower.plugin.servicegen;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.flexiblepower.plugin.servicegen.compiler.ProtoCompiler;
import org.flexiblepower.plugin.servicegen.compiler.XjcCompiler;
import org.flexiblepower.plugin.servicegen.model.InterfaceDescription;
import org.flexiblepower.plugin.servicegen.model.InterfaceVersionDescription;
import org.flexiblepower.plugin.servicegen.model.InterfaceVersionDescription.Type;
import org.flexiblepower.plugin.servicegen.model.ServiceDescription;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;

/**
 * CreateComponentMojo
 *
 * @author coenvl
 * @version 0.1
 * @since Jun 28, 2017
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class CreateComponentMojo extends AbstractMojo {

    /**
     * The groupId of the service to build
     */
    @Parameter(property = "project.groupId", required = true)
    private String groupId;

    /**
     * The artifactId of the service to build
     */
    @Parameter(property = "project.artifactId", required = true)
    private String artifactId;

    /**
     * Version of protobuf to use
     */
    @Parameter(property = "protobuf.version", required = true)
    private String protobufVersion;

    /**
     * Folder where the service.yml and descriptor/xsd files are located
     */
    @Parameter(property = "project.resourcedir", defaultValue = "${project.basedir}/src/main/resources")
    private String resourceLocation;

    /**
     * Main java source folder where all java sources should be put
     */
    @Parameter(property = "project.sourcedir", defaultValue = "${project.basedir}/src/main/java")
    private String sourceLocation;

    /**
     * Service definition file
     */
    @Parameter(property = "defpi.service.description", defaultValue = "service.json")
    private String serviceFilename;

    /**
     * Main package of the service
     */
    @Parameter(property = "defpi.service.package", defaultValue = "${project.groupId}.${project.artifactId}")
    private String servicePackage;

    /**
     * Folder where the protobuf sources can be found
     */
    @Parameter(property = "protobuf.input.directory", defaultValue = "${project.basedir}/src/main/resources")
    private String protobufInputLocation;

    /**
     * Folder where the protobuf resources should be put
     */
    @Parameter(property = "protobuf.resource.directory",
               defaultValue = "${project.basedir}/src/main/resources/protobuf")
    private String protobufResourceLocation;

    /**
     * Package where the protobuf sources should be placed
     */
    @Parameter(property = "protobuf.output.package", defaultValue = "proto")
    private String protobufOutputPackage;

    /**
     * Folder where the XSD sources can be found
     */
    @Parameter(property = "xsd.input.directory", defaultValue = "${project.basedir}/src/main/resources")
    private String xsdInputLocation;

    /**
     * Folder where the XSD resources should be put
     */
    @Parameter(property = "xsd.resource.directory", defaultValue = "${project.basedir}/src/main/resources/xsd")
    private String xsdResourceLocation;

    /**
     * Folder where the XSD definitions should be copied to
     */
    @Parameter(property = "xsd.output.package", defaultValue = "xml")
    private String xsdOutputPackage;

    /**
     * Folder where the protobuf definitions should be copied to
     */
    private final String dockerLocation = "docker";

    /**
     * Folder where the protobuf definitions should be copied to
     */
    private final String dockerArmLocation = "docker-arm";

    private final Map<String, String> hashes = new HashMap<>();
    private Path resourcePath;
    private Templates templates;

    /**
     * Main function of the Maven plugin, will call the several stages of the
     * plugin.
     */
    @Override
    public void execute() throws MojoExecutionException {
        try {
            if (!this.servicePackage.matches("[a-z][a-z0-9_.]+")) {
                this.getLog().warn("Invalid java package name " + this.servicePackage);
                this.servicePackage = this.servicePackage.replaceAll("[^a-zA-Z0-9_.]", "").toLowerCase();
                this.getLog().warn("New target package name " + this.servicePackage);
            }

            this.resourcePath = Paths.get(this.resourceLocation);
            final File serviceDescriptionFile = this.resourcePath.resolve(this.serviceFilename).toFile();
            if (!this.validateServiceDefinition(serviceDescriptionFile)) {
                throw new MojoExecutionException("Invalid service description, see message log");
            }

            final ServiceDescription service = this.readServiceDefinition(serviceDescriptionFile);

            final Path javaSourceFolder = Paths.get(this.sourceLocation).resolve(this.servicePackage.replace('.', '/'));
            Files.createDirectories(javaSourceFolder);

            // Add descriptors and related hashes
            this.createDescriptors(service.getInterfaces());
            this.templates = new Templates(this.servicePackage,
                    this.protobufOutputPackage,
                    this.xsdOutputPackage,
                    service,
                    this.hashes);

            this.createJavaFiles(service, javaSourceFolder);
            this.createDockerfiles(service);
            this.compileDescriptors(service);

        } catch (final Exception e) {
            this.getLog().debug(e);
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    /**
     * Reads and parses the service json into a ServiceDescription object
     *
     * @return ServiceDescription object containing the data of the json
     * @throws ProcessingException
     * @throws IOException
     * @throws FileNotFoundException
     *             service.yml is not found in the resource directory
     * @throws JsonParseException
     *             file could not be parsed as json file
     * @throws JsonMappingException
     *             Json could not be mapped to a ServiceDescription
     */
    private ServiceDescription readServiceDefinition(final File inputFile) throws ProcessingException, IOException {
        this.getLog().info(String.format("Reading service definition from %s", inputFile));

        final ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(inputFile, ServiceDescription.class);
    }

    /**
     * @param inputFile
     * @throws IOException
     * @throws ProcessingException
     */
    public boolean validateServiceDefinition(final File inputFile) throws ProcessingException {
        final JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
        final URL schemaURL = this.getClass().getClassLoader().getResource("schema.json");

        try {
            final JsonNode schemaNode = JsonLoader.fromURL(schemaURL);
            final JsonNode data = JsonLoader.fromFile(inputFile);

            final JsonSchema schema = factory.getJsonSchema(schemaNode);
            final ProcessingReport report = schema.validate(data);

            if (report.isSuccess()) {
                return true;
            } else {
                this.getLog().warn("Errors while reading " + inputFile + ":");
                for (final ProcessingMessage m : report) {
                    this.getLog().warn(m.getMessage());
                }
            }
        } catch (final JsonParseException e) {
            this.getLog().warn("Invalid JSON syntax: " + e.getMessage());
        } catch (final IOException e) {
            this.getLog().warn("IOException while reading file: " + e.getMessage());
        }

        return false;

    }

    /**
     * Creates altered descriptor files that can be compiled into java source code.
     *
     * @param descriptors
     *            Map of descriptors
     * @throws IOException
     */
    private void createDescriptors(final Set<InterfaceDescription> interfaces) throws IOException {
        for (final InterfaceDescription iface : interfaces) {
            for (final InterfaceVersionDescription versionDescription : iface.getInterfaceVersions()) {
                final String fullName = PluginUtils.getVersionedName(iface, versionDescription);

                if (versionDescription.getType().equals(Type.PROTO)) {
                    final Path protoSourceFilePath = this.resourcePath.resolve(this.protobufInputLocation)
                            .resolve(versionDescription.getLocation());
                    final Path outputPath = Files
                            .createDirectories(this.resourcePath.resolve(this.protobufResourceLocation))
                            .resolve(fullName + ".proto");

                    // Append the descriptor and store hash of source
                    this.appendProtoDescriptor(iface, versionDescription, protoSourceFilePath, outputPath);
                    this.hashes.put(fullName, PluginUtils.SHA256(protoSourceFilePath));
                } else if (versionDescription.getType().equals(Type.XSD)) {
                    final Path xsdSourceFilePath = this.resourcePath.resolve(this.xsdInputLocation)
                            .resolve(versionDescription.getLocation());
                    final Path xsdResourceFolder = Files
                            .createDirectories(this.resourcePath.resolve(this.xsdResourceLocation));
                    final Path outputPath = xsdResourceFolder.resolve(fullName + ".xsd");

                    // Copy the descriptor and store hash of source
                    Files.copy(xsdSourceFilePath, outputPath, StandardCopyOption.REPLACE_EXISTING);
                    this.hashes.put(fullName, PluginUtils.SHA256(xsdSourceFilePath));
                } else {
                    throw new IOException("Unknown descriptor file type: " + versionDescription.getType());
                }

            }
        }

        this.getLog().info("Generated descriptors: " + Arrays.toString(this.hashes.entrySet().toArray()));
    }

    /**
     * Appends the original descriptor with information used by the descriptorc compiler. The java package and outer
     * classname have to be set, and also the specific syntax (descriptor2 or descriptor3) is set by this method.
     *
     * @param name
     *            Name of the descriptor
     * @param descriptor
     *            Descriptor object indicating which file has to be used
     * @param inputPath
     *            Path of the original descriptor file
     * @param outputPath
     *            Folder the altered descriptor file should be stored.
     * @throws IOException
     */
    private void appendProtoDescriptor(final InterfaceDescription itf,
            final InterfaceVersionDescription vitf,
            final Path inputPath,
            final Path outputPath) throws IOException {
        Files.copy(inputPath, outputPath, StandardCopyOption.REPLACE_EXISTING);

        String packageName = this.servicePackage + "." + PluginUtils.getPackageName(itf, vitf);
        final String versionedName = PluginUtils.getVersionedName(itf, vitf);
        if ((this.protobufOutputPackage != null) && !this.protobufOutputPackage.isEmpty()) {
            packageName = packageName + "." + this.protobufOutputPackage;
        }

        try (final Scanner scanner = new Scanner(new FileInputStream(inputPath.toFile()), "UTF-8")) {
            Files.write(outputPath,
                    ("syntax = \"proto2\";" + "\n\n" + "option java_package = \"" + packageName + "\";\n"
                            + "option java_outer_classname = \"" + versionedName + "Proto\";\n\n" + "package "
                            + packageName + ";\n" + scanner.useDelimiter("\\A").next()).getBytes(),
                    StandardOpenOption.CREATE);
        }

    }

    /**
     * Create stubs for the service implementation. By using the templates in
     * the Template object.
     *
     * @param interfaces
     *            List of interfaces for which stubs should be created.
     * @throws IOException
     * @throws HashComputeException
     */
    private void createJavaFiles(final ServiceDescription serviceDescription, final Path dest) throws IOException {
        this.getLog().debug("Creating stubs");

        final String ext = ".java";
        Files.createDirectories(dest);
        final Path serviceImpl = dest.resolve(PluginUtils.serviceImplClass(serviceDescription) + ext);
        if (serviceImpl.toFile().exists()) {
            this.getLog().debug("Skipping existing file " + serviceImpl.toString());
        } else {
            Files.write(serviceImpl, this.templates.generateServiceImplementation().getBytes());
        }

        for (final InterfaceDescription itf : serviceDescription.getInterfaces()) {
            final Path interfacePath = Files.createDirectories(dest.resolve(PluginUtils.getPackageName(itf)));
            final Path manager = interfacePath.resolve(PluginUtils.managerInterface(itf) + ext);
            final Path managerImpl = interfacePath.resolve(PluginUtils.managerClass(itf) + ext);

            // Write interface files
            Files.write(manager, this.templates.generateManagerInterface(itf).getBytes());

            if (managerImpl.toFile().exists()) {
                this.getLog().debug("Skipping existing file " + managerImpl.toString());
            } else {
                Files.write(managerImpl, this.templates.generateManagerImplementation(itf).getBytes());
            }

            for (final InterfaceVersionDescription version : itf.getInterfaceVersions()) {
                final Path interfaceVersionPath = Files
                        .createDirectories(interfacePath.resolve(PluginUtils.getPackageName(version)));

                // Create intermediate directories
                final Path connectionHandler = interfaceVersionPath
                        .resolve(PluginUtils.connectionHandlerInterface(itf, version) + ext);
                final Path connectionHandlerImpl = interfaceVersionPath
                        .resolve(PluginUtils.connectionHandlerClass(itf, version) + ext);

                // Write files
                Files.write(connectionHandler, this.templates.generateHandlerInterface(itf, version).getBytes());

                if (connectionHandlerImpl.toFile().exists()) {
                    this.getLog().debug("Skipping existing file " + connectionHandlerImpl.toString());
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
     *            The current ServiceDescription object
     * @throws IOException
     * @throws HashComputeException
     */
    private void createDockerfiles(final ServiceDescription service) throws IOException {
        this.getLog().debug("Creating Dockerfiles");

        final Path dockerFolder = Files.createDirectories(this.resourcePath.resolve(this.dockerLocation));
        final Path dockerArmFolder = Files.createDirectories(this.resourcePath.resolve(this.dockerArmLocation));

        Files.write(dockerFolder.resolve("Dockerfile"), this.templates.generateDockerfile("x86", service).getBytes());

        Files.write(dockerArmFolder.resolve("Dockerfile"),
                this.templates.generateDockerfile("arm", service).getBytes());
    }

    /**
     * @param service
     * @throws IOException
     */
    private void compileDescriptors(final ServiceDescription service) throws IOException {
        this.getLog().debug("Compiling protobuf definitions to java code");
        final ProtoCompiler protoCompiler = new ProtoCompiler(this.protobufVersion);
        final XjcCompiler xjcCompiler = new XjcCompiler();

        for (final InterfaceDescription iface : service.getInterfaces()) {
            for (final InterfaceVersionDescription versionDescription : iface.getInterfaceVersions()) {
                final String fullName = PluginUtils.getVersionedName(iface, versionDescription);

                if (versionDescription.getType().equals(Type.PROTO)) {
                    protoCompiler.compile(
                            this.resourcePath.resolve(this.protobufResourceLocation).resolve(fullName + ".proto"),
                            Paths.get(this.sourceLocation));
                } else if (versionDescription.getType().equals(Type.XSD)) {
                    xjcCompiler.setBasePackageName(this.servicePackage + "."
                            + PluginUtils.getPackageName(iface, versionDescription) + "." + this.xsdOutputPackage);
                    xjcCompiler.compile(this.resourcePath.resolve(this.xsdResourceLocation).resolve(fullName + ".xsd"),
                            Paths.get(this.sourceLocation));
                }
            }
        }
    }

}