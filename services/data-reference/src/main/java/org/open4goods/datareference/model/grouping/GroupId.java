package org.open4goods.datareference.model.grouping;

import java.util.Objects;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Stable identifier of a model or family group.
 *
 * <p>Distinct from {@link org.open4goods.datareference.model.CanonicalConceptId}:
 * a group is a derived runtime grouping of GTIN leaves, not an independently
 * authored O4G concept. A merge keeps one canonical {@code GroupId} and aliases
 * the predecessors; a split allocates new ids. Group ids are never reused for a
 * different member set.
 *
 * @param type model or family
 * @param slug lower-case kebab-case slug, unique within its type
 */
public record GroupId(GroupType type, String slug) {

    private static final Pattern SLUG_PATTERN = Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");

    /** Validates the group identifier. */
    public GroupId {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(slug, "slug must not be null");
        if (!SLUG_PATTERN.matcher(slug).matches()) {
            throw new IllegalArgumentException("group slug must be lower-case kebab-case: " + slug);
        }
    }

    /**
     * Returns the stable serialized form.
     *
     * @return identifier such as {@code model:sony-tv-xr500} or {@code family:sony-tv-xr}
     */
    @JsonValue
    public String externalForm() {
        return type.name().toLowerCase() + ":" + slug;
    }

    /**
     * Rebuilds a group identifier from its serialized form.
     *
     * @param value serialized form such as {@code model:sony-tv-xr500}
     * @return validated identifier
     */
    @JsonCreator
    public static GroupId parse(String value) {
        Objects.requireNonNull(value, "value must not be null");
        int separator = value.indexOf(':');
        if (separator < 0) {
            throw new IllegalArgumentException("group id must be <type>:<slug>: " + value);
        }
        String rawType = value.substring(0, separator);
        GroupType type;
        try {
            type = GroupType.valueOf(rawType.toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("unsupported group type: " + rawType, exception);
        }
        return new GroupId(type, value.substring(separator + 1));
    }

    @Override
    public String toString() {
        return externalForm();
    }
}
