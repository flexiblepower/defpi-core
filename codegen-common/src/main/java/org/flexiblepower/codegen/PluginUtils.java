/*-
 * #%L
 * dEF-Pi service codegen-common
 * %%
 * Copyright (C) 2017 - 2018 Flexible Power Alliance Network
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
 * @version 0.1
 * @since Jun 8, 2017
 */
@Slf4j
public class PluginUtils {

    /**
     * Reads and parses the service json into a ServiceDescription object
     *
     * @param inputFile The file that contains the service description in JSON definition
     * @return ServiceDescription object containing the data of the json
     * @throws FileNotFoundException
     *             service.yml is not found in the resource directory
     * @throws JsonParseException
     *             file could not be parsed as json file
     * @throws JsonMappingException
     *             Json could not be mapped to a ServiceDescription
     * @throws IOException When any other IOException occurs during processing the service definition file
     */
    public static ServiceDescription readServiceDefinition(final File inputFile) throws IOException {
        PluginUtils.log.info(String.format("Reading service definition from %s", inputFile));

        final ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(inputFile, ServiceDescription.class);
    }

    /**
     * Validate a service definition file by checking the JSON to a json schema file.
     *
     * @param inputFile The file containing the service description to validate
     * @return A boolean stating wether the service definition in the file is valid or not
     * @throws ProcessingException When an exception occurs during processing
     */
    public static boolean validateServiceDefinition(final File inputFile) throws ProcessingException {
        try {
            final ProcessingReport report = PluginUtils.processServiceDefinition(inputFile);

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
     * Create the processing report on a service definition file by checking the JSON to a json schema file.
     *
     * @param inputFile The file containing the service description to validate
     * @return A report on the validity of the input file
     * @throws IOException When the JSON schema file cannot be read
     * @throws ProcessingException When an exception occurs during processing
     * @see JsonSchema#validate(JsonNode)
     */
    public static ProcessingReport processServiceDefinition(final File inputFile) throws IOException,
            ProcessingException {
        final JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
        final URL schemaURL = PluginUtils.class.getClassLoader().getResource("schema.json");

        final JsonNode schemaNode = JsonLoader.fromURL(schemaURL);
        final JsonNode data = JsonLoader.fromFile(inputFile);

        final JsonSchema schema = factory.getJsonSchema(schemaNode);
        return schema.validate(data);
    }

    /**
     * Transform a string into its camelcase equivalent. i.e. it will remove all non alphanumeric characters from the
     * string, then turn the first letter of words into uppercase and the rest of the letters into lowercase.
     * <p>
     * Examples:
     * "This is a sentence" becomes "ThisIsASentence"
     * "**The record**: 54meters" becomes "TheRecord54meters"
     *
     * @param str The string to transform into camel caps
     * @return A string in camel caps
     */
    public static String camelCaps(final String str) {
        final StringBuilder ret = new StringBuilder();

        for (final String word : str.replaceAll("[^a-zA-Z0-9_ ]", "").split(" ")) {
            if (!word.isEmpty()) {
                ret.append(Character.toUpperCase(word.charAt(0)));
                ret.append(word.substring(1).toLowerCase());
            }
        }

        // Return a cleaned-up string
        return ret.toString();
    }

    /**
     * Transform a string into its snakecase equivalent. i.e. it will remove all non alphanumeric characters from the
     * string, and then concatenate all remaining words that were separated by spaces, with underscores in between.
     * <p>
     * Examples:
     * "This is a sentence" becomes "this_is_a_sentence"
     * "**The record**: 54meters" becomes "the_record_54meters"
     *
     * @param str The string to transform into camel caps
     * @return A string in camel caps
     */
    public static String snakeCaps(final String str) {
        return Stream.of(str.split(" "))
                .map(String::toLowerCase)
                .collect(Collectors.joining("_"))
                .replaceAll("[^a-zA-Z0-9_]", "");
    }

    /**
     * Simply replace the first letter by a capital letter. This is useful when a resource or type needs to be
     * transformed to a proper class name.
     *
     * @param str The string to capitalize
     * @return The same string with the first letter capitalized
     */
    public static String capitalize(final String str) {
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    /**
     * Compute the outgoing dEF-Pi hash of a specific interface version.
     *
     * @param vitf The versioned interface description to compute the hash of.
     * @return A hash code that identifies the outgoing messages for this specific interface version
     */
    public static String getSendHash(final InterfaceVersionDescription vitf) {
        return PluginUtils.getHash(vitf, vitf.getSends());
    }

    /**
     * Compute the incoming dEF-Pi hash of a specific interface version.
     *
     * @param vitf The versioned interface description to compute the hash of.
     * @return A hash code that identifies the incoming messages for this specific interface version
     */
    public static String getReceiveHash(final InterfaceVersionDescription vitf) {
        return PluginUtils.getHash(vitf, vitf.getReceives());
    }

    private static String getHash(final InterfaceVersionDescription vitf, final Set<String> messageSet) {
        String baseHash = vitf.getHash();
        if ((messageSet != null) && !messageSet.isEmpty()) {
            baseHash += ";" + String.join(";", messageSet);
        }
        return PluginUtils.SHA256(baseHash);
    }

    /**
     * Compute the SHA256 hash of a particular String by first obtaining the bytes of the UTF-8 encoding of the string,
     * and then computing
     *
     * @param body The string to compute the hash of
     * @return The SHA256 hash
     * @see MessageDigest
     */
    public static String SHA256(final String body) {
        return PluginUtils.SHA256(body.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Compute the SHA256 hash of the file at a particular path
     *
     * @param path The path of a file to compute the hash of
     * @return The SHA256 hash of the file
     * @throws IOException If an exception occurs while reading from the file
     * @see MessageDigest
     */
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
     * Download a file to a particular destination
     *
     * @param src The string representation of the URL to download
     * @param dst The destination file to download to
     * @throws IOException When any exception occurs while reading from the URL, or to the destination file
     */
    public static void downloadFile(final URL src, final File dst) throws IOException {
        System.out.println("Downloading " + src + " to " + dst);

        try (
                final InputStream in = new BufferedInputStream(src.openStream());
                final FileOutputStream out = new FileOutputStream(dst)) {
            final byte[] buf = new byte[1024];
            int n = 0;
            while (-1 != (n = in.read(buf))) {
                out.write(buf, 0, n);
            }
        }
    }

    /**
     * Download the file from the specified location, or resolve it from the resource path if it is a local resource.
     *
     * @param location The location where to find the file
     * @param resources The path to resolve local resources from
     * @return The path to the file where the resulting file can be found
     * @throws FileNotFoundException When neither the url can be resolved, nor the local path can be found
     */
    public static Path downloadFileOrResolve(final String location, final Path resources) throws FileNotFoundException {
        try {
            final URL url = new URL(location);
            final Path tempFile = Files.createTempFile(null, null);
            PluginUtils.downloadFile(url, tempFile.toFile());
            return tempFile;
        } catch (final IOException e) {
            final Path ret = resources.resolve(location);
            if (!ret.toFile().exists()) {
                throw new FileNotFoundException("Unable to get file from " + location);
            } else {
                return ret;
            }
        }
    }

}
