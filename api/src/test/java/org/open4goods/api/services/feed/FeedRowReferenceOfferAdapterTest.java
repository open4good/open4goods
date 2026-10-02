package org.open4goods.api.services.feed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.open4goods.api.services.feed.FeedRowReferenceOfferAdapter.FeedRowTranslation;
import org.open4goods.datareference.model.Gtin;
import org.open4goods.datareference.model.SourceContentType;
import org.open4goods.datareference.model.SourceId;
import org.open4goods.datareference.model.SourceRecordMutation;
import org.open4goods.datareference.model.SourceUsagePolicyRef;
import org.open4goods.datareference.model.evidence.LocalizedTextEvidence;
import org.open4goods.datareference.model.evidence.ScalarEvidence;
import org.open4goods.pricehistory.model.OfferAvailability;
import org.open4goods.pricehistory.model.OfferCondition;
import org.open4goods.pricehistory.model.OfferObservation;
import org.open4goods.services.feedservice.definition.ColumnResolution;
import org.open4goods.services.feedservice.definition.ColumnTarget;
import org.open4goods.services.feedservice.definition.FeedColumnResolver;
import org.open4goods.services.feedservice.definition.FeedDefinition;
import org.open4goods.services.feedservice.definition.FeedSemantics;
import org.open4goods.services.feedservice.definition.UnknownColumnPolicy;

/**
 * Verifies the AC3 reference/offer split and the AC6 payload-hash based content identity.
 */
class FeedRowReferenceOfferAdapterTest {

    private static final SourceUsagePolicyRef POLICY = new SourceUsagePolicyRef("merchant-feed", "1");
    private static final Instant RETRIEVED_AT = Instant.parse("2026-10-01T10:00:00Z");
    private static final Currency EUR = Currency.getInstance("EUR");
    private static final Gtin GTIN = new Gtin("3401590439888");

    private static final FeedDefinition DEFINITION = new FeedDefinition(
            new SourceId("merchant.acme"),
            "2026-09-01",
            List.of("sku"),
            Locale.FRENCH,
            Map.of("weight", "weight_unit"),
            FeedSemantics.FULL,
            Map.of(
                    "title", new ColumnTarget.ReferenceField(SourceContentType.TEXT, "name"),
                    "brand", new ColumnTarget.ReferenceField(SourceContentType.IDENTITY, "brand"),
                    "weight", new ColumnTarget.ReferenceField(SourceContentType.ATTRIBUTE, "weight"),
                    "price", new ColumnTarget.OfferField(ColumnTarget.OfferFieldKind.PRICE),
                    "availability", new ColumnTarget.OfferField(ColumnTarget.OfferFieldKind.AVAILABILITY),
                    "condition", new ColumnTarget.OfferField(ColumnTarget.OfferFieldKind.OFFER_CONDITION)),
            UnknownColumnPolicy.REPORT);

    private final FeedRowReferenceOfferAdapter adapter = new FeedRowReferenceOfferAdapter();

    private Map<String, String> row() {
        return Map.of(
                "sku", "SKU-42",
                "title", "Acme Widget",
                "brand", "Acme",
                "weight", "1.5",
                "weight_unit", "kg",
                "price", "19.90",
                "availability", "available",
                "condition", "new");
    }

    private ColumnResolution resolution() {
        return FeedColumnResolver.resolve(DEFINITION, row().keySet());
    }

    @Test
    void referenceColumnsBecomeAssertionsAndOfferColumnsBecomeOneObservation() {
        FeedRowTranslation translation =
                adapter.translate(DEFINITION, resolution(), row(), GTIN, EUR, RETRIEVED_AT, POLICY);

        SourceRecordMutation mutation = translation.referenceMutation().orElseThrow();
        assertThat(mutation.candidate().assertions()).extracting(a -> a.field().key())
                .containsExactlyInAnyOrder("name", "brand", "weight");

        OfferObservation observation = translation.offerObservation().orElseThrow();
        assertThat(observation.amount()).isEqualTo(new BigDecimal("19.9"));
        assertThat(observation.availability()).isEqualTo(OfferAvailability.AVAILABLE);
        assertThat(observation.condition()).isEqualTo(OfferCondition.NEW);
        assertThat(observation.key().providerOfferId()).isEqualTo("SKU-42");
    }

    @Test
    void referenceFieldsAreNeverEmbeddedInTheOfferObservation() {
        // AC3: nothing in OfferObservation carries "Acme Widget", "Acme" or weight/unit values;
        // the record only has condition, currency, amount, availability and identity fields.
        FeedRowTranslation translation =
                adapter.translate(DEFINITION, resolution(), row(), GTIN, EUR, RETRIEVED_AT, POLICY);

        OfferObservation observation = translation.offerObservation().orElseThrow();
        assertThat(observation.toString()).doesNotContain("Acme Widget").doesNotContain("1.5");
    }

    @Test
    void priceAvailabilityAndConditionNeverProduceASourceAssertion() {
        // AC3: the three offer-only kinds never appear among the reference assertions' field keys.
        FeedRowTranslation translation =
                adapter.translate(DEFINITION, resolution(), row(), GTIN, EUR, RETRIEVED_AT, POLICY);

        SourceRecordMutation mutation = translation.referenceMutation().orElseThrow();
        assertThat(mutation.candidate().assertions()).extracting(a -> a.field().key())
                .doesNotContain("price", "availability", "condition");
    }

