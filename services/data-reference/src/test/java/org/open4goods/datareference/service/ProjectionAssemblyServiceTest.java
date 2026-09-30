package org.open4goods.datareference.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.open4goods.datareference.model.CanonicalAttributeId;
import org.open4goods.datareference.model.Gtin;
import org.open4goods.datareference.model.ProjectionSurface;
import org.open4goods.datareference.model.RuleVersion;
import org.open4goods.datareference.model.SourceRecordHead;
import org.open4goods.datareference.model.SourceRecordKey;
import org.open4goods.datareference.model.SourceUsagePolicyRef;
import org.open4goods.datareference.model.projection.EvaluationRefreshTrigger;
import org.open4goods.datareference.model.projection.EvaluationSummary;
import org.open4goods.datareference.model.projection.OfferSummary;
import org.open4goods.datareference.model.projection.ProductReferenceProjectionEnvelope;
import org.open4goods.datareference.model.projection.ProjectionReplayInputs;
import org.open4goods.datareference.model.projection.SearchSummary;
import org.open4goods.datareference.model.registry.RegistryVersion;
import org.open4goods.datareference.model.resolution.ResolutionReason;
import org.open4goods.datareference.model.resolution.ResolvedValue;
import org.open4goods.datareference.model.value.CodeValue;
import org.open4goods.datareference.port.ProjectionWritePort;
import org.open4goods.datareference.port.ScanFailure;
import org.open4goods.datareference.port.SourceRecordHeadStore;

/** Unit coverage for the only typed writer of a projection envelope. */
class ProjectionAssemblyServiceTest {

    private static final Instant AT = Instant.parse("2026-09-16T12:00:00Z");
    private static final Gtin GTIN = new Gtin("4006381333931");
    private static final ResolvedValue ALLOWED_VALUE = new ResolvedValue(new CanonicalAttributeId("brand"),
            new CodeValue("brand", "Allowed"), new org.open4goods.datareference.model.AssertionId("allowed-assertion"),
            List.of(new org.open4goods.datareference.model.AssertionId("allowed-assertion")),
            new RuleVersion("resolution", 1), ResolutionReason.CONFIGURED_SOURCE_RANK, false);

    @Test
    void writesOneCompleteEnvelopeAndPassesOnlyResolvedValuesToEvaluationAndSearch() {
        CapturingWriter writer = new CapturingWriter();
        ProjectionAssemblyService service = new ProjectionAssemblyService(new EmptyHeads(),
                (gtin, heads, surface, at) -> List.of(ALLOWED_VALUE),
                (gtin, surface, inputs) -> new OfferSummary(0, false, null, null, AT),
                input -> {
                    assertThat(input.resolvedValues()).containsExactly(ALLOWED_VALUE);
                    return new EvaluationSummary(new RuleVersion("evaluation", 1), AT, Map.of(), Map.of(), List.of());
                },
                (gtin, surface, inputs, values) -> {
                    assertThat(values).containsExactly(ALLOWED_VALUE);
                    return new SearchSummary(new RuleVersion("lexical-search", 1), List.of("allowed"));
                }, new DeterministicDomainSliceComposer(), writer, Clock.fixed(AT, ZoneOffset.UTC));

        ProductReferenceProjectionEnvelope result = service.rebuild(GTIN, replayInputs(), EvaluationRefreshTrigger.REFERENCE_CHANGED);

        assertThat(writer.written).isEqualTo(result);
        assertThat(result.components()).hasSize(ProjectionSurface.values().length);
        assertThat(result.components().values()).allSatisfy(component -> {
            assertThat(component.resolvedValues()).containsExactly(ALLOWED_VALUE);
            assertThat(component.search().lexicalTerms()).containsExactly("allowed");
        });
    }

    private static ProjectionReplayInputs replayInputs() {
        return new ProjectionReplayInputs(new RegistryVersion(1), new RuleVersion("normalization", 1),
                new RuleVersion("resolution", 1), List.of(new SourceUsagePolicyRef("policy", "1")), AT);
    }

    private static final class CapturingWriter implements ProjectionWritePort {
        private ProductReferenceProjectionEnvelope written;

        @Override
        public void write(ProductReferenceProjectionEnvelope projection) {
            written = projection;
        }

        @Override
        public List<ScanFailure> writeAll(List<ProductReferenceProjectionEnvelope> projections) {
            projections.forEach(this::write);
            return List.of();
        }
    }

    private static final class EmptyHeads implements SourceRecordHeadStore {
        @Override
        public org.open4goods.datareference.model.SourceRecordTransition apply(
                org.open4goods.datareference.model.SourceRecordMutation mutation) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<SourceRecordHead> find(SourceRecordKey key) {
            return Optional.empty();
        }

        @Override
        public List<SourceRecordHead> findByGtin(Gtin gtin) {
            return List.of();
        }

        @Override
        public boolean storeIfNewer(SourceRecordHead head) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean delete(SourceRecordKey key) {
            throw new UnsupportedOperationException();
        }
    }
}
