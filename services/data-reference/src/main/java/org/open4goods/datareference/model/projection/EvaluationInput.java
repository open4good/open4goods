package org.open4goods.datareference.model.projection;

import java.util.List;
import java.util.Objects;

import org.open4goods.datareference.model.Gtin;
import org.open4goods.datareference.model.ProjectionSurface;
import org.open4goods.datareference.model.resolution.ResolvedValue;

/**
 * Eligible data passed to the evaluation bridge.
 *
 * <p>The bridge receives resolved values and an offer summary only. It has no
 * legacy-product parameter, so an implementation cannot silently fill a
 * missing reference value from the legacy read model.
 *
 * @param gtin product to evaluate
 * @param surface policy-filtered publication surface
 * @param replayInputs fixed versions and evaluation instant
 * @param resolvedValues eligible normalized reference values
 * @param offers current offer summary
 * @param trigger event requiring a refresh
 */
public record EvaluationInput(Gtin gtin, ProjectionSurface surface, ProjectionReplayInputs replayInputs,
        List<ResolvedValue> resolvedValues, OfferSummary offers, EvaluationRefreshTrigger trigger) {

    /** Copies the eligible values and rejects missing replay coordinates. */
    public EvaluationInput {
        Objects.requireNonNull(gtin, "gtin must not be null");
        Objects.requireNonNull(surface, "surface must not be null");
        Objects.requireNonNull(replayInputs, "replayInputs must not be null");
        resolvedValues = List.copyOf(Objects.requireNonNull(resolvedValues, "resolvedValues must not be null"));
        if (resolvedValues.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("resolvedValues must not contain null");
        }
        Objects.requireNonNull(offers, "offers must not be null");
        Objects.requireNonNull(trigger, "trigger must not be null");
    }
}
