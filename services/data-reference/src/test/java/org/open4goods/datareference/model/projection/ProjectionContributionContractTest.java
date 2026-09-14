package org.open4goods.datareference.model.projection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.open4goods.datareference.model.CanonicalAttributeId;
import org.open4goods.datareference.model.Gtin;
import org.open4goods.datareference.model.ProjectionSurface;
import org.open4goods.datareference.model.RuleVersion;
import org.open4goods.datareference.model.SourceUsagePolicyRef;
import org.open4goods.datareference.model.registry.RegistryVersion;
import org.open4goods.datareference.model.value.DecimalValue;

/** Tests the typed non-reference contributions to a reference projection. */
class ProjectionContributionContractTest {

    @Test
    void evaluationCarriesAFixedReplayInstantAndMissingInputs() {
        EvaluationSummary summary = new EvaluationSummary(
                new RuleVersion("impact-evaluation", 3),
                Instant.parse("2026-09-12T00:00:00Z"),
                Map.of(new CanonicalAttributeId("impact"), new DecimalValue(new BigDecimal("7.25"))),
                Map.of("class-median", new DecimalValue(new BigDecimal("5.50"))),
                List.of(new CanonicalAttributeId("repair-index")));

        assertThat(summary.evaluatedAt()).isEqualTo(Instant.parse("2026-09-12T00:00:00Z"));
        assertThat(summary.missingInputs()).containsExactly(new CanonicalAttributeId("repair-index"));
    }

    @Test
    void offerSummaryRejectsAnImpossibleAvailabilityState() {
        assertThatThrownBy(() -> new OfferSummary(0, true, null, null, Instant.EPOCH))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("availability");
    }

    @Test
    void replayInputsMakePolicyAndRuleVersionsExplicit() {
        ProjectionReplayInputs inputs = new ProjectionReplayInputs(
                new RegistryVersion(7),
                new RuleVersion("quantity-normalization", 2),
                new RuleVersion("reference-resolution", 4),
                List.of(new SourceUsagePolicyRef("icecat-standard", "3")),
                Instant.parse("2026-09-12T00:00:00Z"));

        assertThat(inputs.policyRefs()).containsExactly(new SourceUsagePolicyRef("icecat-standard", "3"));
    }

    @Test
    void searchSummaryCannotCarryBlankOrSemanticTerms() {
        assertThatThrownBy(() -> new SearchSummary(new RuleVersion("lexical-search", 1), List.of("television", " ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blanks");
    }

    @Test
    void envelopeRequiresAComponentForEveryPublicationSurface() {
        ProductReferenceProjection component = new ProductReferenceProjection(
                new Gtin("4006381333931"), ProjectionSurface.NUDGER_WEB,
                new ProjectionReplayInputs(new RegistryVersion(7), new RuleVersion("normalization", 1),
                        new RuleVersion("resolution", 1), List.of(new SourceUsagePolicyRef("policy", "1")), Instant.EPOCH),
                Instant.EPOCH, List.of(), new OfferSummary(0, false, null, null, Instant.EPOCH),
                new EvaluationSummary(new RuleVersion("evaluation", 1), Instant.EPOCH, Map.of(), Map.of(), List.of()),
                new SearchSummary(new RuleVersion("lexical-search", 1), List.of()));

        assertThatThrownBy(() -> new ProductReferenceProjectionEnvelope(component.gtin(),
                Map.of(ProjectionSurface.NUDGER_WEB, component)))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("missing projection component");
    }

    @Test
    void everyCurrentConsumerHasTypedInputsAndAnOwningWorkOrder() {
        assertThat(ProjectionConsumer.values()).extracting(ProjectionConsumer::ownerWorkOrder)
                .doesNotContainNull()
                .allMatch(owner -> !owner.isBlank());
        assertThat(ProjectionConsumer.values()).allSatisfy(consumer ->
                assertThat(consumer.inputs()).isNotEmpty());
    }
}
