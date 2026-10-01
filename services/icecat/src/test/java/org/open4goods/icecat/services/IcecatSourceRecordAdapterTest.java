package org.open4goods.icecat.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.open4goods.datareference.model.GtinMatchConfidence;
import org.open4goods.datareference.model.LanguageTag;
import org.open4goods.datareference.model.ProjectionSurface;
import org.open4goods.datareference.model.SourceContentType;
import org.open4goods.datareference.model.SourceRecordCompleteness;
import org.open4goods.datareference.model.SourceRecordKey;
import org.open4goods.datareference.model.SourceRecordMutation;
import org.open4goods.datareference.model.SourceRecordState;
import org.open4goods.datareference.model.SourceUsagePolicyRegistry;
import org.open4goods.datareference.model.evidence.LocalizedTextEvidence;
import org.open4goods.datareference.model.evidence.MediaEvidence;
import org.open4goods.datareference.model.evidence.ScalarEvidence;
import org.open4goods.datareference.serialization.DataReferenceJson;
import org.open4goods.icecat.model.IcecatLiveApiResponse.Category;
import org.open4goods.icecat.model.IcecatLiveApiResponse.Feature;
import org.open4goods.icecat.model.IcecatLiveApiResponse.FeatureDetail;
import org.open4goods.icecat.model.IcecatLiveApiResponse.FeaturesGroups;
import org.open4goods.icecat.model.IcecatLiveApiResponse.GTIN;
import org.open4goods.icecat.model.IcecatLiveApiResponse.Gallery;
import org.open4goods.icecat.model.IcecatLiveApiResponse.GeneralInfo;
import org.open4goods.icecat.model.IcecatLiveApiResponse.IceDataItem;
import org.open4goods.icecat.model.IcecatLiveApiResponse.Name;

/** Contract tests for {@link IcecatSourceRecordAdapter}. */
class IcecatSourceRecordAdapterTest {

    private static final Instant RETRIEVED = Instant.parse("2026-09-30T12:00:00Z");
    private static final LanguageTag EN = new LanguageTag("en");
    private static final LanguageTag FR = new LanguageTag("fr");
    private final IcecatSourceRecordAdapter adapter = new IcecatSourceRecordAdapter();

    @Test
    void aSingleConfiguredLocaleProducesAFullActiveHead() {
        IceDataItem item = itemWithId(123);
        item.generalInfo.title = "Washing machine";

        var mutation = adapter.adapt(item, EN, Set.of(EN), "v1", RETRIEVED).orElseThrow();

        assertThat(mutation.candidate().key().externalForm()).isEqualTo("icecat/123");
        assertThat(mutation.candidate().completeness()).isEqualTo(SourceRecordCompleteness.FULL);
        assertThat(mutation.candidate().state()).isEqualTo(SourceRecordState.ACTIVE);
    }

    @Test
    void oneOfSeveralConfiguredLocalesProducesAPartialUpdate() {
        IceDataItem item = itemWithId(123);
        item.generalInfo.title = "Washing machine";

        var mutation = adapter.adapt(item, EN, Set.of(EN, FR), "v1", RETRIEVED).orElseThrow();

        assertThat(mutation.candidate().completeness()).isEqualTo(SourceRecordCompleteness.PARTIAL);
    }

