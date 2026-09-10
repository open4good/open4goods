package org.open4goods.datareference.model.evidence;

import java.util.Objects;

import org.open4goods.datareference.model.LanguageTag;

/**
 * One provider value kept exactly as the provider wrote it.
 *
 * <p>Brand and model are scalar evidence: they are single provider tokens whose
 * spelling is itself the evidence, not prose to be translated.
 *
 * <p>{@code lexicalValue} is never trimmed, case-folded or unit-converted here.
 * Leading zeroes, thousands separators and provider unit spellings such as
 * {@code "cm"} or {@code "centimetres"} are the raw material normalization
 * needs, and a store that loses them cannot be replayed.
 *
 * @param lexicalValue provider value, unmodified
 * @param lexicalUnit provider unit as written, or {@code null} when the provider stated none
 * @param language language of the value, {@code und} when non-linguistic
 */
public record ScalarEvidence(String lexicalValue, String lexicalUnit, LanguageTag language)
        implements SourceEvidence {

    /**
     * Validates the evidence without altering provider spelling.
     */
    public ScalarEvidence {
        Objects.requireNonNull(lexicalValue, "lexicalValue must not be null");
        Objects.requireNonNull(language, "language must not be null");
        if (lexicalUnit != null && lexicalUnit.isBlank()) {
            throw new IllegalArgumentException("lexicalUnit must be absent rather than blank");
        }
    }

    /**
     * Builds non-linguistic scalar evidence without a unit.
     *
     * @param lexicalValue provider value
     * @return scalar evidence tagged {@code und}
     */
    public static ScalarEvidence of(String lexicalValue) {
        return new ScalarEvidence(lexicalValue, null, LanguageTag.UND);
    }

    @Override
    public SourceEvidenceKind kind() {
        return SourceEvidenceKind.SCALAR;
    }
}
