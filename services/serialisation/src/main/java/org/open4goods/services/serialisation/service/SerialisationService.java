package org.open4goods.services.serialisation.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.open4goods.services.serialisation.exception.SerialisationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

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

	private Yaml yaml;

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
        YAMLFactory yamlFactory = new YAMLFactory()
        	    .disable(YAMLGenerator.Feature.SPLIT_LINES);
        this.yamlMapper =  new ObjectMapper(yamlFactory)
        	    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        	    .setSerializationInclusion(Include.NON_EMPTY)
        	    .enable(SerializationFeature.INDENT_OUTPUT)
                .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        	
        	
        // Low level yaml for literals rendering
        DumperOptions options = new DumperOptions();
        // Use literal style (|) so that "\n" is rendered as a multiline block.
        options.setDefaultScalarStyle(DumperOptions.ScalarStyle.LITERAL);
        options.setPrettyFlow(true);
        options.setSplitLines(false);
        
        options.setIndent(2);
        this.yaml = new Yaml(options);
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

    public String toYamLiteral(final Object o) throws SerialisationException {
        try {
  
            return yaml.dump(o);
        } catch (Exception e) {
            logger.error("Error serializing {} to YAML using SnakeYAML", o, e);
            throw new SerialisationException("Error serializing object to YAML using SnakeYAML", e);
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
