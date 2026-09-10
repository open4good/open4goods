package org.open4goods.datareference.model.evidence;

import java.util.Objects;

import org.open4goods.datareference.model.LanguageTag;

/**
 * Provider prose in a stated language: a name, a description, marketing copy.
 *
 * <p>Distinct from {@link ScalarEvidence} because the same product text in two
 * languages is two observations of the same coordinate, not a conflict to be
 * resolved.
 *
 * @param text provider text, unmodified
 * @param language language the provider stated, {@code und} when it stated none
 */
public record LocalizedTextEvidence(String text, LanguageTag language) implements SourceEvidence {

    /**
     * Validates the text and its explicit language.
     */
    public LocalizedTextEvidence {
        Objects.requireNonNull(text, "text must not be null");
        Objects.requireNonNull(language, "language must not be null");
    }

    @Override
    public SourceEvidenceKind kind() {
        return SourceEvidenceKind.LOCALIZED_TEXT;
    }
}
