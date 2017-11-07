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
package org.flexiblepower.plugin.servicegen;

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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.flexiblepower.codegen.PluginUtils;
import org.flexiblepower.codegen.model.InterfaceDescription;
import org.flexiblepower.codegen.model.InterfaceVersionDescription;
import org.flexiblepower.codegen.model.InterfaceVersionDescription.Type;
import org.flexiblepower.codegen.model.ServiceDescription;
import org.flexiblepower.plugin.servicegen.compiler.ProtoCompiler;
import org.flexiblepower.plugin.servicegen.compiler.XjcCompiler;
import org.sonatype.plexus.build.incremental.BuildContext;

import com.fasterxml.jackson.core.JsonParseException;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;

/**
 * CreateComponentMojo
 *
 * @version 0.1
 * @since Jun 28, 2017
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class CreateComponentMojo extends AbstractMojo {

    @Component
    private BuildContext buildContext;

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    /**
     * Version of protobuf to use
     */
    @Parameter(property = "protobuf.version", required = true, readonly = true)
    private String protobufVersion;

    /**
     * Folder where the service.yml and descriptor/xsd files are located
     */
    @Parameter(property = "project.resources.directory",
               defaultValue = "${project.basedir}/src/main/resources",
               readonly = true)
    private String inputResources;

    /**
     * Folder where the intermediate (generated) resources should be placed
     */
    @Parameter(property = "defpi.resources.generated",
               defaultValue = "${project.build.directory}/generated-resources/defpi",
               readonly = true)
    private String targetResources;

    /**
     * Main java source folder where all placeholder java sources should be put
     */
    @Parameter(property = "defpi.sources.main", defaultValue = "${project.basedir}/src/main/java", readonly = true)
    private String mainSourceLocation;

    /**
     * Main java source folder where all generated java sources should be put that shouldn't be altered
     */
    @Parameter(property = "defpi.sources.generated",
               defaultValue = "${project.build.directory}/generated-sources/java",
               readonly = true)
    private String genSourceLocation;

    /**
     * Folder where the protobuf sources can be found, relative to the ${project.resources.directory}
     */
    @Parameter(property = "defpi.proto.inputfolder", defaultValue = ".")
    private String protobufInputLocation;

    /**
     * Folder where the protobuf resources should be put, relative to the ${defpi.resources.generated}
     */
    @Parameter(property = "defpi.proto.outputfolder", defaultValue = "proto")
    private String protobufOutputLocation;

    /**
     * Package where the protobuf sources should be placed (in folder ${defpi.sources.generated})
     */
    @Parameter(property = "defpi.proto.package", defaultValue = "proto")
    private String protobufOutputPackage;

    /**
     * Folder where the XSD sources can be found, relative to the ${project.resources.directory}
     */
    @Parameter(property = "defpi.xsd.inputfolder", defaultValue = ".")
    private String xsdInputLocation;

    /**
     * Folder where the XSD resources should be put, relative to the ${defpi.resources.generated}
     */
    @Parameter(property = "defpi.xsd.outputfolder", defaultValue = "xsd")
    private String xsdOutputLocation;

    /**
     * Package where the XSD sources will be put in (in source folder ${defpi.sources.generated})
     */
    @Parameter(property = "defpi.xsd.package", defaultValue = "xml")
    private String xsdOutputPackage;

    /**
     * Service definition file, relative to the ${project.resources.directory}
     */
    @Parameter(property = "defpi.service.description", defaultValue = "service.json")
    private String serviceFilename;

    /**
     * Main package of the service
     */
    @Parameter(property = "defpi.service.package", defaultValue = "${project.groupId}.${project.artifactId}")
    private String servicePackage;

    /**
     * Entrypoint for the Docker image, that is the command to execute when the service is started
     */
    @Parameter(property = "defpi.docker-entrypoint",
               defaultValue = "java -jar $JVM_ARGUMENTS /${project.artifactId}-${project.version}-jar-with-dependencies.jar")
    private String dockerEntryPoint;

    /**
     * Folder where additional defpi resources may be put that will end up in the service image, relative to the
     * ${project.resources.directory}
     */
    @Parameter(property = "defpi.service.resources", defaultValue = "defpi-resources")
    private String serviceResourceLocation;

    /**
     * Folder where the x86 dockerfile should be placed, relative to ${defpi.sources.generated}
     */
    @Parameter(property = "defpi.dockerfolder.x86", defaultValue = "docker", readonly = true)
    private String dockerLocation;

    /**
     * Folder where the ARM dockerfile should be placed, relative to ${defpi.sources.generated}
     */
    @Parameter(property = "defpi.dockerfolder.arm", defaultValue = "docker-arm", readonly = true)
    private String dockerArmLocation;

    private final Map<String, InterfaceVersionDescription> hashes = new HashMap<>();

    private ProtoCompiler protoCompiler;
    private XjcCompiler xjcCompiler;

    private JavaTemplates templates;

    /**
     * Main function of the Maven plugin, will call the several stages of the plugin.
     *
     * @throws MojoFailureException
     * @throws MojoExecutionException
     */
    @Override
    public void execute() throws MojoFailureException, MojoExecutionException {
        try {
            final File serviceDescriptionFile = Paths.get(this.inputResources).resolve(this.serviceFilename).toFile();
            if (!this.buildContextUpdated(serviceDescriptionFile)) {
                this.getLog().info("No change for service definition, not running code generator");
                return;
            }

            this.validateServiceDefinition(serviceDescriptionFile);
            if (!this.servicePackage.matches("[a-z][a-z0-9_.]+")) {
                this.getLog().warn("Invalid java package name " + this.servicePackage);
                this.servicePackage = this.servicePackage.replaceAll("[^a-zA-Z0-9_.]", "").toLowerCase();
                this.getLog().warn("New target package name " + this.servicePackage);
            }

            final ServiceDescription service = PluginUtils.readServiceDefinition(serviceDescriptionFile);
            service.setId(this.project.getArtifactId());

            // Add descriptors and related hashes
            this.compileDescriptors(service);

            // Add templates to generate java code and the dockerfile
            this.templates = new JavaTemplates(this.servicePackage, service);

            this.createJavaFiles(service);
            this.createDockerfiles(service);

            // If it doesn't exist, create the service-resource folder that should be included in the docker image
            final Path defpiResourceFolder = Paths.get(this.inputResources).resolve(this.serviceResourceLocation);
            Files.createDirectories(defpiResourceFolder);

            // Add (and refresh) source folders
            if (this.buildContext != null) {
                this.buildContext.refresh(new File(this.genSourceLocation));
                this.buildContext.refresh(new File(this.mainSourceLocation));
            }
        } catch (final ProcessingException | IOException e) {
            this.getLog().error(e.getMessage() + ": " + e.getStackTrace()[0], e);
            throw new MojoFailureException(e.getMessage(), e);
        } catch (final Exception e) {
            this.getLog().error(e.getMessage() + ": " + e.getStackTrace()[0], e);
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private boolean buildContextUpdated(final File serviceDescriptionFile) throws IOException {
        // If the buildcontext is null we are not in eclipse, make sure we run any way
        if ((this.buildContext == null) || this.buildContext.hasDelta(serviceDescriptionFile)) {
            return true;
        }

        final File[] protosourceFiles = Paths.get(this.inputResources)
                .resolve(this.protobufInputLocation)
                .toFile()
                .listFiles();
        for (final File proto : protosourceFiles) {
            if (this.buildContext.hasDelta(proto)) {
                return true;
            }
        }

        final File[] xsdsourceFiles = Paths.get(this.inputResources)
                .resolve(this.xsdInputLocation)
                .toFile()
                .listFiles();
        for (final File xsd : xsdsourceFiles) {
            if (this.buildContext.hasDelta(xsd)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param serviceDefinition
     * @throws ProcessingException
     * @throws IOException
     */
    private void validateServiceDefinition(final File serviceDefinition) throws ProcessingException, IOException {
        this.buildContext.removeMessages(serviceDefinition);
        try {
            final ProcessingReport report = PluginUtils.processServiceDefinition(serviceDefinition);
            if (!report.isSuccess()) {
                for (final ProcessingMessage m : report) {
                    this.buildContext
                            .addMessage(serviceDefinition, 1, 1, m.getMessage(), BuildContext.SEVERITY_ERROR, null);
                }
                throw new ProcessingException("Invalid service description, see message log");
            }
        } catch (final JsonParseException e) {
            this.buildContext.addMessage(serviceDefinition,
                    e.getLocation().getLineNr(),
                    e.getLocation().getColumnNr(),
                    e.getOriginalMessage(),
                    BuildContext.SEVERITY_ERROR,
                    null);
        } finally {
            this.buildContext.refresh(serviceDefinition);
        }
    }

    /**
     * Create stubs for the service implementation. By using the templates in the JavaTemplates object.
     *
     * @param serviceDescription
     * @throws IOException
     */
    private void createJavaFiles(final ServiceDescription serviceDescription) throws IOException {
        this.getLog().debug("Creating stubs");

        final String packageFolder = this.servicePackage.replace('.', '/');
        final Path mainJavaFolder = Paths.get(this.mainSourceLocation).resolve(packageFolder);
        final Path generatedJavaFolder = Paths.get(this.genSourceLocation).resolve(packageFolder);

        Files.createDirectories(mainJavaFolder);
        Files.createDirectories(generatedJavaFolder);

        final String ext = ".java";
        if (serviceDescription.getParameters() != null) {
            final Path configInterface = generatedJavaFolder
                    .resolve(JavaPluginUtils.configInterfaceClass(serviceDescription) + ext);
            Files.write(configInterface, this.templates.generateConfigInterface().getBytes());
        }

        final Path serviceImpl = mainJavaFolder.resolve(JavaPluginUtils.serviceImplClass(serviceDescription) + ext);
        if (serviceImpl.toFile().exists()) {
            this.getLog().debug("Skipping existing file " + serviceImpl.toString());
        } else {
            Files.write(serviceImpl, this.templates.generateServiceImplementation().getBytes());
        }

        for (final InterfaceDescription itf : serviceDescription.getInterfaces()) {
            final String interfacePackageName = JavaPluginUtils.getPackageName(itf);
            final Path interfaceGeneratedPath = Files
                    .createDirectories(generatedJavaFolder.resolve(interfacePackageName));
            final Path interfaceMainPath = Files.createDirectories(mainJavaFolder.resolve(interfacePackageName));

            final Path manager = interfaceGeneratedPath.resolve(JavaPluginUtils.managerInterface(itf) + ext);
            final Path managerImpl = interfaceMainPath.resolve(JavaPluginUtils.managerClass(itf) + ext);

            // Write interface files
            Files.write(manager, this.templates.generateManagerInterface(itf).getBytes());

            if (managerImpl.toFile().exists()) {
                this.getLog().debug("Skipping existing file " + managerImpl.toString());
            } else {
                Files.write(managerImpl, this.templates.generateManagerImplementation(itf).getBytes());
            }

            for (final InterfaceVersionDescription version : itf.getInterfaceVersions()) {
                // Create folders for the version directories
                final String versionPackageName = JavaPluginUtils.getPackageName(version);
                final Path versionGeneratedPath = Files
                        .createDirectories(interfaceGeneratedPath.resolve(versionPackageName));
                final Path versionMainPath = Files.createDirectories(interfaceMainPath.resolve(versionPackageName));

                final Path connectionHandler = versionGeneratedPath
                        .resolve(JavaPluginUtils.connectionHandlerInterface(itf, version) + ext);
                final Path connectionHandlerImpl = versionMainPath
                        .resolve(JavaPluginUtils.connectionHandlerClass(itf, version) + ext);

                // Write files
                Files.write(connectionHandler, this.templates.generateHandlerInterface(itf, version).getBytes())
                        .toFile();

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
     * Create the Dockerfiles
     *
     * @param service
     *            The current ServiceDescription object
     * @throws IOException
     */
    private void createDockerfiles(final ServiceDescription service) throws IOException {
        this.getLog().debug("Creating Dockerfiles");

        final Path targetResourcePath = Paths.get(this.targetResources);
        final Path dockerFolder = Files.createDirectories(targetResourcePath.resolve(this.dockerLocation));
        final Path dockerArmFolder = Files.createDirectories(targetResourcePath.resolve(this.dockerArmLocation));

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
     * @param vitf
     * @throws IOException
     */
    private void compileXSDDescriptor(final InterfaceDescription itf, final InterfaceVersionDescription vitf)
            throws IOException {
        // First get the hash of the input file
        final Path xsdSourceFilePath = PluginUtils.downloadFileOrResolve(vitf.getLocation(),
                Paths.get(this.inputResources).resolve(this.xsdInputLocation));

        // Compute hash and store in interface
        final String interfaceHash = PluginUtils.SHA256(xsdSourceFilePath);
        vitf.setHash(interfaceHash);

        if (this.hashes.containsKey(interfaceHash)) {
            vitf.setModelPackageName(this.hashes.get(interfaceHash).getModelPackageName());
            return;
        }

        // Get the package name and add the hash
        vitf.setModelPackageName(
                this.servicePackage + "." + JavaPluginUtils.getPackageName(itf, vitf) + "." + this.xsdOutputPackage);
        this.hashes.put(interfaceHash, vitf);

        // Append additional compilation info to the proto file and compile the java code
        final Path xsdDestFilePath = Files
                .createDirectories(Paths.get(this.targetResources).resolve(this.xsdOutputLocation))
                .resolve(JavaPluginUtils.getVersionedName(itf, vitf) + ".xsd");

        // Copy the descriptor and start compilation
        Files.copy(xsdSourceFilePath, xsdDestFilePath, StandardCopyOption.REPLACE_EXISTING);
        this.xjcCompiler.setBasePackageName(vitf.getModelPackageName());
        this.xjcCompiler.compile(xsdDestFilePath, Paths.get(this.genSourceLocation));
    }

    /**
     * @param itf
     * @param vitf
     * @throws IOException
     */
    private void compileProtoDescriptor(final InterfaceDescription itf, final InterfaceVersionDescription vitf)
            throws IOException {
        final Path protoSourceFilePath = PluginUtils.downloadFileOrResolve(vitf.getLocation(),
                Paths.get(this.inputResources).resolve(this.protobufInputLocation));

        // Compute hash and store in interface
        final String interfaceHash = PluginUtils.SHA256(protoSourceFilePath);
        vitf.setHash(interfaceHash);

        if (this.hashes.containsKey(interfaceHash)) {
            // If we already have it, just copy the package name
            vitf.setModelPackageName(this.hashes.get(interfaceHash).getModelPackageName());
            return;
        }

        // Get the package name and add the hash
        final String versionedName = JavaPluginUtils.getVersionedName(itf, vitf);
        String protoPackageName = this.servicePackage + "." + JavaPluginUtils.getPackageName(itf, vitf);
        if ((this.protobufOutputPackage != null) && !this.protobufOutputPackage.isEmpty()) {
            protoPackageName = protoPackageName + "." + this.protobufOutputPackage;
        }

        final String protoClassName = protoPackageName + "." + versionedName + "Proto";

        // Store for later reference
        vitf.setModelPackageName(protoClassName);
        this.hashes.put(interfaceHash, vitf);

        // Append additional compilation info to the proto file and compile the java code
        final Path protoDestFilePath = Files
                .createDirectories(Paths.get(this.targetResources).resolve(this.protobufOutputLocation))
                .resolve(versionedName + ".proto");
        Files.copy(protoSourceFilePath, protoDestFilePath, StandardCopyOption.REPLACE_EXISTING);

        try (final Scanner scanner = new Scanner(new FileInputStream(protoSourceFilePath.toFile()), "UTF-8")) {
            Files.write(protoDestFilePath,
                    ("syntax = \"proto2\";" + "\n\n" + "option java_package = \"" + protoPackageName + "\";\n"
                            + "option java_outer_classname = \"" + versionedName + "Proto\";\n\n" + "package "
                            + protoPackageName + ";\n" + scanner.useDelimiter("\\A").next()).getBytes());
        }

        this.protoCompiler.compile(protoDestFilePath, Paths.get(this.genSourceLocation));
    }

}