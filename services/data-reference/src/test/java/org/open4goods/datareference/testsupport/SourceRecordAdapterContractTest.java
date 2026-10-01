package org.open4goods.datareference.testsupport;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.time.Instant;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.open4goods.datareference.model.GtinMatchConfidence;
import org.open4goods.datareference.model.LanguageTag;
import org.open4goods.datareference.model.ProjectionSurface;
import org.open4goods.datareference.model.SourceContentType;
import org.open4goods.datareference.model.SourceRecordHead;
import org.open4goods.datareference.model.SourceRecordState;
import org.open4goods.datareference.model.SourceUsagePolicyRegistry;
import org.open4goods.datareference.serialization.DataReferenceJson;

/**
 * Contract every source-record adapter must satisfy (GOU-38 AC8).
 *
 * <p>A source adapter converts one provider row into a {@code SourceRecordMutation}. Whatever the
 * provider, the adapter must behave identically on these axes. A source module inherits this
 * class and supplies only its own row fixtures through {@link #fixture()}; it adds provider-
 * specific tests of its own alongside the inherited ones.
 *
 * <p>Deletion driven by catalogue reconciliation (GOU-38 AC3) is deliberately not one of these
 * scenarios: no adapter can build a reconciliation-driven tombstone until AC3 ships the shared
 * reconciliation port. Add that scenario here, once, when that port exists — not per adapter.
 *
 * @param <T> provider row type consumed by the adapter under test
 */
public abstract class SourceRecordAdapterContractTest<T> {

    /** Retrieval instant shared by every scenario that does not itself vary time. */
    protected static final Instant RETRIEVED = Instant.parse("2026-09-16T12:00:00Z");
    /** Schema version shared by every scenario; the contract does not depend on its value. */
    protected static final String SCHEMA_VERSION = "contract-schema-1";

    /**
     * Supplies the adapter under test and its row fixtures.
     *
     * @return fixture for this adapter
     */
    protected abstract SourceRecordAdapterContractFixture<T> fixture();

    @Test
    void repeatedRowsProduceAStablePayloadHash() {
        SourceRecordAdapterContractFixture<T> fx = fixture();
        T row = fx.repeatableRow();

        SourceRecordHead first = fx.adapt(row, SCHEMA_VERSION, RETRIEVED).candidate();
        SourceRecordHead repeated = fx.adapt(row, SCHEMA_VERSION, RETRIEVED.plusSeconds(30)).candidate();

        assertThat(repeated.payloadHash()).isEqualTo(first.payloadHash());
    }

    @Test
    void identicalInputsSerializeToByteEquivalentOutput() {
        SourceRecordAdapterContractFixture<T> fx = fixture();
        T row = fx.repeatableRow();

        SourceRecordHead first = fx.adapt(row, SCHEMA_VERSION, RETRIEVED).candidate();
        SourceRecordHead byteEquivalent = fx.adapt(row, SCHEMA_VERSION, RETRIEVED).candidate();

        assertThat(DataReferenceJson.mapper().writeValueAsBytes(byteEquivalent))
                .isEqualTo(DataReferenceJson.mapper().writeValueAsBytes(first));
    }

    @Test
    void unrecognizedUnitsOrLanguagesFallBackToUnd() {
        SourceRecordAdapterContractFixture<T> fx = fixture();
        SourceRecordHead head = fx.adapt(fx.rowWithUnknownUnitOrLanguage(), SCHEMA_VERSION, RETRIEVED).candidate();

        assertThat(head.assertions())
                .extracting(assertion -> assertion.evidence().language())
                .contains(LanguageTag.UND);
    }

    @Test
    void aProviderVersionChangePreservesTheRecordKeyAndChangesThePayload() {
        SourceRecordAdapterContractFixture<T> fx = fixture();
        SourceRecordHead before = fx.adapt(fx.rowBeforeProviderVersionChange(), SCHEMA_VERSION, RETRIEVED).candidate();
        SourceRecordHead after = fx.adapt(fx.rowAfterProviderVersionChange(), SCHEMA_VERSION, RETRIEVED.plusSeconds(1))
                .candidate();

        assertThat(after.key()).isEqualTo(before.key());
        assertThat(after.providerVersion()).isNotEqualTo(before.providerVersion());
        assertThat(after.payloadHash()).isNotEqualTo(before.payloadHash());
    }

    @Test
    void aGtinCorrectionReplacesTheAttachedLinkWithoutChangingTheRecordKey() {
        SourceRecordAdapterContractFixture<T> fx = fixture();
        SourceRecordHead before = fx.adapt(fx.rowBeforeGtinCorrection(), SCHEMA_VERSION, RETRIEVED).candidate();
        SourceRecordHead after = fx.adapt(fx.rowAfterGtinCorrection(), SCHEMA_VERSION, RETRIEVED.plusSeconds(1))
                .candidate();

        assertThat(after.key()).isEqualTo(before.key());
        assertThat(after.gtinLinks()).isNotEqualTo(before.gtinLinks());
        assertThat(after.gtinLinks())
                .extracting(link -> link.confidence())
                .contains(GtinMatchConfidence.EXACT);
    }

    @Test
    void aWithdrawnRowProducesAnEmptyDeletedHead() {
        SourceRecordAdapterContractFixture<T> fx = fixture();
        SourceRecordHead head = fx.adapt(fx.withdrawnRow(), SCHEMA_VERSION, RETRIEVED).candidate();

        assertThat(head.state()).isEqualTo(SourceRecordState.DELETED);
        assertThat(head.assertions()).isEmpty();
        assertThat(head.gtinLinks()).isEmpty();
    }

    /**
     * Publication surfaces a GOU-95 review has opened for {@link SourceContentType#ATTRIBUTE} on
     * this adapter's source. Empty by default: an unreviewed policy denies every surface. A source
     * whose policy row has since been reviewed and partially opened overrides this to match.
     */
    protected Set<ProjectionSurface> reviewedOpenAttributeSurfaces() {
        return Set.of();
    }

    @Test
    void theUsagePolicyOpensOnlyItsReviewedAttributeSurfaces() throws IOException {
        SourceRecordAdapterContractFixture<T> fx = fixture();
        SourceRecordHead head = fx.adapt(fx.repeatableRow(), SCHEMA_VERSION, RETRIEVED).candidate();
        SourceUsagePolicyRegistry policies = SourceUsagePolicyRegistry.loadDefault();
        Set<ProjectionSurface> openSurfaces = reviewedOpenAttributeSurfaces();

        for (ProjectionSurface surface : ProjectionSurface.values()) {
            boolean allowed = policies.allows(head.key().sourceId(), head.usagePolicyRef(),
                    SourceContentType.ATTRIBUTE, surface, RETRIEVED);
            assertThat(allowed).isEqualTo(openSurfaces.contains(surface));
        }
    }

    @Test
    void rowsWithoutAStableIdentifierAreRejected() {
        SourceRecordAdapterContractFixture<T> fx = fixture();
        assertThat(fx.attempt(fx.rowWithoutIdentifier(), SCHEMA_VERSION, RETRIEVED)).isEmpty();
    }
}
