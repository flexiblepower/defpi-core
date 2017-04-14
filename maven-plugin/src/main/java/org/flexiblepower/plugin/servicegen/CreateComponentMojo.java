package org.flexiblepower.plugin.servicegen;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;

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
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;
import org.flexiblepower.plugin.servicegen.model.Descriptor;
import org.flexiblepower.plugin.servicegen.model.Interface;
import org.flexiblepower.plugin.servicegen.model.Service;
import org.flexiblepower.plugin.servicegen.model.Type;
import org.xml.sax.SAXException;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import japa.parser.JavaParser;
import japa.parser.ParseException;
import japa.parser.ast.CompilationUnit;
import japa.parser.ast.body.ClassOrInterfaceDeclaration;
import japa.parser.ast.expr.AnnotationExpr;
import japa.parser.ast.expr.MemberValuePair;
import japa.parser.ast.expr.NormalAnnotationExpr;
import japa.parser.ast.visitor.VoidVisitorAdapter;

@Mojo(name = "create", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class CreateComponentMojo extends AbstractMojo {

    private static final String messageRepositoryLink = "http://efpi-rd1.sensorlab.tno.nl/descriptors";
    /**
     * Folder where the service.yml and descriptor/xsd files are located
     */
    @Parameter(property = "project.resourcedir", required = true)
    private File resourceDir;

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
     * List of found ConnectionFactory classes via reflection and a java
     * annotation
     */
    private final Set<String> existingFactories = new HashSet<>();

    /**
     * Main java source folder
     */
    private File sourceFolder;
    /**
     * Java source folder for service handlers
     */
    private File sourceHandlerFolder;
    /**
     * Folder to store altered protobuf descriptor files
     */
    private File descriptorProtobufFolder;
    /**
     * Folder to store xsd descriptor files
     */
    private File descriptorXSDFolder;
    /**
     * Folder where the Dockerfile is located
     */
    private File dockerFolder;

    /**
     * Folder where the ARM Dockerfile is located
     */
    private File dockerARMFolder;

    /**
     * Templates for code generation
     */
    private final Templates templates = new Templates();

    /**
     * Main function of the Maven plugin, will call the several stages of the
     * plugin.
     */
    @Override
    public void execute() throws MojoExecutionException {
        try {
            final Service service = this.readServiceYaml();
            this.servicePackage = this.project.getGroupId() + "." + this.project.getArtifactId();
            this.templates.servicePackage = this.servicePackage;
            this.createFolderStructure();

            final Map<String, Descriptor> descriptors = service.getDescriptors();

            this.createDescriptors(descriptors);

            descriptors.put(null, new Descriptor());
            for (final Interface i : service.getInterfaces()) {
                for (final Type type : i.getPublish()) {
                    type.setDescriptorObject(descriptors.get(type.getDescriptor()));
                }
                for (final Type type : i.getSubscribe()) {
                    type.setDescriptorObject(descriptors.get(type.getDescriptor()));
                }
            }
            this.templates.hashes = this.hashes;
            try {
                this.findExistingFactories();
            } catch (final Exception e) {
                this.getLog().info("Couldn't parse existing files in the handler package");
            }
            this.createStubs(service.getInterfaces());
            this.createDockerfile(service);

        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Reads and parses the service yaml into a Service object
     *
     * @return Service object containing the data of the yaml
     * @throws FileNotFoundException
     *             service.yml is not found in the resource directory
     */
    private Service readServiceYaml() throws IOException {
        final Constructor constructor = new Constructor(Service.class);

        final TypeDescription interfaceDescription = new TypeDescription(Service.class);
        interfaceDescription.putListPropertyType("interfaces", Interface.class);

        final TypeDescription descriptorsDescription = new TypeDescription(Service.class);
        descriptorsDescription.putMapPropertyType("descriptors", String.class, Descriptor.class);

        final TypeDescription typesDescription = new TypeDescription(Interface.class);
        typesDescription.putListPropertyType("subscribe", Type.class);
        typesDescription.putListPropertyType("publish", Type.class);

        constructor.addTypeDescription(interfaceDescription);
        constructor.addTypeDescription(descriptorsDescription);
        constructor.addTypeDescription(typesDescription);

        final Yaml yaml = new Yaml(constructor);
        try (InputStream input = new FileInputStream(new File(this.resourceDir.getPath() + "/service.yml"))) {
            return (Service) yaml.load(input);
        }
    }

    /**
     * Makes the folders for the generated files
     */
    private void createFolderStructure() {
        this.getLog().info("Creating folder structure");
        this.sourceFolder = new File("src/main/java/" + this.servicePackage.replace('.', '/'));

        this.sourceHandlerFolder = new File(this.sourceFolder.getPath() + "/handlers");
        this.sourceHandlerFolder.mkdirs();

        this.descriptorProtobufFolder = new File(this.resourceDir.getPath() + "/protobuf");
        this.descriptorProtobufFolder.mkdirs();

        this.descriptorXSDFolder = new File(this.resourceDir.getPath() + "/xsd");
        this.descriptorXSDFolder.mkdirs();

        this.dockerFolder = new File(this.resourceDir.getPath() + "/docker/");
        this.dockerFolder.mkdirs();

        this.dockerARMFolder = new File(this.resourceDir.getPath() + "/docker-arm/");
        this.dockerARMFolder.mkdirs();
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
    private void createDescriptors(final Map<String, Descriptor> descriptors)
            throws IOException, ParserConfigurationException, SAXException, XPathExpressionException {
        this.getLog().info("Creating descriptors");

        for (final Entry<String, Descriptor> descriptorEntry : descriptors.entrySet()) {
            final String name = descriptorEntry.getKey();
            final Descriptor descriptor = descriptorEntry.getValue();
            if (descriptor.getUrl() != null) {
                this.downloadDescriptor(name, descriptor);
            }

            if (descriptor.getSource().equals("protobuf")) {
                final Path filePath = Paths.get(this.resourceDir.getPath() + "/" + descriptor.getFile());
                this.appendDescriptor(name, descriptor, filePath, this.descriptorProtobufFolder.toPath());
                if (descriptor.isUpload()) {
                    this.uploadDescriptor(name, filePath.toFile());
                }
            } else if (descriptor.getSource().equals("xsd")) {
                System.out.println("Reading xsd descriptor");
                final Path filePath = Paths.get(this.resourceDir.getPath() + "/" + descriptor.getFile());
                final Path output = Paths.get(this.descriptorXSDFolder.getPath() + "/" + descriptor.getFile());
                Files.copy(filePath, output, StandardCopyOption.REPLACE_EXISTING);
                this.hashes.put(name, this.fileToSHA256(filePath.toFile()));
            }
        }

        this.getLog().info("Generated descriptors: " + Arrays.toString(this.hashes.entrySet().toArray()));
    }

    /**
     * Appends the original descriptor with information used by the descriptorc
     * compiler. The java package and outer classname have to be set, and also
     * the specific syntax (descriptor2 or descriptor3) is set by this method.
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
    private void appendDescriptor(final String name,
            final Descriptor descriptor,
            final Path filePath,
            final Path outputPath) throws IOException {
        this.getLog().info("Modifying descriptor");
        if (!this.hashes.containsKey(name)) {
            this.hashes.put(name, this.fileToSHA256(filePath.toFile()));
        }
        final Path output = Paths.get(outputPath + "/" + descriptor.getFile());
        Files.copy(filePath, output, StandardCopyOption.REPLACE_EXISTING);

        try (final Scanner scanner = new Scanner(new FileInputStream(filePath.toFile()), "UTF-8")) {
            Files.write(output,
                    ("syntax = \"proto2\";" + "\n\n" + "option java_package = \"" + this.servicePackage
                            + ".protobuf\";\n" + "option java_outer_classname = \"" + name + "Proto\";\n\n"
                            + scanner.useDelimiter("\\A").next()).getBytes(),
                    StandardOpenOption.CREATE);
        }
    }

    /**
     * Downloads the descriptor file from the message repository and store it in
     * the resource directory
     *
     * @param name
     *            Name of the descriptor
     * @param descriptor
     *            Descriptor object indicating which file has to be used
     * @throws MalformedURLException
     * @throws IOException
     */
    private void downloadDescriptor(final String name, final Descriptor descriptor)
            throws MalformedURLException, IOException {
        this.getLog().info("Downloading descriptor");
        descriptor.setUpload(false);
        if (descriptor.getSource().equals("protobuf")) {
            descriptor.setFile(name + ".proto");
        } else if (descriptor.getSource().equals("xsd")) {
            descriptor.setFile(name + ".xsd");
        }
        final String hash = descriptor.getUrl().substring(descriptor.getUrl().lastIndexOf('/') + 1);
        this.hashes.put(name, hash);
        FileUtils.copyURLToFile(new URL(descriptor.getUrl()),
                new File(this.resourceDir.getPath() + "/" + descriptor.getFile()));
        this.getLog().info("Descriptor downloaded from: " + descriptor.getUrl());
    }

    /**
     * Upload the descriptor file to the registry.
     *
     * @param name
     *            Name of the descriptor
     * @param file
     *            Descriptor file that will be uploaded
     * @throws ClientDescriptorcolException
     * @throws IOException
     */
    private void uploadDescriptor(final String name, final File file) throws ClientProtocolException, IOException {
        this.getLog().info("Uploading descriptor");
        @SuppressWarnings("resource")
        final HttpClient client = HttpClientBuilder.create().build();
        final HttpPost request = new HttpPost(CreateComponentMojo.messageRepositoryLink);
        final String json = "{\"name\": \"%s\", \"sha256\": \"%s\", \"descriptor\": \"%s\"}";

        String fileContent;
        try (Scanner scanner = new Scanner(new FileInputStream(file), "UTF-8")) {
            fileContent = scanner.useDelimiter("\\A").next();
        }

        final HttpEntity entity = EntityBuilder.create()
                .setText(String.format(json, name, this.hashes.get(name), StringUtils.escape(fileContent)))
                .build();
        request.setEntity(entity);
        request.setHeader("Accept", "application/json");
        request.setHeader("Content-Type", "application/json");

        final HttpResponse r = client.execute(request);
        this.getLog().info("Descriptor uploaded (" + r.getStatusLine().getStatusCode() + ")");

    }

    /**
     * Find existing factories in the handler package. This is done by parsing
     * the java files to read out the Factory annotation.
     *
     * @throws IOException
     */
    @SuppressWarnings("synthetic-access")
    private void findExistingFactories() throws IOException {
        this.getLog().info("Finding existing factories");
        CompilationUnit cu;
        for (final File f : this.sourceHandlerFolder.listFiles()) {
            try {
                cu = JavaParser.parse(f);
                new AnnotationVisitor().visit(cu, f);
            } catch (final ParseException e) {
                this.getLog().info("File " + f.getAbsolutePath() + " could not be parsed, file excluded");
            }
        }
        this.getLog().info("Existing factories: " + this.existingFactories);
    }

    /**
     * @author Maarten Kollenstart Extract annotation from a
     *         ClassOrInterfaceDeclaration object
     */
    private class AnnotationVisitor extends VoidVisitorAdapter<File> {

        @Override
        @SuppressWarnings("synthetic-access")
        public void visit(final ClassOrInterfaceDeclaration n, final File file) {
            if (n.getAnnotations() != null) {
                for (final AnnotationExpr annotation : n.getAnnotations()) {
                    if (annotation instanceof NormalAnnotationExpr) {
                        final NormalAnnotationExpr normalAnnotation = (NormalAnnotationExpr) annotation;
                        if (normalAnnotation.getPairs() != null) {
                            for (final MemberValuePair p : normalAnnotation.getPairs()) {
                                CreateComponentMojo.this.existingFactories
                                        .add(p.getValue().toString().replace("\"", ""));
                            }
                        }
                    }
                }
            }
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
    private void createStubs(final List<Interface> interfaces) throws IOException {
        this.getLog().info("Creating stubs");
        final Path serviceImpl = Paths.get(this.sourceFolder.getPath() + "/ServiceImpl.java");
        serviceImpl.toFile().delete();
        Files.write(serviceImpl,
                this.templates.parseServiceImplementation(interfaces).getBytes(),
                StandardOpenOption.CREATE);

        for (final Interface i : interfaces) {
            if (!this.existingFactories.contains(i.getName())) {
                final File factory = new File(
                        this.sourceFolder.getPath() + "/handlers/" + i.getClassPrefix() + "Factory.java");
                final File subscribeHandler = new File(
                        this.sourceFolder.getPath() + "/handlers/" + i.getClassPrefix() + "SubscribeHandler.java");
                final File publishHandler = new File(
                        this.sourceFolder.getPath() + "/handlers/" + i.getClassPrefix() + "PublishHandler.java");
                try {
                    Files.write(factory.toPath(),
                            this.templates.parseFactory(i).getBytes(),
                            StandardOpenOption.CREATE_NEW);
                    if (i.getSubscribe() != null) {
                        Files.write(subscribeHandler.toPath(),
                                this.templates.parseSubscribeHandler(i).getBytes(),
                                StandardOpenOption.CREATE_NEW);
                    }
                    if (i.getPublish() != null) {
                        Files.write(publishHandler.toPath(),
                                this.templates.parsePublishHandler(i).getBytes(),
                                StandardOpenOption.CREATE_NEW);
                    }
                } catch (final IOException e) {
                    this.getLog().info("Could not create stubs for interface " + i.getName());
                }
            } else {
                this.getLog().info("Factory for " + i.getName() + " already found");
            }
        }
    }

    /**
     * Create the Dockerfile
     *
     * @param service
     *            The current Service object
     * @throws IOException
     */
    private void createDockerfile(final Service service) throws IOException {
        this.getLog().info("Creating Dockerfiles");
        Files.write(Paths.get(this.dockerFolder.getPath() + "/Dockerfile"),
                this.templates.parseDockerfile("x86", service).getBytes(),
                StandardOpenOption.CREATE);
        Files.write(Paths.get(this.dockerARMFolder.getPath() + "/Dockerfile"),
                this.templates.parseDockerfile("arm", service).getBytes(),
                StandardOpenOption.CREATE);
    }

    /**
     * Helper function to convert a file into a SHA256 hash
     *
     * @param file
     *            File to be hashed
     * @return
     */
    private String fileToSHA256(final File file) {
        this.getLog().info("Hashing file");
        try (final FileInputStream fis = new FileInputStream(file)) {
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
            fis.close();
            return sb.toString();
        } catch (final NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (final FileNotFoundException e) {
            e.printStackTrace();
        } catch (final IOException e) {
            e.printStackTrace();
        }
        return "";
    }
}