    @Test
    void anEnglishAndAFrenchRefreshOfTheSameTitleOccupyDistinctCoordinates() {
        IceDataItem enItem = itemWithId(123);
        enItem.generalInfo.title = "Washing machine";
        IceDataItem frItem = itemWithId(123);
        frItem.generalInfo.title = "Lave-linge";

        var enMutation = adapter.adapt(enItem, EN, Set.of(EN, FR), "v1", RETRIEVED).orElseThrow();
        var frMutation = adapter.adapt(frItem, FR, Set.of(EN, FR), "v1", RETRIEVED.plusSeconds(1)).orElseThrow();

        var enField = enMutation.candidate().assertions().stream()
                .filter(a -> a.field().key().startsWith("title#")).findFirst().orElseThrow().field();
        var frField = frMutation.candidate().assertions().stream()
                .filter(a -> a.field().key().startsWith("title#")).findFirst().orElseThrow().field();

        // Distinct coordinates: applying the French PARTIAL update after the English one
        // (per SourceRecordHead#supersedes/PARTIAL merge semantics used by the store) cannot
        // remove the English assertion, because the two languages never share a (field, ordinal).
        assertThat(enField).isNotEqualTo(frField);
        assertThat(enField.key()).isEqualTo("title#en");
        assertThat(frField.key()).isEqualTo("title#fr");
    }

    @Test
    void repeatedBulletPointsKeepProviderOrderAsDistinctOrdinals() {
        IceDataItem item = itemWithId(123);
        item.generalInfo.bulletPoints = new org.open4goods.icecat.model.IcecatLiveApiResponse.BulletPoints();
        item.generalInfo.bulletPoints.values = List.of("Fast", "Quiet", "Reliable");

        var head = adapter.adapt(item, EN, Set.of(EN), "v1", RETRIEVED).orElseThrow().candidate();

        assertThat(head.assertions())
                .filteredOn(a -> a.field().key().equals("bulletPoint#en"))
                .extracting(a -> ((LocalizedTextEvidence) a.evidence()).text())
                .containsExactly("Fast", "Quiet", "Reliable");
    }

    @Test
    void aNumericFeatureWithAMeasureSignKeepsItsUnitAndStaysStableAcrossLanguages() {
        IceDataItem enItem = itemWithId(123);
        enItem.featuresGroups = List.of(featureGroup(feature("77", "77", "15", "cm", "0")));
        IceDataItem frItem = itemWithId(123);
        frItem.featuresGroups = List.of(featureGroup(feature("77", "77", "15", "cm", "0")));

        var enHead = adapter.adapt(enItem, EN, Set.of(EN, FR), "v1", RETRIEVED).orElseThrow().candidate();
        var frHead = adapter.adapt(frItem, FR, Set.of(EN, FR), "v1", RETRIEVED).orElseThrow().candidate();

        var enAssertion = enHead.assertions().stream().filter(a -> a.field().key().equals("feature:77")).findFirst().orElseThrow();
        var frAssertion = frHead.assertions().stream().filter(a -> a.field().key().equals("feature:77")).findFirst().orElseThrow();
        assertThat(enAssertion.field()).isEqualTo(frAssertion.field());
        assertThat(enAssertion.evidence()).isEqualTo(new ScalarEvidence("15", "cm", LanguageTag.UND));
        assertThat(enAssertion.contentType()).isEqualTo(SourceContentType.ATTRIBUTE);
    }

    @Test
    void aFeatureWithoutAMeasureSignHasNoUnit() {
        IceDataItem item = itemWithId(123);
        item.featuresGroups = List.of(featureGroup(feature("88", "88", "Yes", null, "0")));

        var head = adapter.adapt(item, EN, Set.of(EN), "v1", RETRIEVED).orElseThrow().candidate();

        var assertion = head.assertions().stream().filter(a -> a.field().key().equals("feature:88")).findFirst().orElseThrow();
        assertThat(assertion.evidence()).isEqualTo(new ScalarEvidence("Yes", null, LanguageTag.UND));
    }

    @Test
    void aLocalizedFeatureIsKeyedByLanguageUsingTheStableCategoryFeatureId() {
        IceDataItem item = itemWithId(123);
        Feature feature = feature("99", "99", "Stainless steel finish", null, "1");
        item.featuresGroups = List.of(featureGroup(feature));

        var head = adapter.adapt(item, FR, Set.of(EN, FR), "v1", RETRIEVED).orElseThrow().candidate();

        assertThat(head.assertions()).anySatisfy(assertion -> {
            assertThat(assertion.field().key()).isEqualTo("feature:99#fr");
            assertThat(assertion.evidence()).isEqualTo(new LocalizedTextEvidence("Stainless steel finish", FR));
        });
    }

