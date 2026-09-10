package org.open4goods.datareference.port;

import java.util.List;

import org.open4goods.datareference.model.SourceAssertion;
import org.open4goods.datareference.model.SourceId;
import org.open4goods.datareference.model.resolution.NormalizedValue;

/**
 * Turns what a source said into typed canonical values.
 *
 * <p>Returns a list rather than one value because a single provider field can
 * legitimately produce several: a dimensions string carrying width, height and
 * depth is one assertion and three canonical values. It returns an empty list,
 * never null, when no rule maps the assertion.
 */
public interface NormalizationPort {

    /**
     * Normalizes one assertion.
     *
     * @param sourceId source that made the assertion, selecting its mapping rules
     * @param assertion assertion to normalize
     * @return canonical values, empty when no rule maps this assertion
     */
    List<NormalizedValue> normalize(SourceId sourceId, SourceAssertion assertion);
}
