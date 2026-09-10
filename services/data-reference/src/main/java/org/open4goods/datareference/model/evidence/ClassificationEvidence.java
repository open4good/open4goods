package org.open4goods.datareference.model.evidence;

import java.util.Objects;

import org.open4goods.datareference.model.LanguageTag;

/**
 * Membership the provider declared in its own taxonomy.
 *
 * <p>The scheme is recorded alongside the code because a bare code is not an
 * identity: Icecat category 1234 and an EPREL product group both serialize as
 * digits and mean nothing in each other's taxonomy. Mapping to an O4G canonical
 * class happens downstream and never overwrites what the provider said.
 *
 * @param scheme provider taxonomy identifier
 * @param code class code inside that taxonomy, as written
 * @param label provider label for the class, or {@code null}
 * @param language language of the label, {@code und} when absent
 */
public record ClassificationEvidence(String scheme, String code, String label, LanguageTag language)
        implements SourceEvidence {

    /**
     * Validates the classification reference.
     */
    public ClassificationEvidence {
        scheme = require(scheme, "scheme");
        code = require(code, "code");
        Objects.requireNonNull(language, "language must not be null");
        if (label != null && label.isBlank()) {
            throw new IllegalArgumentException("label must be absent rather than blank");
        }
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
        return SourceEvidenceKind.CLASSIFICATION;
    }
}
