package org.open4goods.datareference.model;

import java.util.Objects;

/**
 * Versioned provider field identity without provider-specific fields in the common model.
 *
 * @param namespace provider or schema namespace
 * @param key provider field identifier
 * @param version provider schema version
 */
public record SourceFieldId(String namespace, String key, String version) {

    /**
     * Rejects incomplete source identities while preserving provider spelling.
     */
    public SourceFieldId {
        namespace = requireText(namespace, "namespace");
        key = requireText(key, "key");
        version = requireText(version, "version");
    }

    /**
     * Returns the stable serialized form of this field identity.
     *
     * @return identifier such as {@code icecat:1234:v2}
     */
    public String externalForm() {
        return namespace + ":" + key + ":" + version;
    }

    @Override
    public String toString() {
        return externalForm();
    }

    /**
     * Validates a required textual component.
     *
     * @param value component value
     * @param name component name for error reporting
     * @return trimmed component value
     */
    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }
}
