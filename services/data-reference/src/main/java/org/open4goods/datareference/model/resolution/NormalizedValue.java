package org.open4goods.datareference.model.resolution;

import java.util.Objects;

import org.open4goods.datareference.model.AssertionId;
import org.open4goods.datareference.model.CanonicalAttributeId;
import org.open4goods.datareference.model.RuleVersion;
import org.open4goods.datareference.model.value.CanonicalValue;

/**
 * One assertion after normalization, before anything has been chosen.
 *
 * <p>Keeps the assertion it came from so that resolution can name a winner by
 * id, and the rule version so that a rebuild can tell a value produced by an
 * old rule from one produced by the current rule.
 *
 * @param sourceAssertionId assertion the value was normalized from
 * @param attribute canonical attribute it was mapped onto
 * @param value normalized value
 * @param ruleVersion normalization rule that produced it
 */
public record NormalizedValue(
        AssertionId sourceAssertionId,
        CanonicalAttributeId attribute,
        CanonicalValue value,
        RuleVersion ruleVersion) {

    /**
     * Validates the normalized value.
     */
    public NormalizedValue {
        Objects.requireNonNull(sourceAssertionId, "sourceAssertionId must not be null");
        Objects.requireNonNull(attribute, "attribute must not be null");
        Objects.requireNonNull(value, "value must not be null");
        Objects.requireNonNull(ruleVersion, "ruleVersion must not be null");
    }
}
