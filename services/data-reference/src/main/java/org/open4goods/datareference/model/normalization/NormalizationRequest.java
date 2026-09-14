package org.open4goods.datareference.model.normalization;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;

import org.open4goods.datareference.model.SourceAssertion;
import org.open4goods.datareference.model.SourceId;

/** Explicit adapter input for normalizing one immutable source assertion. */
public record NormalizationRequest(
        SourceId sourceId,
        SourceAssertion assertion,
        Locale sourceLocale,
        String providerLanguage,
        LocalDate mappingEffectiveOn) {

    /** Validates the versioned mapping and locale coordinates. */
    public NormalizationRequest {
        Objects.requireNonNull(sourceId, "sourceId must not be null");
        Objects.requireNonNull(assertion, "assertion must not be null");
        Objects.requireNonNull(sourceLocale, "sourceLocale must not be null");
        Objects.requireNonNull(mappingEffectiveOn, "mappingEffectiveOn must not be null");
    }
}
