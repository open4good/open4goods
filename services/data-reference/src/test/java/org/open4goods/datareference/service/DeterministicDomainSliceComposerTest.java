package org.open4goods.datareference.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.open4goods.datareference.model.AssertionId;
import org.open4goods.datareference.model.CanonicalAttributeId;
import org.open4goods.datareference.model.Gtin;
import org.open4goods.datareference.model.ProjectionSurface;
import org.open4goods.datareference.model.RuleVersion;
import org.open4goods.datareference.model.SourceUsagePolicyRef;
import org.open4goods.datareference.model.projection.EvaluationSummary;
import org.open4goods.datareference.model.projection.OfferSummary;
import org.open4goods.datareference.model.projection.ProjectionReplayInputs;
import org.open4goods.datareference.model.projection.SearchSummary;
import org.open4goods.datareference.model.registry.RegistryVersion;
import org.open4goods.datareference.model.resolution.ResolutionReason;
import org.open4goods.datareference.model.resolution.ResolvedValue;
import org.open4goods.datareference.model.value.DecimalValue;

/** Tests stable assembly at the only typed domain-slice composition boundary. */
class DeterministicDomainSliceComposerTest {

    @Test
    void ordersResolvedValuesByCanonicalAttributeRatherThanInputOrder() {
        var result = new DeterministicDomainSliceComposer().compose(
                new Gtin("4006381333931"), ProjectionSurface.NUDGER_WEB, replayInputs(),
                List.of(value("zeta"), value("alpha")), offerSummary(), evaluationSummary(), searchSummary(), Instant.EPOCH);

        assertThat(result.resolvedValues()).extracting(value -> value.attribute().externalForm())
                .containsExactly("o4g:attribute:alpha", "o4g:attribute:zeta");
    }

    private static ProjectionReplayInputs replayInputs() {
        return new ProjectionReplayInputs(new RegistryVersion(1), new RuleVersion("normalization", 1),
                new RuleVersion("resolution", 1), List.of(new SourceUsagePolicyRef("policy", "1")), Instant.EPOCH);
    }

    private static ResolvedValue value(String attribute) {
        CanonicalAttributeId id = new CanonicalAttributeId(attribute);
        AssertionId assertion = new AssertionId("urn:o4g:assertion:" + attribute);
        return new ResolvedValue(id, new DecimalValue(BigDecimal.ONE), assertion, List.of(assertion),
                new RuleVersion("resolution", 1), ResolutionReason.CONFIGURED_SOURCE_RANK, false);
    }

    private static OfferSummary offerSummary() {
        return new OfferSummary(0, false, null, null, Instant.EPOCH);
    }

    private static EvaluationSummary evaluationSummary() {
        return new EvaluationSummary(new RuleVersion("evaluation", 1), Instant.EPOCH, Map.of(), Map.of(), List.of());
    }

    private static SearchSummary searchSummary() {
        return new SearchSummary(new RuleVersion("lexical-search", 1), List.of());
    }
}
