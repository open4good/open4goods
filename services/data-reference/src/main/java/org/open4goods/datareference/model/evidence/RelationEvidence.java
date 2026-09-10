package org.open4goods.datareference.model.evidence;

import java.util.Objects;

import org.open4goods.datareference.model.LanguageTag;

/**
 * A link the provider declared to another product, model or family.
 *
 * <p>The target is described by scheme and identifier rather than typed as a
 * GTIN, because a provider frequently relates records by its own internal id or
 * by a manufacturer model reference. Resolving the target to a GTIN is a later,
 * versioned decision that must not be baked into the evidence.
 *
 * @param relationType provider relation such as a variant or accessory link
 * @param targetScheme identifier scheme of the target
 * @param targetIdentifier target identifier inside that scheme, as written
 * @param language language of any embedded label, {@code und} when none
 */
public record RelationEvidence(
        String relationType,
        String targetScheme,
        String targetIdentifier,
        LanguageTag language) implements SourceEvidence {

    /**
     * Validates the relation.
     */
    public RelationEvidence {
        relationType = require(relationType, "relationType");
        targetScheme = require(targetScheme, "targetScheme");
        targetIdentifier = require(targetIdentifier, "targetIdentifier");
        Objects.requireNonNull(language, "language must not be null");
    }

    private static String require(String value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return trimmed;
    }

    @Override
    public SourceEvidenceKind kind() {
        return SourceEvidenceKind.RELATION;
    }
}