    @Test
    void galleryLogosAndDocumentsBecomeMediaAssertionsWithProviderUrls() {
        IceDataItem item = itemWithId(123);
        Gallery gallery = new Gallery();
        gallery.id = "g1";
        gallery.pic = "https://icecat.example/gallery/1.jpg";
        gallery.type = "photo";
        item.gallery = List.of(gallery);
        item.generalInfo.description = new org.open4goods.icecat.model.IcecatLiveApiResponse.Description();
        item.generalInfo.description.leafletPDFURL = "https://icecat.example/leaflet.pdf";

        var head = adapter.adapt(item, EN, Set.of(EN), "v1", RETRIEVED).orElseThrow().candidate();

        assertThat(head.assertions()).anySatisfy(assertion -> {
            assertThat(assertion.field().key()).isEqualTo("gallery:g1");
            assertThat(assertion.contentType()).isEqualTo(SourceContentType.MEDIA);
            assertThat(((MediaEvidence) assertion.evidence()).uri().toString())
                    .isEqualTo("https://icecat.example/gallery/1.jpg");
        });
        assertThat(head.assertions()).anySatisfy(assertion -> {
            assertThat(assertion.field().key()).isEqualTo("document:leaflet");
            assertThat(((MediaEvidence) assertion.evidence()).mediaType()).isEqualTo("application/pdf");
        });
    }

    @Test
    void approvedAndUnapprovedGtinsCarryDistinctConfidenceAndDeduplicateByValue() {
        IceDataItem item = itemWithId(123);
        GTIN approved = new GTIN();
        approved.gtin = "4006381333931";
        approved.isApproved = true;
        item.generalInfo.gtins = List.of(approved);
        item.generalInfo.gtin = List.of("4006381333931");

        var head = adapter.adapt(item, EN, Set.of(EN), "v1", RETRIEVED).orElseThrow().candidate();

        assertThat(head.gtinLinks()).singleElement().satisfies(link -> {
            assertThat(link.gtin().value()).isEqualTo("4006381333931");
            assertThat(link.confidence()).isEqualTo(GtinMatchConfidence.EXACT);
        });
    }

    @Test
    void givesIdenticalPayloadHashesToRepeatedResponses() {
        IceDataItem item = itemWithId(123);
        item.generalInfo.title = "Washing machine";

        var first = adapter.adapt(item, EN, Set.of(EN), "v1", RETRIEVED).orElseThrow();
        var repeated = adapter.adapt(item, EN, Set.of(EN), "v1", RETRIEVED.plusSeconds(30)).orElseThrow();
        var byteEquivalent = adapter.adapt(item, EN, Set.of(EN), "v1", RETRIEVED).orElseThrow();

        assertThat(repeated.candidate().payloadHash()).isEqualTo(first.candidate().payloadHash());
        assertThat(DataReferenceJson.mapper().writeValueAsBytes(byteEquivalent.candidate()))
                .isEqualTo(DataReferenceJson.mapper().writeValueAsBytes(first.candidate()));
    }

    @Test
    void producesADeletedFullHeadForATombstone() {
        SourceRecordKey key = IcecatSourceRecordAdapter.keyFor(123);

        SourceRecordMutation mutation = adapter.tombstone(key, "v1", RETRIEVED);

        assertThat(mutation.candidate().state()).isEqualTo(SourceRecordState.DELETED);
        assertThat(mutation.candidate().assertions()).isEmpty();
        assertThat(mutation.candidate().gtinLinks()).isEmpty();
    }

