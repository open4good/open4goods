package org.open4goods.datareference.model.normalization;

import java.util.Objects;

import org.open4goods.datareference.model.AssertionId;
import org.open4goods.datareference.model.LanguageTag;
import org.open4goods.datareference.model.registry.RegistryVersion;
import org.open4goods.datareference.model.resolution.NormalizedValue;

/** Auditable result for one source assertion; failures carry no projected value. */
public record NormalizationResult(
        AssertionId sourceAssertionId,
        RegistryVersion registryVersion,
        LanguageTag language,
        NormalizationStatus status,
        String diagnostic,
        NormalizedValue value) {

    /** Validates result identity and keeps successful and failed shapes distinct. */
    public NormalizationResult {
        Objects.requireNonNull(sourceAssertionId, "sourceAssertionId must not be null");
        Objects.requireNonNull(registryVersion, "registryVersion must not be null");
        Objects.requireNonNull(language, "language must not be null");
        Objects.requireNonNull(status, "status must not be null");
        if (status == NormalizationStatus.SUCCESS && value == null) {
            throw new IllegalArgumentException("a successful normalization needs a value");
        }
        if (status != NormalizationStatus.SUCCESS && value != null) {
            throw new IllegalArgumentException("a failed normalization cannot carry a value");
        }
    }
}
