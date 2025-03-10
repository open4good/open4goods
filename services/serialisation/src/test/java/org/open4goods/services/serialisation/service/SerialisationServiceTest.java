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
    public void testSnakeYamlMultilineSerialization() {
        try {
            Map<String, String> sample = Map.of("key", "line1\nline2\nline3");
            String yamlOutput = serialisationService.toYamLiteral(sample);
            assertNotNull(yamlOutput, "SnakeYAML serialization returned null");
            // Check that the YAML output uses the literal block scalar indicator (|).
            assertTrue(yamlOutput.contains("|"), "YAML output does not use literal block scalar style");
            // Verify that the content is correctly rendered in multiple lines.
            assertTrue(yamlOutput.contains("line1"), "YAML output missing 'line1'");
            assertTrue(yamlOutput.contains("line2"), "YAML output missing 'line2'");
            assertTrue(yamlOutput.contains("line3"), "YAML output missing 'line3'");
        } catch (SerialisationException e) {
            fail(e);
        }
    }


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
