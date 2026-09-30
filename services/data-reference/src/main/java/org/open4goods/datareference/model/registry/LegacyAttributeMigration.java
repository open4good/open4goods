package org.open4goods.datareference.model.registry;

import java.util.Objects;
import java.util.regex.Pattern;

import org.open4goods.datareference.model.CanonicalAttributeId;

/**
 * One reviewable disposition from a legacy vertical attribute file to O4G.
 *
 * @param legacyResource repository-relative legacy attribute resource
 * @param status reviewed migration disposition
 * @param replacement stable O4G attribute that replaces the legacy setting
 * @param rationale concise reason the disposition is correct
 */
public record LegacyAttributeMigration(
        String legacyResource,
        LegacyAttributeMigrationStatus status,
        CanonicalAttributeId replacement,
        String rationale) {

    private static final Pattern LEGACY_ATTRIBUTE_RESOURCE = Pattern.compile(
            "verticals/src/main/resources/attributes/[A-Z0-9_]+\\.yml");

    /**
     * Validates a repository-local legacy path and its complete reviewed disposition.
     */
    public LegacyAttributeMigration {
        Objects.requireNonNull(legacyResource, "legacyResource must not be null");
        if (!LEGACY_ATTRIBUTE_RESOURCE.matcher(legacyResource).matches()) {
            throw new IllegalArgumentException("legacyResource must name one legacy attribute YAML file: "
                    + legacyResource);
        }
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(replacement, "replacement must not be null");
        Objects.requireNonNull(rationale, "rationale must not be null");
        rationale = rationale.trim();
        if (rationale.isEmpty()) {
            throw new IllegalArgumentException("rationale must not be blank");
        }
    }
}
