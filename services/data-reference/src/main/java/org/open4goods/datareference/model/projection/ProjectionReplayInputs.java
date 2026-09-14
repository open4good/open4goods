package org.open4goods.datareference.model.projection;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import org.open4goods.datareference.model.RuleVersion;
import org.open4goods.datareference.model.SourceUsagePolicyRef;
import org.open4goods.datareference.model.registry.RegistryVersion;

/**
 * Versioned inputs that make a projection replay deterministic.
 *
 * @param registryVersion authored registry version
 * @param normalizationVersion normalization rule version
 * @param resolutionVersion reference-resolution rule version
 * @param policyRefs ordered source-policy references applied before resolution
 * @param evaluationInstant fixed instant used for policy and evaluation decisions
 */
public record ProjectionReplayInputs(RegistryVersion registryVersion, RuleVersion normalizationVersion,
        RuleVersion resolutionVersion, List<SourceUsagePolicyRef> policyRefs, Instant evaluationInstant) {

    /** Validates fixed, versioned replay inputs. */
    public ProjectionReplayInputs {
        Objects.requireNonNull(registryVersion, "registryVersion must not be null");
        Objects.requireNonNull(normalizationVersion, "normalizationVersion must not be null");
        Objects.requireNonNull(resolutionVersion, "resolutionVersion must not be null");
        policyRefs = List.copyOf(Objects.requireNonNull(policyRefs, "policyRefs must not be null"));
        if (policyRefs.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("policyRefs must not contain null");
        }
        Objects.requireNonNull(evaluationInstant, "evaluationInstant must not be null");
    }
}
