package org.open4goods.eprelservice.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.open4goods.datareference.model.ProjectionSurface;
import org.open4goods.datareference.model.GtinMatchConfidence;
import org.open4goods.datareference.model.SourceContentType;
import org.open4goods.datareference.model.SourceRecordState;
import org.open4goods.datareference.model.SourceUsagePolicyRegistry;
import org.open4goods.datareference.model.evidence.LocalizedTextEvidence;
import org.open4goods.datareference.model.evidence.ScalarEvidence;
import org.open4goods.datareference.serialization.DataReferenceJson;
import org.open4goods.model.eprel.EprelProduct;
import org.open4goods.services.eprelservice.service.EprelSourceRecordAdapter;

/** Tests for {@link EprelSourceRecordAdapter}. */
class EprelSourceRecordAdapterTest {

    private static final Instant RETRIEVED = Instant.parse("2026-09-16T12:00:00Z");
    private final EprelSourceRecordAdapter adapter = new EprelSourceRecordAdapter();

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
                .containsExactly(new LocalizedTextEvidence("Class A", new org.open4goods.datareference.model.LanguageTag("en")),
                        new LocalizedTextEvidence("Classe A", new org.open4goods.datareference.model.LanguageTag("fr-CA")));
        assertThat(mutation.candidate().assertions())
                .anySatisfy(assertion -> {
                    assertThat(assertion.field().key()).isEqualTo("productGroup");
                    assertThat(assertion.contentType()).isEqualTo(SourceContentType.CLASSIFICATION);
                });
    }

    @Test
    void givesIdenticalPayloadHashesToRepeatedCatalogueRows() {
        EprelProduct product = product("12345");

        var first = adapter.adapt(product, "catalogue-2026-09", RETRIEVED).orElseThrow();
        var repeated = adapter.adapt(product, "catalogue-2026-09", RETRIEVED.plusSeconds(30)).orElseThrow();
        var byteEquivalent = adapter.adapt(product, "catalogue-2026-09", RETRIEVED).orElseThrow();

        assertThat(repeated.candidate().payloadHash()).isEqualTo(first.candidate().payloadHash());
        assertThat(DataReferenceJson.mapper().writeValueAsBytes(byteEquivalent.candidate()))
                .isEqualTo(DataReferenceJson.mapper().writeValueAsBytes(first.candidate()));
    }

    @Test
    void preservesAnUnknownUnitAndUsesUndForAnUnrecognizedLanguageMap() {
        EprelProduct product = product("12345");
        product.setCategorySpecificAttributes(Map.of(
                "coolingPower", Map.of("value", "12.5", "unit", "furlong-per-fortnight"),
                "unrecognizedLocale", Map.of("not a language tag", "raw text")));

        var head = adapter.adapt(product, "catalogue-2026-09", RETRIEVED).orElseThrow().candidate();

        assertThat(head.assertions()).anySatisfy(assertion -> {
            assertThat(assertion.field().key()).isEqualTo("coolingPower");
            assertThat(assertion.evidence()).isEqualTo(new ScalarEvidence("12.5", "furlong-per-fortnight",
                    org.open4goods.datareference.model.LanguageTag.UND));
        });
        assertThat(head.assertions()).anySatisfy(assertion -> {
            assertThat(assertion.field().key()).isEqualTo("unrecognizedLocale");
            assertThat(assertion.evidence().language()).isEqualTo(org.open4goods.datareference.model.LanguageTag.UND);
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
    void defaultEprelPolicyDeniesAllPublicationSurfaces() throws Exception {
        var head = adapter.adapt(product("12345"), "catalogue-2026-09", RETRIEVED).orElseThrow().candidate();
        SourceUsagePolicyRegistry policies = SourceUsagePolicyRegistry.loadDefault();

        assertThat(policies.allows(head.key().sourceId(), head.usagePolicyRef(), SourceContentType.ATTRIBUTE,
                ProjectionSurface.B2B_API, RETRIEVED)).isFalse();
        assertThat(policies.allows(head.key().sourceId(), head.usagePolicyRef(), SourceContentType.ATTRIBUTE,
                ProjectionSurface.ODBL_EXPORT, RETRIEVED)).isFalse();
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

    private EprelProduct product(String registration) {
        EprelProduct product = new EprelProduct();
        product.setEprelRegistrationNumber(registration);
        product.setProductGroup("televisions");
        product.setModelIdentifier("MODEL-123");
        product.setPublishedOnDateTs(RETRIEVED.getEpochSecond());
        return product;
    }
}