    @Test
    void offerObservationIsAbsentWhenNoPriceColumnIsMapped() {
        FeedDefinition definitionWithoutPrice = new FeedDefinition(
                DEFINITION.sourceId(), DEFINITION.providerSchemaVersion(), DEFINITION.keyColumns(),
                DEFINITION.language(), Map.of(), FeedSemantics.FULL,
                Map.of("title", new ColumnTarget.ReferenceField(SourceContentType.TEXT, "name")),
                UnknownColumnPolicy.REPORT);
        Map<String, String> onlyReferenceRow = Map.of("sku", "SKU-42", "title", "Acme Widget");
        ColumnResolution resolution = FeedColumnResolver.resolve(definitionWithoutPrice, onlyReferenceRow.keySet());

        FeedRowTranslation translation =
                adapter.translate(definitionWithoutPrice, resolution, onlyReferenceRow, GTIN, EUR, RETRIEVED_AT, POLICY);

        assertThat(translation.offerObservation()).isEmpty();
        assertThat(translation.referenceMutation()).isPresent();
    }

    @Test
    void referenceMutationIsAbsentWhenFeedHasNoReferenceColumns() {
        FeedDefinition offerOnlyDefinition = new FeedDefinition(
                DEFINITION.sourceId(), DEFINITION.providerSchemaVersion(), DEFINITION.keyColumns(),
                DEFINITION.language(), Map.of(), FeedSemantics.FULL,
                Map.of("price", new ColumnTarget.OfferField(ColumnTarget.OfferFieldKind.PRICE)),
                UnknownColumnPolicy.REPORT);
        Map<String, String> priceOnlyRow = Map.of("sku", "SKU-42", "price", "19.90");
        ColumnResolution resolution = FeedColumnResolver.resolve(offerOnlyDefinition, priceOnlyRow.keySet());

        FeedRowTranslation translation =
                adapter.translate(offerOnlyDefinition, resolution, priceOnlyRow, GTIN, EUR, RETRIEVED_AT, POLICY);

        assertThat(translation.referenceMutation()).isEmpty();
        assertThat(translation.offerObservation()).isPresent();
    }

    @Test
    void payloadHashIsTheContentIdentityAndTwoIdenticalRowsHashIdentically() {
        // AC6: identity comes from SourceRecordHead.payloadHash / OfferObservation.contentHash,
        // never from any Product-derived state - this adapter never reads or writes a Product.
        FeedRowTranslation first = adapter.translate(DEFINITION, resolution(), row(), GTIN, EUR, RETRIEVED_AT, POLICY);
        FeedRowTranslation second = adapter.translate(DEFINITION, resolution(), row(), GTIN, EUR, RETRIEVED_AT, POLICY);

        assertThat(first.referenceMutation().orElseThrow().candidate().payloadHash())
                .isEqualTo(second.referenceMutation().orElseThrow().candidate().payloadHash());
        assertThat(first.offerObservation().orElseThrow().contentHash())
                .isEqualTo(second.offerObservation().orElseThrow().contentHash());
    }

    @Test
    void aChangedReferenceValueChangesOnlyThePayloadHashNotTheOfferContentHash() {
        Map<String, String> changedTitle = new java.util.HashMap<>(row());
        changedTitle.put("title", "Acme Widget Pro");

        FeedRowTranslation original = adapter.translate(DEFINITION, resolution(), row(), GTIN, EUR, RETRIEVED_AT, POLICY);
        FeedRowTranslation changed = adapter.translate(DEFINITION, resolution(), changedTitle, GTIN, EUR, RETRIEVED_AT, POLICY);

        assertThat(changed.referenceMutation().orElseThrow().candidate().payloadHash())
                .isNotEqualTo(original.referenceMutation().orElseThrow().candidate().payloadHash());
        assertThat(changed.offerObservation().orElseThrow().contentHash())
                .isEqualTo(original.offerObservation().orElseThrow().contentHash());
    }

    @Test
    void missingKeyColumnValueFailsFastRatherThanGuessingAProviderOfferId() {
        Map<String, String> noSku = new java.util.HashMap<>(row());
        noSku.remove("sku");

        assertThatThrownBy(() -> adapter.translate(DEFINITION, resolution(), noSku, GTIN, EUR, RETRIEVED_AT, POLICY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void textContentTypeProducesLocalizedTextEvidenceWhileOthersProduceScalarEvidence() {
        FeedRowTranslation translation =
                adapter.translate(DEFINITION, resolution(), row(), GTIN, EUR, RETRIEVED_AT, POLICY);

        SourceRecordMutation mutation = translation.referenceMutation().orElseThrow();
        assertThat(mutation.candidate().assertions())
                .filteredOn(a -> a.field().key().equals("name"))
                .first().extracting("evidence").isInstanceOf(LocalizedTextEvidence.class);
        assertThat(mutation.candidate().assertions())
                .filteredOn(a -> a.field().key().equals("weight"))
                .first().extracting("evidence").isInstanceOf(ScalarEvidence.class);
    }
}