    @Test
    void producesARejectedTerminalAttemptForARestrictedResponseWithoutFabricatingContent() {
        SourceRecordKey key = IcecatSourceRecordAdapter.keyFor(123);

        SourceRecordMutation mutation = adapter.restricted(key, "v1", RETRIEVED, "RESTRICTED");

        assertThat(mutation.candidate().state()).isEqualTo(SourceRecordState.REJECTED);
        assertThat(mutation.sanitizedErrorCode()).isEqualTo("RESTRICTED");
        assertThat(mutation.candidate().assertions()).isEmpty();
    }

    @Test
    void producesAnUnavailableTerminalAttemptForATransientFailure() {
        SourceRecordKey key = IcecatSourceRecordAdapter.keyFor(123);

        SourceRecordMutation mutation = adapter.unavailable(key, "v1", RETRIEVED, "ERROR");

        assertThat(mutation.candidate().state()).isEqualTo(SourceRecordState.UNAVAILABLE);
        assertThat(mutation.sanitizedErrorCode()).isEqualTo("ERROR");
    }

    @Test
    void anUnmappedFeatureIsStillCarriedWithAStableIdRatherThanDropped() {
        IceDataItem item = itemWithId(123);
        item.featuresGroups = List.of(featureGroup(feature("555", "555", "Some unmapped value", null, "0")));

        var head = adapter.adapt(item, EN, Set.of(EN), "v1", RETRIEVED).orElseThrow().candidate();

        assertThat(head.assertions()).anySatisfy(assertion -> assertThat(assertion.field().key()).isEqualTo("feature:555"));
    }

    /**
     * The policy reference carried by every head must name a row that actually exists: resolution
     * is version-exact, so a reference to a superseded version resolves to nothing and silently
     * denies every surface. This pins the reviewed GOU-95 row (NUDGER_WEB only) instead of letting
     * a stale version read as a deliberate refusal.
     */
    @Test
    void icecatPolicyOpensOnlyTheReviewedNudgerWebSurface() throws Exception {
        var head = adapter.adapt(itemWithId(123), EN, Set.of(EN), "v1", RETRIEVED).orElseThrow().candidate();
        SourceUsagePolicyRegistry policies = SourceUsagePolicyRegistry.loadDefault();

        assertThat(policies.allows(head.key().sourceId(), head.usagePolicyRef(), SourceContentType.ATTRIBUTE,
                ProjectionSurface.NUDGER_WEB, RETRIEVED)).isTrue();
        assertThat(policies.allows(head.key().sourceId(), head.usagePolicyRef(), SourceContentType.ATTRIBUTE,
                ProjectionSurface.B2B_API, RETRIEVED)).isFalse();
        assertThat(policies.allows(head.key().sourceId(), head.usagePolicyRef(), SourceContentType.ATTRIBUTE,
                ProjectionSurface.ODBL_EXPORT, RETRIEVED)).isFalse();
    }

    @Test
    void rejectsAResponseWithoutAStableIcecatProductId() {
        IceDataItem item = new IceDataItem();

        assertThat(adapter.adapt(item, EN, Set.of(EN), "v1", RETRIEVED)).isEmpty();
    }

    private static IceDataItem itemWithId(int icecatId) {
        IceDataItem item = new IceDataItem();
        item.generalInfo = new GeneralInfo();
        item.generalInfo.icecatId = icecatId;
        item.generalInfo.category = new Category();
        return item;
    }

    private static FeaturesGroups featureGroup(Feature feature) {
        FeaturesGroups group = new FeaturesGroups();
        group.features = List.of(feature);
        return group;
    }

    private static Feature feature(String id, String categoryFeatureId, String value, String sign, String localized) {
        Feature feature = new Feature();
        feature.id = id;
        feature.categoryFeatureId = categoryFeatureId;
        feature.rawValue = value;
        feature.value = value;
        feature.localized = localized;
        feature.featureDetail = new FeatureDetail();
        feature.featureDetail.id = id;
        feature.featureDetail.sign = sign;
        feature.featureDetail.name = new Name();
        feature.featureDetail.name.value = "Ignored translated label";
        return feature;
    }
}
