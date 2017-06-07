package org.flexiblepower.plugin.servicegen;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

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
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.flexiblepower.plugin.servicegen.model.InterfaceDescription;
import org.flexiblepower.plugin.servicegen.model.InterfaceVersionDescription;
import org.flexiblepower.plugin.servicegen.model.InterfaceVersionDescription.Type;
import org.flexiblepower.plugin.servicegen.model.ServiceDescription;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.databind.ObjectMapper;

@Mojo(name = "create", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class CreateComponentMojo extends AbstractMojo {

    // private static final String messageRepositoryLink = "http://efpi-rd1.sensorlab.tno.nl/descriptors";

    /**
     * Folder where the service.yml and descriptor/xsd files are located
     */
    @Parameter(property = "project.resourcedir", required = true)
    private String resourceDir;

    @Component
    private MavenProject project;

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
            this.servicePackage = this.project.getGroupId() + "." + this.project.getArtifactId();

            final ServiceDescription service = this.readServiceDefinition();
            this.createFolderStructure();

            // Add descriptors and related hashes
            this.createDescriptors(service.getInterfaces());
            this.templates = new Templates(this.servicePackage, service, this.hashes);

            this.createStubs(service);
            this.createDockerfile(service);

        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Reads and parses the service yaml into a ServiceDescription object
     *
     * @return ServiceDescription object containing the data of the yaml
     * @throws FileNotFoundException
     *             service.yml is not found in the resource directory
     */
    private ServiceDescription readServiceDefinition() throws IOException {
        final File inputFile = this.resourcePath.resolve("service.json").toFile();
        this.getLog().info(String.format("Reading service definition from %s", inputFile));

        try (InputStream input = new FileInputStream(inputFile)) {
            return (new ObjectMapper()).readValue(input, ServiceDescription.class);
        }
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
     * Creates altered descriptor files that can be compiled into java source
     * code.
     *
     * @param descriptors
     *            Map of descriptors
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws XPathExpressionException
     */
    private void createDescriptors(final Set<InterfaceDescription> interfaces)
            throws IOException, ParserConfigurationException, SAXException, XPathExpressionException {
        this.getLog().info("Creating descriptors");

        for (final InterfaceDescription iface : interfaces) {
            for (final InterfaceVersionDescription versionDescription : iface.getInterfaceVersions()) {
                final String fullName = Templates.camelCaps(iface.getName() + versionDescription.getVersionName());

                // versionDescription.getVersionName());
                final Path inputPath = this.resourcePath.resolve(versionDescription.getLocation());
                final Path outputPath = this.descriptorProtobufFolder.resolve(versionDescription.getLocation());

                if (versionDescription.getType().equals(Type.PROTO)) {
                    this.appendDescriptor(fullName, inputPath, outputPath);
                } else if (versionDescription.getType().equals(Type.XSD)) {
                    this.getLog().info("Reading xsd descriptor");
                    Files.copy(inputPath, outputPath, StandardCopyOption.REPLACE_EXISTING);
                }

                if (!this.hashes.containsKey(fullName)) {
                    this.hashes.put(fullName, this.fileToSHA256(outputPath));
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
     * @param filePath
     *            Path of the original descriptor file
     * @param outputPath
     *            Folder the altered descriptor file should be stored.
     * @throws IOException
     */
    private void appendDescriptor(final String name, final Path filePath, final Path outputPath) throws IOException {
        this.getLog().info("Modifying descriptor");
        Files.copy(filePath, outputPath, StandardCopyOption.REPLACE_EXISTING);

        try (final Scanner scanner = new Scanner(new FileInputStream(filePath.toFile()), "UTF-8")) {
            Files.write(outputPath,
                    ("syntax = \"proto2\";" + "\n\n" + "option java_package = \"" + this.servicePackage
                            + ".protobuf\";\n" + "option java_outer_classname = \"" + name + "Proto\";\n\n"
                            + scanner.useDelimiter("\\A").next()).getBytes(),
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
     */
    private void createStubs(final ServiceDescription serviceDescription) throws IOException {
        this.getLog().info("Creating stubs");

        final Path serviceImpl = this.sourceFolder.resolve(Templates.serviceImplClass(serviceDescription) + ".java");
        serviceImpl.toFile().delete();
        Files.write(serviceImpl, this.templates.generateServiceImplementation().getBytes(), StandardOpenOption.CREATE);

        for (final InterfaceDescription itf : serviceDescription.getInterfaces()) {
            for (final InterfaceVersionDescription version : itf.getInterfaceVersions()) {
                final Path factory = this.sourceHandlerFolder.resolve(Templates.factoryClass(itf, version) + ".java");
                final Path connectionHandler = this.sourceHandlerFolder
                        .resolve(Templates.connectionHandlerClass(itf, version) + ".java");
                final Path connectionHandlerImpl = this.sourceHandlerFolder
                        .resolve(Templates.connectionHandlerImplClass(itf, version) + "PublishHandler.java");
                try {
                    Files.write(factory,
                            this.templates.generateFactory(itf, version).getBytes(),
                            StandardOpenOption.CREATE_NEW);
                    Files.write(connectionHandler,
                            this.templates.generateConnectionHandler(itf, version).getBytes(),
                            StandardOpenOption.CREATE_NEW);
                    Files.write(connectionHandlerImpl,
                            this.templates.generateConnectionHandlerImplementation(itf, version).getBytes(),
                            StandardOpenOption.CREATE_NEW);
                } catch (final IOException e) {
                    this.getLog().info("Could not create stubs for interface " + itf.getName());
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

    /**
     * Helper function to convert a file into a SHA256 hash
     *
     * @param file
     *            File to be hashed
     * @return
     * @throws IOException
     * @throws FileNotFoundException
     * @throws NoSuchAlgorithmException
     */
    private String fileToSHA256(final Path file) throws FileNotFoundException, IOException {
        this.getLog().info("Hashing file");
        try (final FileInputStream fis = new FileInputStream(file.toFile())) {
            final MessageDigest md = MessageDigest.getInstance("SHA-256");
            final byte[] dataBytes = new byte[1024];

            int nread = 0;
            while ((nread = fis.read(dataBytes)) != -1) {
                md.update(dataBytes, 0, nread);
            }
            final byte[] mdbytes = md.digest();

            final StringBuffer sb = new StringBuffer();
            for (final byte mdbyte : mdbytes) {
                sb.append(Integer.toString((mdbyte & 0xff) + 0x100, 16).substring(1));
            }
            return sb.toString();
        } catch (final NoSuchAlgorithmException e) {
            throw new IOException("Error while computing hash for file " + file, e);
        }
    }
}
//