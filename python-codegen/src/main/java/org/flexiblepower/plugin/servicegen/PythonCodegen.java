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

import org.flexiblepower.codegen.PluginUtils;
import org.flexiblepower.codegen.model.InterfaceDescription;
import org.flexiblepower.codegen.model.InterfaceVersionDescription;
import org.flexiblepower.codegen.model.InterfaceVersionDescription.Type;
import org.flexiblepower.codegen.model.ServiceDescription;
import org.flexiblepower.plugin.servicegen.compiler.ProtoCompiler;

import lombok.extern.slf4j.Slf4j;

/**
 * CreateComponentMojo
 *
 * @author coenvl
 * @version 0.1
 * @since Jun 28, 2017
 */
@Slf4j
public class PythonCodegen {

    private final String serviceFilename = "service.json";
    private final String resourceLocation = "resource";
    private final String sourceLocation = "service";
    private final String protobufVersion = "3.3.0";

    // In what subfolders to put all resources
    private final String protobufInputLocation = "proto";
    private final String protobufOutputLocation = "proto";
    // private final String xsdInputLocation = "xsd";
    private final String dockerLocation = "docker";
    private final String dockerArmLocation = "docker-arm";

    private final String dockerEntryPoint = "python ./service/main.py";

    private final Map<String, InterfaceVersionDescription> hashes = new HashMap<>();

    private ProtoCompiler protoCompiler;

    private Path resourcePath;
    private PythonTemplates templates;

