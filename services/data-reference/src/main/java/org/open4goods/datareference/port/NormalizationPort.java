package org.open4goods.datareference.port;

import org.open4goods.datareference.model.normalization.NormalizationRequest;
import org.open4goods.datareference.model.normalization.NormalizationResult;

/**
 * Turns what a source said into typed canonical values.
 *
 * <p>Adapters pass locale, language and mapping-time inputs explicitly. A
 * failure is a result, never an empty collection that might be mistaken for an
 * intentional omission by a projection writer.
 */
public interface NormalizationPort {

    /**
     * Normalizes one assertion.
     *
     * @param request immutable source and locale inputs
     * @return typed value or an auditable failure
     */
    NormalizationResult normalize(NormalizationRequest request);
}
