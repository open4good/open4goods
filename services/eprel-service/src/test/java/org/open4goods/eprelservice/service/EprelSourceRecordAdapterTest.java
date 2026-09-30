package org.open4goods.eprelservice.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.open4goods.datareference.model.GtinMatchConfidence;
import org.open4goods.datareference.model.LanguageTag;
import org.open4goods.datareference.model.SourceContentType;
import org.open4goods.datareference.model.SourceRecordMutation;
import org.open4goods.datareference.model.evidence.LocalizedTextEvidence;
import org.open4goods.datareference.model.evidence.ScalarEvidence;
import org.open4goods.datareference.testsupport.SourceRecordAdapterContractFixture;
import org.open4goods.datareference.testsupport.SourceRecordAdapterContractTest;
import org.open4goods.model.eprel.EprelProduct;
import org.open4goods.services.eprelservice.service.EprelSourceRecordAdapter;

/**
 * Tests for {@link EprelSourceRecordAdapter}: the shared AC8 contract, plus behaviour specific to
 * how EPREL parameters map to source assertions.
 */
class EprelSourceRecordAdapterTest extends SourceRecordAdapterContractTest<EprelProduct> {

    private final EprelSourceRecordAdapter adapter = new EprelSourceRecordAdapter();

    @Override
    protected SourceRecordAdapterContractFixture<EprelProduct> fixture() {
        return new SourceRecordAdapterContractFixture<>() {
            @Override
            public SourceRecordMutation adapt(EprelProduct row, String schemaVersion, Instant retrievedAt) {
                return adapter.adapt(row, schemaVersion, retrievedAt).orElseThrow();
            }

            @Override
            public Optional<SourceRecordMutation> attempt(EprelProduct row, String schemaVersion, Instant retrievedAt) {
                return adapter.adapt(row, schemaVersion, retrievedAt);
            }

            @Override
            public EprelProduct repeatableRow() {
                return product("12345");
            }

            @Override
            public EprelProduct rowWithUnknownUnitOrLanguage() {
                EprelProduct product = product("12345");
                product.setCategorySpecificAttributes(Map.of(
                        "coolingPower", Map.of("value", "12.5", "unit", "furlong-per-fortnight"),
                        "unrecognizedLocale", Map.of("not a language tag", "raw text")));
                return product;
            }

            @Override
            public EprelProduct rowBeforeProviderVersionChange() {
                EprelProduct product = product("12345");
                product.setVersionId(1L);
                return product;
            }

            @Override
            public EprelProduct rowAfterProviderVersionChange() {
                EprelProduct product = product("12345");
                product.setVersionId(2L);
                return product;
            }

            @Override
            public EprelProduct rowBeforeGtinCorrection() {
                EprelProduct product = product("12345");
                product.setGtinIdentifier("0123456789012");
                product.setVersionId(1L);
                return product;
            }

            @Override
            public EprelProduct rowAfterGtinCorrection() {
                EprelProduct product = product("12345");
                product.setGtinIdentifier("4006381333931");
                product.setVersionId(2L);
                return product;
            }

            @Override
            public EprelProduct withdrawnRow() {
                EprelProduct product = product("12345");
                product.setStatus("WITHDRAWN");
                return product;
            }

            @Override
            public EprelProduct rowWithoutIdentifier() {
                return new EprelProduct();
            }
        };
    }

    @Test
    void mapsTheRegistrationRecordAndPreservesProviderParameterAndLanguageCoordinates() {
        EprelProduct product = product("12345");
        product.setGtinIdentifier("0123456789012");
        product.setCategorySpecificAttributes(Map.of("energyClassLabel", Map.of("fr-CA", "Classe A", "en", "Class A")));

        var mutation = adapter.adapt(product, "catalogue-2026-09", RETRIEVED).orElseThrow();

        assertThat(mutation.candidate().key().externalForm()).isEqualTo("eprel/12345");
        assertThat(mutation.candidate().gtinLinks()).singleElement().satisfies(link -> {
            assertThat(link.gtin().value()).isEqualTo("0123456789012");
            assertThat(link.confidence()).isEqualTo(GtinMatchConfidence.EXACT);
        });
        assertThat(mutation.candidate().assertions())
                .filteredOn(assertion -> assertion.field().key().equals("energyClassLabel"))
                .extracting(assertion -> assertion.evidence())
                .containsExactly(new LocalizedTextEvidence("Class A", new LanguageTag("en")),
                        new LocalizedTextEvidence("Classe A", new LanguageTag("fr-CA")));
        assertThat(mutation.candidate().assertions())
                .anySatisfy(assertion -> {
                    assertThat(assertion.field().key()).isEqualTo("productGroup");
                    assertThat(assertion.contentType()).isEqualTo(SourceContentType.CLASSIFICATION);
                });
    }

