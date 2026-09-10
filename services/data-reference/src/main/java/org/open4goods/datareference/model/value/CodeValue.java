package org.open4goods.datareference.model.value;

/**
 * Canonical code in an explicitly named code system.
 *
 * @param namespace code-system namespace
 * @param value code value
 */
public record CodeValue(String namespace, String value) implements CanonicalValue {

    /** Validates the code identity. */
    public CodeValue {
        if (namespace == null || namespace.isBlank() || value == null || value.isBlank()) {
            throw new IllegalArgumentException("code namespace and value must not be blank");
        }
        namespace = namespace.trim();
        value = value.trim();
    }

    @Override
    public CanonicalValueType type() {
        return CanonicalValueType.CODE;
    }
}
