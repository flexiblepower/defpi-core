package org.flexiblepower.plugin.servicegen;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

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

    @Parameter(property = "defpi.docker-entrypoint",
               defaultValue = "java -jar $JVM_ARGUMENTS /${project.artifactId}-${project.version}-jar-with-dependencies.jar")
    private String dockerEntryPoint;

    /**
     * Folder where additional defpi resources may be put
     */
    private final String defpiResourceLocation = "defpi-resources";

    /**
     * Folder where the protobuf definitions should be copied to
     */
    private final String dockerLocation = "docker";

    /**
     * Folder where the protobuf definitions should be copied to
     */
    private final String dockerArmLocation = "docker-arm";

    private final Map<String, InterfaceVersionDescription> hashes = new HashMap<>();

    private ProtoCompiler protoCompiler;
    private XjcCompiler xjcCompiler;

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
            service.setId(this.artifactId);

            final Path javaSourceFolder = Paths.get(this.sourceLocation).resolve(this.servicePackage.replace('.', '/'));
            Files.createDirectories(javaSourceFolder);

            final Path defpiResourceFolder = Paths.get(this.resourceLocation).resolve(this.defpiResourceLocation);
            Files.createDirectories(defpiResourceFolder);

            // Add descriptors and related hashes
            this.compileDescriptors(service);

            // Add templates to generate java code and the dockerfile
            this.templates = new Templates(this.servicePackage, service);

            this.createJavaFiles(service, javaSourceFolder);
            this.createDockerfiles(service);

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

        if (serviceDescription.getParameters() != null) {
            final Path configInterface = dest.resolve(PluginUtils.configInterfaceClass(serviceDescription) + ext);
            Files.write(configInterface, this.templates.generateConfigInterface().getBytes());
        }

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

        Files.write(dockerFolder.resolve("Dockerfile"),
                this.templates.generateDockerfile("x86", service, this.dockerEntryPoint).getBytes());

        Files.write(dockerArmFolder.resolve("Dockerfile"),
                this.templates.generateDockerfile("arm", service, this.dockerEntryPoint).getBytes());
    }

    /**
     * @param service
     * @throws IOException
     */
    private void compileDescriptors(final ServiceDescription service) throws IOException {
        this.getLog().debug("Compiling descriptors definitions to java code");

        this.protoCompiler = new ProtoCompiler(this.protobufVersion);
        this.xjcCompiler = new XjcCompiler();

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
        final Path xsdSourceFilePath = this.downloadFileOrResolve(vitf.getLocation(),
                this.resourcePath.resolve(this.xsdInputLocation));

        // Compute hash and store in interface
        final String interfaceHash = PluginUtils.SHA256(xsdSourceFilePath);
        vitf.setHash(interfaceHash);

        if (this.hashes.containsKey(interfaceHash)) {
            vitf.setModelPackageName(this.hashes.get(interfaceHash).getModelPackageName());
            return;
        }

        // Get the package name and add the hash
        vitf.setModelPackageName(
                this.servicePackage + "." + PluginUtils.getPackageName(itf, vitf) + "." + this.xsdOutputPackage);
        this.hashes.put(interfaceHash, vitf);

        // Append additional compilation info to the proto file and compile the java code
        final Path xsdResourceFolder = Files.createDirectories(this.resourcePath.resolve(this.xsdResourceLocation));
        final String versionedName = PluginUtils.getVersionedName(itf, vitf);
        final Path xsdDestFilePath = xsdResourceFolder.resolve(versionedName + ".xsd");

        // Copy the descriptor and start compilation
        Files.copy(xsdSourceFilePath, xsdDestFilePath, StandardCopyOption.REPLACE_EXISTING);

        this.xjcCompiler.setBasePackageName(vitf.getModelPackageName());
        this.xjcCompiler.compile(xsdDestFilePath, Paths.get(this.sourceLocation));
    }

    /**
     * @param itf
     * @param vitf
     * @throws IOException
     */
    private void compileProtoDescriptor(final InterfaceDescription itf, final InterfaceVersionDescription vitf)
            throws IOException {
        final Path protoSourceFilePath = this.downloadFileOrResolve(vitf.getLocation(),
                this.resourcePath.resolve(this.protobufInputLocation));

        // Compute hash and store in interface
        final String interfaceHash = PluginUtils.SHA256(protoSourceFilePath);
        vitf.setHash(interfaceHash);

        if (this.hashes.containsKey(interfaceHash)) {
            // If we already have it, just copy the package name
            vitf.setModelPackageName(this.hashes.get(interfaceHash).getModelPackageName());
            return;
        }

        // Get the package name and add the hash
        final String versionedName = PluginUtils.getVersionedName(itf, vitf);
        String protoPackageName = this.servicePackage + "." + PluginUtils.getPackageName(itf, vitf);
        if ((this.protobufOutputPackage != null) && !this.protobufOutputPackage.isEmpty()) {
            protoPackageName = protoPackageName + "." + this.protobufOutputPackage;
        }

        final String protoClassName = protoPackageName + "." + versionedName + "Proto";

        // Store for later reference
        vitf.setModelPackageName(protoClassName);
        this.hashes.put(interfaceHash, vitf);

        // Append additional compilation info to the proto file and compile the java code

        final Path protoDestFilePath = Files.createDirectories(this.resourcePath.resolve(this.protobufResourceLocation))
                .resolve(versionedName + ".proto");
        Files.copy(protoSourceFilePath, protoDestFilePath, StandardCopyOption.REPLACE_EXISTING);

        try (final Scanner scanner = new Scanner(new FileInputStream(protoSourceFilePath.toFile()), "UTF-8")) {
            Files.write(protoDestFilePath,
                    ("syntax = \"proto2\";" + "\n\n" + "option java_package = \"" + protoPackageName + "\";\n"
                            + "option java_outer_classname = \"" + versionedName + "Proto\";\n\n" + "package "
                            + protoPackageName + ";\n" + scanner.useDelimiter("\\A").next()).getBytes());
        }

        this.protoCompiler.compile(protoDestFilePath, Paths.get(this.sourceLocation));

    }

    /**
     * @param location
     * @param resolve
     * @return
     */
    private Path downloadFileOrResolve(final String location, final Path resolve) {
        // First get the hash of the input file
        try {
            final URL url = new URL(location);
            this.getLog().info("Downloading descriptor from " + url);
            final Path tempFile = Files.createTempFile(null, null);
            try (
                    InputStream is = url.openConnection().getInputStream();
                    FileOutputStream fos = new FileOutputStream(tempFile.toFile())) {
                final byte[] buff = new byte[1024];
                int read = 0;
                while ((read = is.read(buff)) != -1) {
                    fos.write(buff, 0, read);
                }
            }
            // If we wrote it, continue with the downloaded file
            return tempFile;
        } catch (final IOException e) {
            return this.resourcePath.resolve(this.protobufInputLocation).resolve(location);
        }
    }

}