    @Test
    void preservesAnUnknownUnitValueAndUnit() {
        EprelProduct product = product("12345");
        product.setCategorySpecificAttributes(Map.of(
                "coolingPower", Map.of("value", "12.5", "unit", "furlong-per-fortnight")));

        var head = adapter.adapt(product, "catalogue-2026-09", RETRIEVED).orElseThrow().candidate();

        assertThat(head.assertions()).anySatisfy(assertion -> {
            assertThat(assertion.field().key()).isEqualTo("coolingPower");
            assertThat(assertion.evidence()).isEqualTo(new ScalarEvidence("12.5", "furlong-per-fortnight", LanguageTag.UND));
        });
    }

    @Test
    void changesTheAttachmentAndProviderVersionWithoutChangingTheEprelRecordKey() {
        EprelProduct original = product("12345");
        original.setGtinIdentifier("0123456789012");
        original.setVersionId(1L);
        EprelProduct corrected = product("12345");
        corrected.setGtinIdentifier("4006381333931");
        corrected.setVersionId(2L);

        var first = adapter.adapt(original, "catalogue-2026-09", RETRIEVED).orElseThrow().candidate();
        var replacement = adapter.adapt(corrected, "catalogue-2026-09", RETRIEVED.plusSeconds(1)).orElseThrow().candidate();

        assertThat(replacement.key()).isEqualTo(first.key());
        assertThat(replacement.providerVersion()).isEqualTo("version-id:2");
        assertThat(replacement.gtinLinks()).extracting(link -> link.gtin().value()).containsExactly("4006381333931");
        assertThat(replacement.payloadHash()).isNotEqualTo(first.payloadHash());
    }

    @Test
    void treatsAProviderVersionOnlyChangeAsANewSourceObservation() {
        EprelProduct original = product("12345");
        original.setVersionId(1L);
        EprelProduct corrected = product("12345");
        corrected.setVersionId(2L);

        var first = adapter.adapt(original, "catalogue-2026-09", RETRIEVED).orElseThrow().candidate();
        var replacement = adapter.adapt(corrected, "catalogue-2026-09", RETRIEVED.plusSeconds(1)).orElseThrow()
                .candidate();

        assertThat(replacement.key()).isEqualTo(first.key());
        assertThat(replacement.providerVersion()).isEqualTo("version-id:2");
        assertThat(replacement.payloadHash()).isNotEqualTo(first.payloadHash());
    }

    @Test
    void reviewedEprelPolicyAllowsWebAndB2bAttributesButDeniesOdblAndUnreviewedContentTypes() throws Exception {
        var head = adapter.adapt(product("12345"), "catalogue-2026-09", RETRIEVED).orElseThrow().candidate();
        SourceUsagePolicyRegistry policies = SourceUsagePolicyRegistry.loadDefault();

        // GOU-95/GOU-105: reviewed value-added-only redistribution opens NUDGER_WEB and B2B_API.
        assertThat(policies.allows(head.key().sourceId(), head.usagePolicyRef(), SourceContentType.ATTRIBUTE,
                ProjectionSurface.NUDGER_WEB, RETRIEVED)).isTrue();
        assertThat(policies.allows(head.key().sourceId(), head.usagePolicyRef(), SourceContentType.ATTRIBUTE,
                ProjectionSurface.B2B_API, RETRIEVED)).isTrue();
        // ODbL export is not opened on any source at this stage.
        assertThat(policies.allows(head.key().sourceId(), head.usagePolicyRef(), SourceContentType.ATTRIBUTE,
                ProjectionSurface.ODBL_EXPORT, RETRIEVED)).isFalse();
        // TEXT is not a type EprelSourceRecordAdapter emits, so it was never reviewed.
        assertThat(policies.allows(head.key().sourceId(), head.usagePolicyRef(), SourceContentType.TEXT,
                ProjectionSurface.NUDGER_WEB, RETRIEVED)).isFalse();
    }

    @Test
    void producesADeletedFullHeadForWithdrawnRecords() {
        EprelProduct product = product("12345");
        product.setStatus("WITHDRAWN");

        var mutation = adapter.adapt(product, "catalogue-2026-09", RETRIEVED).orElseThrow();

        assertThat(mutation.candidate().state()).isEqualTo(SourceRecordState.DELETED);
        assertThat(mutation.candidate().assertions()).isEmpty();
        assertThat(mutation.candidate().gtinLinks()).isEmpty();
    }

    @Test
    void rejectsRowsWithoutAnEprelRegistrationOrModelIdentifier() {
        assertThat(adapter.adapt(new EprelProduct(), "catalogue-2026-09", RETRIEVED)).isEmpty();
    }

    private static EprelProduct product(String registration) {
        EprelProduct product = new EprelProduct();
        product.setEprelRegistrationNumber(registration);
        product.setProductGroup("televisions");
        product.setModelIdentifier("MODEL-123");
        product.setPublishedOnDateTs(RETRIEVED.getEpochSecond());
        return product;
    }
}
