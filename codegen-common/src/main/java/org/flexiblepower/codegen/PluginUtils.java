/**
 * File PluginUtils.java
 *
 * Copyright 2017 TNO
 */
package org.flexiblepower.codegen;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Set;

import org.flexiblepower.codegen.model.InterfaceVersionDescription;
import org.flexiblepower.codegen.model.ServiceDescription;

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

import lombok.extern.slf4j.Slf4j;

/**
 * PluginUtils
 *
 * @author coenvl
 * @version 0.1
 * @since Jun 8, 2017
 */
@Slf4j
public class PluginUtils {

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
    public static ServiceDescription readServiceDefinition(final File inputFile) throws ProcessingException,
            IOException {
        PluginUtils.log.info(String.format("Reading service definition from %s", inputFile));

        final ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(inputFile, ServiceDescription.class);
    }

    /**
     * @param inputFile
     * @throws IOException
     * @throws ProcessingException
     */
    public static boolean validateServiceDefinition(final File inputFile) throws ProcessingException {
        final JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
        final URL schemaURL = PluginUtils.class.getClassLoader().getResource("schema.json");

        try {
            final JsonNode schemaNode = JsonLoader.fromURL(schemaURL);
            final JsonNode data = JsonLoader.fromFile(inputFile);

            final JsonSchema schema = factory.getJsonSchema(schemaNode);
            final ProcessingReport report = schema.validate(data);

            if (report.isSuccess()) {
                return true;
            } else {
                PluginUtils.log.warn("Errors while reading " + inputFile + ":");
                for (final ProcessingMessage m : report) {
                    PluginUtils.log.warn(m.getMessage());
                }
            }
        } catch (final JsonParseException e) {
            PluginUtils.log.warn("Invalid JSON syntax: " + e.getMessage());
        } catch (final IOException e) {
            PluginUtils.log.warn("IOException while reading file: " + e.getMessage());
        }

        return false;
    }

    /**
     * @param i
     * @return
     */
    public static String camelCaps(final String str) {
        final StringBuilder ret = new StringBuilder();

        for (final String word : str.split(" ")) {
            if (!word.isEmpty()) {
                ret.append(Character.toUpperCase(word.charAt(0)));
                ret.append(word.substring(1).toLowerCase());
            }
        }

        // Return a cleaned-up string
        return ret.toString().replaceAll("[^a-zA-Z0-9_]", "");
    }

    public static String getHash(final InterfaceVersionDescription vitf, final Set<String> messageSet) {
        String baseHash = vitf.getHash();
        for (final String key : messageSet) {
            baseHash += ";" + key;
        }
        return PluginUtils.SHA256(baseHash);
    }

    public static String SHA256(final String body) {
        return PluginUtils.SHA256(body.getBytes(StandardCharsets.UTF_8));
    }

    public static String SHA256(final Path path) throws IOException {
        return PluginUtils.SHA256(Files.readAllBytes(path));
    }

    private static String SHA256(final byte[] barr) {
        try {
            final MessageDigest md = MessageDigest.getInstance("SHA-256");
            final byte[] mdbytes = md.digest(barr);
            return String.format("%x", new BigInteger(1, mdbytes));
            // return Base64.getEncoder().encodeToString(mdbytes);
        } catch (final Exception e) {
            throw new RuntimeException("Error computing hash: " + e.getMessage());
        }
    }

    /**
     * @param src
     * @param dst
     */
    public static void downloadFile(final String src, final File dst) throws IOException {
        System.out.println("Downloading " + src + " to " + dst);
        final URL url = new URL(src);

        try (
                final InputStream in = new BufferedInputStream(url.openStream());
                final FileOutputStream out = new FileOutputStream(dst)) {
            final byte[] buf = new byte[1024];
            int n = 0;
            while (-1 != (n = in.read(buf))) {
                out.write(buf, 0, n);
            }
        }
    }

    /**
     * @param location
     * @param resources
     * @return
     */
    public static Path downloadFileOrResolve(final String location, final Path resources) {
        // First get the hash of the input file
        try {
            final Path tempFile = Files.createTempFile(null, null);
            PluginUtils.downloadFile(location, tempFile.toFile());
            return tempFile;
        } catch (final IOException e) {
            return resources.resolve(location);
        }
    }

}
