package org.open4goods.datareference.serialization;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.open4goods.datareference.model.Gtin;
import org.open4goods.datareference.model.ProjectionSurface;
import org.open4goods.datareference.model.projection.ProductReferenceProjection;
import org.open4goods.datareference.model.projection.ProductReferenceProjectionEnvelope;

/** Tests the representation used to compare deterministic replay results. */
class ProjectionCanonicalizerTest {

    @Test
    void ignoresWorkerDeliveryTimeButRetainsPublishedValuesAndProvenance() {
        ProductReferenceProjection fixture = ContractFixtures.projection();
        ProductReferenceProjectionEnvelope first = envelope(fixture, Instant.parse("2026-09-16T10:00:00Z"));
        ProductReferenceProjectionEnvelope replayed = envelope(fixture, Instant.parse("2026-09-16T11:00:00Z"));

        assertThat(ProjectionCanonicalizer.canonicalBytes(replayed))
                .isEqualTo(ProjectionCanonicalizer.canonicalBytes(first));
    }

    private static ProductReferenceProjectionEnvelope envelope(ProductReferenceProjection fixture, Instant builtAt) {
        Map<ProjectionSurface, ProductReferenceProjection> components = new EnumMap<>(ProjectionSurface.class);
        for (ProjectionSurface surface : ProjectionSurface.values()) {
            components.put(surface, new ProductReferenceProjection(new Gtin("4006381333931"), surface,
                    fixture.replayInputs(), builtAt, fixture.resolvedValues(), fixture.offers(), fixture.evaluation(), fixture.search()));
        }
        return new ProductReferenceProjectionEnvelope(new Gtin("4006381333931"), components);
    }
}
