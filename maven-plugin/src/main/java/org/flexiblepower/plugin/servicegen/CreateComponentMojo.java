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

@Mojo(name = "create", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class CreateComponentMojo extends AbstractMojo {

    private static final String SERVICE_FILENAME = "service.json";

    // private static final String messageRepositoryLink = "http://efpi-rd1.sensorlab.tno.nl/descriptors";

    /**
     * Folder where the service.yml and descriptor/xsd files are located
     */
    @Parameter(property = "project.resourcedir", required = true)
    private String resourceDir;

    // @Component
    @Parameter(property = "project.groupId")
    private String groupId;

    @Parameter(property = "project.artifactId")
    private String artifactId;

    // private MavenProject project;

    /**
     * Main package of the service
     */
    private String servicePackage;

    /**
     * SHA256 hashes that identify a specific descriptorcol buffer description
     */
    private final Map<String, String> hashes = new HashMap<>();

    /**
     * Main resources path, from parameter
     */
    private Path resourcePath;
    /**
     * Main java source folder
     */
    private Path sourceFolder;
    /**
     * Java source folder for service handlers
     */
    private Path sourceHandlerFolder;
    /**
     * Folder to store altered protobuf descriptor files
     */
    private Path descriptorProtobufFolder;
    /**
     * Folder to store xsd descriptor files
     */
    // private Path descriptorXSDFolder;
    /**
     * Folder where the Dockerfile is located
     */
    private Path dockerFolder;

    /**
     * Folder where the ARM Dockerfile is located
     */
    private Path dockerARMFolder;

    /**
     * Templates for code generation
     */
    private Templates templates;

    /**
     * Main function of the Maven plugin, will call the several stages of the
     * plugin.
     */
    @Override
    public void execute() throws MojoExecutionException {
        try {
            this.resourcePath = Paths.get(this.resourceDir);
            this.servicePackage = this.groupId + "." + this.artifactId;
            // this.servicePackage = this.project.getGroupId() + "." + this.project.getArtifactId();

            final File serviceDescriptionFile = this.resourcePath.resolve(CreateComponentMojo.SERVICE_FILENAME)
                    .toFile();
            if (!this.validateServiceDefinition(serviceDescriptionFile)) {
                throw new MojoExecutionException("Invalid service description, see message log");
            }

            final ServiceDescription service = this.readServiceDefinition(serviceDescriptionFile);
            this.createFolderStructure();

            // Add descriptors and related hashes
            this.createDescriptors(service.getInterfaces());
            this.templates = new Templates(this.servicePackage, service, this.hashes);

            this.createJavaFiles(service);
            this.createDockerfile(service);

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
     * Makes the folders for the generated files
     *
     * @throws IOException
     */
    private void createFolderStructure() throws IOException {
        this.getLog().info("Creating folder structure");
        this.sourceFolder = Paths.get("src", "main", "java", this.servicePackage.replace('.', '/'));

        this.sourceHandlerFolder = Files.createDirectories(this.sourceFolder.resolve("handlers"));
        this.descriptorProtobufFolder = Files.createDirectories(this.resourcePath.resolve("protobuf"));
        // this.descriptorXSDFolder = Files.createDirectories(this.resourcePath.resolve("xsd"));
        this.dockerFolder = Files.createDirectories(this.resourcePath.resolve("docker"));
        this.dockerARMFolder = Files.createDirectories(this.resourcePath.resolve("docker-arm"));
    }

    /**
     * Creates altered descriptor files that can be compiled into java source code.
     *
     * @param descriptors
     *            Map of descriptors
     * @throws IOException
     */
    private void createDescriptors(final Set<InterfaceDescription> interfaces) throws IOException {
        this.getLog().info("Creating descriptors");

        for (final InterfaceDescription iface : interfaces) {
            for (final InterfaceVersionDescription versionDescription : iface.getInterfaceVersions()) {
                final String fullName = PluginUtils.getVersionedName(iface, versionDescription);

                final Path inputPath = this.resourcePath.resolve(versionDescription.getLocation());
                final Path outputPath = this.descriptorProtobufFolder.resolve(fullName + ".proto");

                if (versionDescription.getType().equals(Type.PROTO)) {
                    this.appendDescriptor(fullName, inputPath, outputPath);
                } else if (versionDescription.getType().equals(Type.XSD)) {
                    this.getLog().info("Reading xsd descriptor");
                    Files.copy(inputPath, outputPath, StandardCopyOption.REPLACE_EXISTING);
                }

                this.hashes.put(fullName, PluginUtils.SHA256(outputPath));
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
    private void appendDescriptor(final String name, final Path inputPath, final Path outputPath) throws IOException {
        this.getLog().info("Modifying descriptor");
        Files.copy(inputPath, outputPath, StandardCopyOption.REPLACE_EXISTING);

        try (final Scanner scanner = new Scanner(new FileInputStream(inputPath.toFile()), "UTF-8")) {
            Files.write(outputPath,
                    ("syntax = \"proto2\";" + "\n\n" + "option java_package = \"" + this.servicePackage
                            + ".protobuf\";\n" + "option java_outer_classname = \"" + name + "Proto\";\n\n" + "package "
                            + name + ";\n" + scanner.useDelimiter("\\A").next()).getBytes(),
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
    private void createJavaFiles(final ServiceDescription serviceDescription) throws IOException {
        this.getLog().info("Creating stubs");

        final String ext = ".java";
        final Path serviceImpl = this.sourceFolder.resolve(PluginUtils.serviceImplClass(serviceDescription) + ext);
        serviceImpl.toFile().delete();
        Files.write(serviceImpl, this.templates.generateServiceImplementation().getBytes(), StandardOpenOption.CREATE);

        for (final InterfaceDescription itf : serviceDescription.getInterfaces()) {
            for (final InterfaceVersionDescription version : itf.getInterfaceVersions()) {
                final Path factory = this.sourceHandlerFolder.resolve(PluginUtils.factoryClass(itf, version) + ext);
                final Path connectionHandler = this.sourceHandlerFolder
                        .resolve(PluginUtils.connectionHandlerClass(itf, version) + ext);
                final Path connectionHandlerImpl = this.sourceHandlerFolder
                        .resolve(PluginUtils.connectionHandlerImplClass(itf, version) + ext);

                Files.write(factory,
                        this.templates.generateFactory(itf, version).getBytes(),
                        StandardOpenOption.CREATE);
                Files.write(connectionHandler,
                        this.templates.generateConnectionHandler(itf, version).getBytes(),
                        StandardOpenOption.CREATE);
                Files.write(connectionHandlerImpl,
                        this.templates.generateConnectionHandlerImplementation(itf, version).getBytes(),
                        StandardOpenOption.CREATE);
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
    private void createDockerfile(final ServiceDescription service) throws IOException {
        this.getLog().info("Creating Dockerfiles");
        Files.write(this.dockerFolder.resolve("Dockerfile"),
                this.templates.generateDockerfile("x86", service).getBytes(),
                StandardOpenOption.CREATE);
        Files.write(this.dockerARMFolder.resolve("Dockerfile"),
                this.templates.generateDockerfile("arm", service).getBytes(),
                StandardOpenOption.CREATE);
    }
}