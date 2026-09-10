package org.open4goods.datareference.model.resolution;

import java.util.List;
import java.util.Objects;

import org.open4goods.datareference.model.AssertionId;
import org.open4goods.datareference.model.CanonicalAttributeId;
import org.open4goods.datareference.model.RuleVersion;
import org.open4goods.datareference.model.value.CanonicalValue;

/**
 * One canonical field after resolution: what won, why, and what it beat.
 *
 * <p>{@code candidateAssertionIds} holds ids, never candidate values. Losing
 * values are provider content and would carry a provider's licence into a
 * document the policy may not allow it in; the ids stay usable for audit
 * because a head can always be re-read by an operator who is entitled to it.
 *
 * @param attribute canonical attribute this value belongs to
 * @param value normalized winning value
 * @param winningAssertionId assertion the winning value came from
 * @param candidateAssertionIds every assertion considered, winner included, in stable order
 * @param ruleVersion resolution rule that decided
 * @param reason why this candidate won
 * @param conflicting whether candidates disagreed after normalization
 */
public record ResolvedValue(
        CanonicalAttributeId attribute,
        CanonicalValue value,
        AssertionId winningAssertionId,
        List<AssertionId> candidateAssertionIds,
        RuleVersion ruleVersion,
        ResolutionReason reason,
        boolean conflicting) {

    /**
     * Validates the resolved value and its provenance.
     */
    public ResolvedValue {
        Objects.requireNonNull(attribute, "attribute must not be null");
        Objects.requireNonNull(value, "value must not be null");
        Objects.requireNonNull(winningAssertionId, "winningAssertionId must not be null");
        Objects.requireNonNull(ruleVersion, "ruleVersion must not be null");
        Objects.requireNonNull(reason, "reason must not be null");
        candidateAssertionIds = List.copyOf(
                Objects.requireNonNull(candidateAssertionIds, "candidateAssertionIds must not be null"));
        if (!candidateAssertionIds.contains(winningAssertionId)) {
            throw new IllegalArgumentException("the winning assertion must be among the candidates");
        }
    }
}