    /**
     * Main function of the Maven plugin, will call the several stages of the
     * plugin.
     */
    public void run() throws Exception {
        // this.resourcePath = Paths.get(this.resourceLocation);
        final File serviceDescriptionFile = Paths.get(this.serviceFilename).toFile();
        if (!PluginUtils.validateServiceDefinition(serviceDescriptionFile)) {
            throw new Exception("Invalid service description, see message log");
        }

        final ServiceDescription service = PluginUtils.readServiceDefinition(serviceDescriptionFile);
        service.setId(PluginUtils.camelCaps(service.getName()));

        this.resourcePath = Paths.get(this.resourceLocation);
        Files.createDirectories(this.resourcePath);

        final Path pythonSourceFolder = Paths.get(this.sourceLocation);
        Files.createDirectories(pythonSourceFolder);

        // Add descriptors and related hashes
        this.compileDescriptors(service);

        // Add templates to generate java code and the dockerfile
        this.templates = new PythonTemplates(service);

        this.createPythonFiles(service, pythonSourceFolder);
        this.createDockerfiles(service);
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
    private void createPythonFiles(final ServiceDescription serviceDescription, final Path dest) throws IOException {
        PythonCodegen.log.debug("Creating stubs");

        final String ext = ".py";
        Files.createDirectories(dest);

        if (serviceDescription.getParameters() != null) {
            final Path configInterface = dest.resolve(JavaPluginUtils.configInterfaceClass(serviceDescription) + ext);
            Files.write(configInterface, this.templates.generateConfigInterface().getBytes());
        }

        final Path serviceImpl = dest.resolve(JavaPluginUtils.serviceImplClass(serviceDescription) + ext);
        if (serviceImpl.toFile().exists()) {
            PythonCodegen.log.debug("Skipping existing file " + serviceImpl.toString());
        } else {
            Files.write(serviceImpl, this.templates.generateServiceImplementation().getBytes());
        }

        for (final InterfaceDescription itf : serviceDescription.getInterfaces()) {
            final Path interfacePath = Files.createDirectories(dest.resolve(JavaPluginUtils.getPackageName(itf)));
            final Path manager = interfacePath.resolve(JavaPluginUtils.managerInterface(itf) + ext);
            final Path managerImpl = interfacePath.resolve(JavaPluginUtils.managerClass(itf) + ext);

            // Write interface files
            Files.write(manager, this.templates.generateManagerInterface(itf).getBytes());

            if (managerImpl.toFile().exists()) {
                PythonCodegen.log.debug("Skipping existing file " + managerImpl.toString());
            } else {
                Files.write(managerImpl, this.templates.generateManagerImplementation(itf).getBytes());
            }

            for (final InterfaceVersionDescription version : itf.getInterfaceVersions()) {
                final Path interfaceVersionPath = Files
                        .createDirectories(interfacePath.resolve(JavaPluginUtils.getPackageName(version)));

                // Create intermediate directories
                final Path connectionHandler = interfaceVersionPath
                        .resolve(JavaPluginUtils.connectionHandlerInterface(itf, version) + ext);
                final Path connectionHandlerImpl = interfaceVersionPath
                        .resolve(JavaPluginUtils.connectionHandlerClass(itf, version) + ext);

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
     *            The current ServiceDescription object
     * @throws IOException
     * @throws HashComputeException
     */
    private void createDockerfiles(final ServiceDescription service) throws IOException {
        PythonCodegen.log.debug("Creating Dockerfiles");

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
        PythonCodegen.log.debug("Compiling descriptors definitions to java code");

        this.protoCompiler = new ProtoCompiler(this.protobufVersion);
        // this.xjcCompiler = new XjcCompiler();

        for (final InterfaceDescription iface : service.getInterfaces()) {
            for (final InterfaceVersionDescription versionDescription : iface.getInterfaceVersions()) {

                if (versionDescription.getType().equals(Type.PROTO)) {
                    this.compileProtoDescriptor(iface, versionDescription);

                } else if (versionDescription.getType().equals(Type.XSD)) {
                    PythonCodegen.log.error("Not yet supported!");
                    // this.compileXSDDescriptor(iface, versionDescription);
                }
            }
        }
    }

    /**
     * @param itf
     * @param versionDescription
     * @throws IOException
     */
    // private void compileXSDDescriptor(final InterfaceDescription itf, final InterfaceVersionDescription vitf)
    // throws IOException {
    // // First get the hash of the input file
    // final Path xsdSourceFilePath = PluginUtils.downloadFileOrResolve(vitf.getLocation(),
    // this.resourcePath.resolve(this.xsdInputLocation));
    //
    // // Compute hash and store in interface
    // final String interfaceHash = PluginUtils.SHA256(xsdSourceFilePath);
    // vitf.setHash(interfaceHash);
    //
    // if (this.hashes.containsKey(interfaceHash)) {
    // vitf.setModelPackageName(this.hashes.get(interfaceHash).getModelPackageName());
    // return;
    // }
    //
    // // Get the package name and add the hash
    // vitf.setModelPackageName(
    // this.servicePackage + "." + JavaPluginUtils.getPackageName(itf, vitf) + "." + this.xsdOutputPackage);
    // this.hashes.put(interfaceHash, vitf);
    //
    // // Append additional compilation info to the proto file and compile the java code
    // final Path xsdResourceFolder = Files.createDirectories(this.resourcePath.resolve(this.xsdResourceLocation));
    // final String versionedName = JavaPluginUtils.getVersionedName(itf, vitf);
    // final Path xsdDestFilePath = xsdResourceFolder.resolve(versionedName + ".xsd");
    //
    // // Copy the descriptor and start compilation
    // Files.copy(xsdSourceFilePath, xsdDestFilePath, StandardCopyOption.REPLACE_EXISTING);
    //
    // this.xjcCompiler.setBasePackageName(vitf.getModelPackageName());
    // this.xjcCompiler.compile(xsdDestFilePath, Paths.get(this.sourceLocation));
    // }

    /**
     * @param itf
     * @param vitf
     * @throws IOException
     */
    private void compileProtoDescriptor(final InterfaceDescription itf, final InterfaceVersionDescription vitf)
            throws IOException {
        final Path protoSourceFilePath = PluginUtils.downloadFileOrResolve(vitf.getLocation(),
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
        final String versionedName = JavaPluginUtils.getVersionedName(itf, vitf);
        final String protoPackageName = JavaPluginUtils.getPackageName(itf, vitf);
        // if ((this.protobufOutputPackage != null) && !this.protobufOutputPackage.isEmpty()) {
        // protoPackageName = protoPackageName + "." + this.protobufOutputPackage;
        // }

        final String protoClassName = protoPackageName + "." + versionedName + "Proto";

        // Store for later reference
        vitf.setModelPackageName(protoClassName);
        this.hashes.put(interfaceHash, vitf);

        // Append additional compilation info to the proto file and compile the java code
        final Path protoDestFilePath = Files.createDirectories(this.resourcePath.resolve(this.protobufOutputLocation))
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

}