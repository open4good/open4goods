* Directory structure *
- pom.xml
- src
--- main
---- java
----- org
------ open4goods
------- services
-------- serialisation
--------- config
--------- dto
--------- exception
---------- SerialisationException.java
--------- service
---------- SerialisationService.java
---- resources
--- test
---- java
----- org
------ open4goods
------- services
-------- serialisation
--------- service
---------- SerialisationServiceTest.java
---- resources
----- application-test.yml

* Files content *

** [services/serialisation//pom.xml] **
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <parent>
    <groupId>org.open4goods</groupId>
    <artifactId>org.open4goods</artifactId>
    <version>0.0.1-SNAPSHOT</version>
  </parent>
  <artifactId>serialisation</artifactId>
  
  <dependencies>
    <dependency>
      <groupId>com.fasterxml.jackson.dataformat</groupId>
      <artifactId>jackson-dataformat-yaml</artifactId>
    </dependency>
        
    <dependency>
      <groupId>commons-codec</groupId>
      <artifactId>commons-codec</artifactId>
    </dependency>

    <!-- Spring Boot Test dependencies -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-test</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>
</project>

** [services/serialisation//src/main/java/org/open4goods/services/serialisation/service/SerialisationService.java] **
package org.open4goods.services.serialisation.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.open4goods.services.serialisation.exception.SerialisationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import org.apache.commons.codec.binary.Base64;

/**
 * Service for serializing and deserializing objects to/from JSON and YAML formats.
 * Provides methods for Base64 encoding/decoding of string content.
 *
 * <p>This service is part of the Open4Goods project.</p>
 */
@Service
public class SerialisationService {

    private static final Logger logger = LoggerFactory.getLogger(SerialisationService.class);

    private final ObjectMapper jsonMapper;
    private final ObjectWriter jsonMapperWithPrettyPrint;
    private final ObjectMapper yamlMapper;

    /**
     * Constructor initializing JSON and YAML ObjectMappers with desired configurations.
     */
    public SerialisationService() {
        // Initialize JSON mapper with NON_EMPTY inclusion and disable failure on unknown properties.
        this.jsonMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .setSerializationInclusion(Include.NON_EMPTY);
        
        // Create a pretty-print writer for JSON.
        this.jsonMapperWithPrettyPrint = new ObjectMapper().writerWithDefaultPrettyPrinter();
        
        // Initialize YAML mapper with NON_EMPTY inclusion and pretty output.
        this.yamlMapper = new ObjectMapper(new YAMLFactory().disable(YAMLGenerator.Feature.SPLIT_LINES))
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .setSerializationInclusion(Include.NON_EMPTY)
                .enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * Serializes an object to its JSON representation.
     *
     * @param o the object to serialize
     * @return JSON string representation of the object
     * @throws SerialisationException if serialization fails
     */
    public String toJson(final Object o) throws SerialisationException {
        try {
            return jsonMapper.writeValueAsString(o);
        } catch (final JsonProcessingException e) {
            logger.error("Error serializing {} to JSON", o, e);
            throw new SerialisationException("Error serializing object to JSON", e);
        }
    }

    /**
     * Serializes an object to its YAML representation.
     *
     * @param o the object to serialize
     * @return YAML string representation of the object
     * @throws SerialisationException if serialization fails
     */
    public String toYaml(final Object o) throws SerialisationException {
        try {
            return yamlMapper.writeValueAsString(o);
        } catch (final JsonProcessingException e) {
            logger.error("Error serializing {} to YAML", o, e);
            throw new SerialisationException("Error serializing object to YAML", e);
        }
    }

    /**
     * Serializes an object to its JSON representation with an option for pretty printing.
     *
     * @param o the object to serialize
     * @param prettyPrint if true, output will be pretty printed
     * @return JSON string representation of the object
     * @throws SerialisationException if serialization fails
     */
    public String toJson(final Object o, final boolean prettyPrint) throws SerialisationException {
        if (prettyPrint) {
            try {
                return jsonMapperWithPrettyPrint.writeValueAsString(o);
            } catch (final JsonProcessingException e) {
                logger.error("Error serializing {} to JSON with pretty print", o, e);
                throw new SerialisationException("Error serializing object to JSON with pretty print", e);
            }
        } else {
            return toJson(o);
        }
    }

    /**
     * Creates a deep clone of an object using JSON serialization.
     *
     * @param <T> the type of the object
     * @param value the object to clone
     * @return a deep clone of the object
     * @throws SerialisationException if cloning fails
     */
    public <T> T clone(final T value) throws SerialisationException {
        try {
            String json = toJson(value);
            @SuppressWarnings("unchecked")
            T clonedObject = (T) jsonMapper.readValue(json, value.getClass());
            return clonedObject;
        } catch (final IOException e) {
            logger.error("Error cloning object: {}", value, e);
            throw new SerialisationException("Error cloning object", e);
        }
    }

    /**
     * Deserializes a JSON string into an object of the specified type.
     *
     * @param <T> the type of the object
     * @param input the JSON string
     * @param valueType the target class
     * @return deserialized object
     * @throws SerialisationException if deserialization fails
     */
    public <T> T fromJson(final String input, final Class<T> valueType) throws SerialisationException {
        try {
            return jsonMapper.readValue(input, valueType);
        } catch (final IOException e) {
            logger.error("Error deserializing JSON to {}", valueType, e);
            throw new SerialisationException("Error deserializing JSON", e);
        }
    }

    /**
     * Deserializes a JSON string into a Map of String keys and values.
     *
     * @param value the JSON string
     * @param typeRef the type reference for the map
     * @return deserialized map
     * @throws SerialisationException if deserialization fails
     */
    public Map<String, String> fromJson(String value, TypeReference<HashMap<String, String>> typeRef) throws SerialisationException {
        try {
            return jsonMapper.readValue(value, typeRef);
        } catch (final IOException e) {
            logger.error("Error deserializing JSON to HashMap<String, String>", e);
            throw new SerialisationException("Error deserializing JSON to HashMap<String, String>", e);
        }
    }

    /**
     * Deserializes a JSON string into a Map of String keys and Object values.
     *
     * @param value the JSON string
     * @param typeRef the type reference for the map
     * @return deserialized map
     * @throws SerialisationException if deserialization fails
     */
    public Map<String, Object> fromJsonTypeRef(String value, TypeReference<Map<String, Object>> typeRef) throws SerialisationException {
        try {
            return jsonMapper.readValue(value, typeRef);
        } catch (final IOException e) {
            logger.error("Error deserializing JSON to Map<String, Object>", e);
            throw new SerialisationException("Error deserializing JSON to Map<String, Object>", e);
        }
    }

    /**
     * Deserializes a YAML string into an object of the specified type.
     *
     * @param <T> the type of the object
     * @param input the YAML string
     * @param valueType the target class
     * @return deserialized object
     * @throws SerialisationException if deserialization fails
     */
    public <T> T fromYaml(final String input, final Class<T> valueType) throws SerialisationException {
        try {
            return yamlMapper.readValue(input, valueType);
        } catch (final IOException e) {
            logger.error("Error deserializing YAML to {}", valueType, e);
            throw new SerialisationException("Error deserializing YAML", e);
        }
    }

    /**
     * Deserializes a YAML string into an object using the specified collection type.
     *
     * @param <T> the type of the object
     * @param input the YAML string
     * @param collectionType the collection type reference
     * @return deserialized object
     * @throws SerialisationException if deserialization fails
     */
    public <T> T fromYaml(final String input, final CollectionType collectionType) throws SerialisationException {
        try {
            return yamlMapper.readValue(input, collectionType);
        } catch (final IOException e) {
            logger.error("Error deserializing YAML to collection type {}", collectionType, e);
            throw new SerialisationException("Error deserializing YAML to collection type", e);
        }
    }

    /**
     * Deserializes YAML content from an InputStream into an object of the specified type.
     *
     * @param <T> the type of the object
     * @param input the InputStream containing YAML content
     * @param valueType the target class
     * @return deserialized object
     * @throws SerialisationException if deserialization fails
     */
    public <T> T fromYaml(final InputStream input, final Class<T> valueType) throws SerialisationException {
        try {
            return yamlMapper.readValue(input, valueType);
        } catch (final IOException e) {
            logger.error("Error deserializing YAML InputStream to {}", valueType, e);
            throw new SerialisationException("Error deserializing YAML InputStream", e);
        }
    }

    /**
     * Deserializes a binary JSON representation into an object of the specified type.
     *
     * @param <T> the type of the object
     * @param bytes the byte array containing JSON data
     * @param c the target class
     * @return deserialized object
     * @throws SerialisationException if deserialization fails
     */
    public <T> T fromBytes(final byte[] bytes, final Class<T> c) throws SerialisationException {
        try {
            return jsonMapper.readValue(bytes, c);
        } catch (final IOException e) {
            logger.error("Error deserializing bytes to {}", c, e);
            throw new SerialisationException("Error deserializing bytes", e);
        }
    }

    /**
     * Serializes an object into its binary JSON representation.
     *
     * @param o the object to serialize
     * @return byte array representing the serialized object
     * @throws SerialisationException if serialization fails
     */
    public byte[] toBytes(final Object o) throws SerialisationException {
        return toJson(o).getBytes();
    }

    /**
     * Retrieves the underlying JSON ObjectMapper.
     *
     * @return the JSON ObjectMapper
     */
    public ObjectMapper jsonMapper() {
        return jsonMapper;
    }

    /**
     * Retrieves the underlying YAML ObjectMapper.
     *
     * @return the YAML ObjectMapper
     */
    public ObjectMapper getYamlMapper() {
        return yamlMapper;
    }

    /**
     * Compresses a string by encoding it using Base64.
     * <p>
     * Note: This method uses Base64 encoding only and does not perform actual compression.
     * </p>
     *
     * @param srcTxt the source text to compress
     * @return Base64 encoded string
     * @throws SerialisationException if encoding fails
     */
    public String compressString(final String srcTxt) throws SerialisationException {
        try {
            return Base64.encodeBase64String(srcTxt.getBytes());
        } catch (final Exception e) {
            logger.error("Error compressing string", e);
            throw new SerialisationException("Error compressing string", e);
        }
    }

    /**
     * Decompresses a Base64 encoded string.
     * <p>
     * Note: This method uses Base64 decoding only and does not perform actual decompression.
     * </p>
     *
     * @param zippedBase64Str the Base64 encoded string to decompress
     * @return the decompressed string
     * @throws SerialisationException if decoding fails
     */
    public static String uncompressString(final String zippedBase64Str) throws SerialisationException {
        try {
            return new String(Base64.decodeBase64(zippedBase64Str));
        } catch (final Exception e) {
            logger.error("Error uncompressing string", e);
            throw new SerialisationException("Error uncompressing string", e);
        }
    }
}

** [services/serialisation//src/main/java/org/open4goods/services/serialisation/exception/SerialisationException.java] **
package org.open4goods.services.serialisation.exception;

/**
 * Custom exception class for handling serialization and deserialization errors.
 */
public class SerialisationException extends Exception {

    private static final long serialVersionUID = 1453990440096206895L;

	/**
     * Constructs a new SerialisationException with the specified detail message.
     *
     * @param message the detail message
     */
    public SerialisationException(String message) {
        super(message);
    }

    /**
     * Constructs a new SerialisationException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause
     */
    public SerialisationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new SerialisationException with the specified cause.
     *
     * @param cause the cause
     */
    public SerialisationException(Throwable cause) {
        super(cause);
    }
}

** [services/serialisation//src/test/resources/application-test.yml] **
urlfetcher:
  domains:
    "localhost":
      userAgent: "TestAgent/1.0"
      strategy: "SELENIUM"
      customHeaders:
        Accept: "application/json"
      timeout: 5000
      retryPolicy:
        maxRetries: 1
        delayBetweenRetries: 500

** [services/serialisation//src/test/java/org/open4goods/services/serialisation/service/SerialisationServiceTest.java] **
package org.open4goods.services.serialisation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.open4goods.services.serialisation.exception.SerialisationException;

/**
 * Unit tests for {@link SerialisationService}.
 */
public class SerialisationServiceTest {

    private final SerialisationService serialisationService = new SerialisationService();




    @Test
    public void testJsonSerializationDeserialization() {
        try {
			Map<String, String> sample = Map.of("key", "value");
			String json = serialisationService.toJson(sample);
			assertNotNull(json, "JSON serialization returned null");
			Map<?, ?> result = serialisationService.fromJson(json, Map.class);
			assertEquals(sample, result, "Deserialized JSON does not match original");
		} catch (SerialisationException e) {
			fail(e);
		}
    }

    @Test
    public void testYamlSerializationDeserialization() {
        try {
			Map<String, String> sample = Map.of("key", "value");
			String yaml = serialisationService.toYaml(sample);
			assertNotNull(yaml, "YAML serialization returned null");
			Map<?, ?> result = serialisationService.fromYaml(yaml, Map.class);
			assertEquals(sample, result, "Deserialized YAML does not match original");
		} catch (SerialisationException e) {
			fail(e);
		}
    }

    @Test
    public void testPrettyPrintJson() {
        try {
			Map<String, String> sample = Map.of("key", "value");
			String prettyJson = serialisationService.toJson(sample, true);
			assertNotNull(prettyJson, "Pretty printed JSON returned null");
			// Heuristic check for pretty printing (line breaks/indentation)
			assertTrue(prettyJson.contains("\n"), "JSON output is not pretty printed");
		} catch (SerialisationException e) {
			fail(e);
		}
    }

    @Test
    public void testClone() {
        try {
			Map<String, String> sample = Map.of("key", "value");
			Map<String, String> clone = serialisationService.clone(sample);
			assertEquals(sample, clone, "Cloned object does not match original");
			assertNotSame(sample, clone, "Cloned object is the same instance as original");
		} catch (SerialisationException e) {
			fail(e);
		}
    }

    @Test
    public void testToBytesAndFromBytes() {
        try {
			Map<String, String> sample = Map.of("key", "value");
			byte[] bytes = serialisationService.toBytes(sample);
			assertNotNull(bytes, "Binary serialization returned null");
			Map<?, ?> result = serialisationService.fromBytes(bytes, Map.class);
			assertEquals(sample, result, "Deserialized binary object does not match original");
		} catch (SerialisationException e) {
			fail(e);
		}
    }

    @Test
    public void testCompressAndUncompress() {
        try {
			String original = "This is a test string.";
			String compressed = serialisationService.compressString(original);
			assertNotNull(compressed, "Compression returned null");
			String uncompressed = SerialisationService.uncompressString(compressed);
			assertEquals(original, uncompressed, "Uncompressed string does not match original");
		} catch (SerialisationException e) {
			fail(e);
		}
    }

    @Test
    public void testInvalidJsonDeserialization() {
        String invalidJson = "{invalid json}";
        assertThrows(SerialisationException.class, () -> {
            serialisationService.fromJson(invalidJson, Map.class);
        }, "Expected SerialisationException for invalid JSON input");
    }
}

