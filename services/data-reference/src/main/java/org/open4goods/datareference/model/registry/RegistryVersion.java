package org.open4goods.datareference.model.registry;

import java.util.Objects;

/**
 * Version of the authored O4G registry a projection was built against.
 *
 * <p>Stored on every projection so that a document can be explained: a value
 * that no longer matches today's registry was correct under the version that
 * produced it, and that is a rebuild, not a bug.
 *
 * @param value monotonic registry version
 */
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public record RegistryVersion(@JsonValue int value) {

    /**
     * Validates the version.
     */
    public RegistryVersion {
        if (value < 1) {
            throw new IllegalArgumentException("registry version must be strictly positive");
        }
    }

    /**
     * Rebuilds a registry version from its serialized form.
     *
     * @param value serialized version
     * @return validated registry version
     */
    @JsonCreator
    public static RegistryVersion fromJson(int value) {
        return new RegistryVersion(value);
    }

    @Override
    public String toString() {
        return Integer.toString(value);
    }
}
