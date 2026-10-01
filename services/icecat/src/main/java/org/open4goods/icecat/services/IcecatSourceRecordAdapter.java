package org.open4goods.icecat.services;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.datareference.model.Gtin;
import org.open4goods.datareference.model.GtinLink;
import org.open4goods.datareference.model.GtinMatchConfidence;
import org.open4goods.datareference.model.GtinMatchMethod;
import org.open4goods.datareference.model.LanguageTag;
import org.open4goods.datareference.model.PayloadHash;
import org.open4goods.datareference.model.SourceAssertion;
import org.open4goods.datareference.model.SourceContentType;
import org.open4goods.datareference.model.SourceFieldId;
import org.open4goods.datareference.model.SourceRecordCompleteness;
import org.open4goods.datareference.model.SourceRecordHead;
import org.open4goods.datareference.model.SourceRecordKey;
import org.open4goods.datareference.model.SourceRecordMutation;
import org.open4goods.datareference.model.SourceRecordState;
import org.open4goods.datareference.model.SourceUsagePolicyRef;
import org.open4goods.datareference.model.evidence.LocalizedTextEvidence;
import org.open4goods.datareference.model.evidence.MediaEvidence;
import org.open4goods.datareference.model.evidence.ScalarEvidence;
import org.open4goods.datareference.model.evidence.SourceEvidence;
import org.open4goods.datareference.serialization.DataReferenceJson;
import org.open4goods.icecat.model.IcecatLiveApiResponse.Description;
import org.open4goods.icecat.model.IcecatLiveApiResponse.Feature;
import org.open4goods.icecat.model.IcecatLiveApiResponse.FeatureLogos;
import org.open4goods.icecat.model.IcecatLiveApiResponse.FeaturesGroups;
import org.open4goods.icecat.model.IcecatLiveApiResponse.GTIN;
import org.open4goods.icecat.model.IcecatLiveApiResponse.Gallery;
import org.open4goods.icecat.model.IcecatLiveApiResponse.GeneralInfo;
import org.open4goods.icecat.model.IcecatLiveApiResponse.IceDataItem;
import org.open4goods.icecat.model.IcecatLiveApiResponse.Multimedia;
import org.springframework.stereotype.Component;

/**
 * Converts one Icecat live-API product response into a replaceable, source-neutral
 * record head.
 *
 * <p>This is the boundary between Icecat's own transport/reference concerns (bulk
 * import, live client, retries, localization, versioned reference indexes - all owned
 * by the rest of {@code services/icecat}) and the shared source-record contract. The
 * adapter never mutates a legacy {@code Product} or {@code DataFragment} and never
 * guesses an O4G attribute from a feature label: it only preserves Icecat's own stable
 * identifiers so that reviewed mapping (see {@code IcecatCategoryVerticalResolver} and
 * the shared normalization service) can resolve them later.
 *
 * <p>Bulk XML import shares the same {@code IceDataItem}-shaped neutral result today
 * only through the live client; wiring the JAXB bulk-export model into this same
 * {@code adapt} contract is tracked separately so both paths converge on one head per
 * Icecat product id, as required by the source-record adapter contract.
 *
 * <p>The live API returns one language per call, so a field whose value differs by
 * language is keyed with an explicit {@code #<language>} suffix; a single-language
 * refresh can then never silently delete another language's assertions, because the
 * two languages never share a coordinate. Fields whose value does not vary by
 * language (identifiers, numeric/measured features, media) keep a plain key so that
 * repeated refreshes in different languages agree on, rather than duplicate, them.
 */
@Component
public class IcecatSourceRecordAdapter {

    /** Icecat source identity used by the policy and source-record contracts. */
    public static final String SOURCE_ID = "icecat";
    /**
     * GOU-95/GOU-105: Open Icecat content reviewed for NUDGER_WEB only, share-alike, with
     * attribution. Policy resolution is version-exact, so this must name the reviewed row's
     * version: an unknown version resolves to nothing and denies every surface.
     */
    public static final SourceUsagePolicyRef USAGE_POLICY = new SourceUsagePolicyRef("icecat-open-content", "2");

    private static final String FIELD_NAMESPACE = "icecat";
    private static final URI EVIDENCE_BASE = URI.create("urn:o4g:icecat:record:");
    private static final String LOCALIZED_FLAG = "1";

    /**
     * Maps one language response for a product to a source-record mutation.
     *
     * <p>The mutation is {@code FULL} only when {@code configuredLocales} contains a single
     * locale equal to {@code language}: only then does this one response state everything the
     * source is configured to describe. Otherwise it is a {@code PARTIAL} update scoped to this
     * language's coordinates, leaving every other configured language's assertions untouched.
     *
     * @param item live or bulk-derived Icecat product item
     * @param language language this response was retrieved in
     * @param configuredLocales full set of languages this deployment is configured to track
     * @param schemaVersion Icecat reference/registry version selected by the importer
     * @param retrievedAt instant at which this response was retrieved
     * @return a mutation, or empty when Icecat supplied no stable product identifier
     */
    public Optional<SourceRecordMutation> adapt(
            IceDataItem item, LanguageTag language, Set<LanguageTag> configuredLocales, String schemaVersion, Instant retrievedAt) {
        Objects.requireNonNull(item, "item must not be null");
        Objects.requireNonNull(language, "language must not be null");
        Objects.requireNonNull(configuredLocales, "configuredLocales must not be null");
        Objects.requireNonNull(schemaVersion, "schemaVersion must not be null");
        Objects.requireNonNull(retrievedAt, "retrievedAt must not be null");

        Optional<SourceRecordKey> key = recordKey(item);
        if (key.isEmpty()) {
            return Optional.empty();
        }
        SourceRecordKey recordKey = key.orElseThrow();

        boolean full = configuredLocales.size() == 1 && configuredLocales.contains(language);
        SourceRecordCompleteness completeness = full ? SourceRecordCompleteness.FULL : SourceRecordCompleteness.PARTIAL;

        List<SourceAssertion> assertions = assertions(item, recordKey, language, schemaVersion);
        List<GtinLink> gtinLinks = gtinLinks(item.generalInfo);
        Instant observedAt = retrievedAt;

        SourceRecordHead head = new SourceRecordHead(
                recordKey,
                schemaVersion,
                null,
                observedAt,
                retrievedAt,
                null,
                completeness,
                SourceRecordState.ACTIVE,
                payloadHash(recordKey, schemaVersion, null, gtinLinks, assertions, SourceRecordState.ACTIVE),
                evidenceReference(recordKey),
                USAGE_POLICY,
                gtinLinks,
                assertions);
        return Optional.of(SourceRecordMutation.full(head));
    }

    /**
     * Builds an explicit withdrawal for a record Icecat no longer serves.
     *
     * @param key identity of the record to withdraw
     * @param schemaVersion O4G contract version this head is written against
     * @param retrievedAt instant this withdrawal was observed
     * @return a DELETED full replacement for the record
     */
    public SourceRecordMutation tombstone(SourceRecordKey key, String schemaVersion, Instant retrievedAt) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(schemaVersion, "schemaVersion must not be null");
        Objects.requireNonNull(retrievedAt, "retrievedAt must not be null");
        SourceRecordHead head = new SourceRecordHead(
                key, schemaVersion, null, retrievedAt, retrievedAt, null, SourceRecordCompleteness.FULL,
                SourceRecordState.DELETED,
                payloadHash(key, schemaVersion, null, List.of(), List.of(), SourceRecordState.DELETED),
                evidenceReference(key), USAGE_POLICY, List.of(), List.of());
        return SourceRecordMutation.full(head);
    }

    /**
     * Builds a transient-failure marker that never overwrites a still-usable head.
     *
     * @param key identity of the record Icecat could not currently serve
     * @param schemaVersion O4G contract version this head is written against
     * @param retrievedAt instant of the failed attempt
     * @param sanitizedErrorCode stable, non-sensitive reason code
     * @return an UNAVAILABLE terminal attempt
     */
    public SourceRecordMutation unavailable(SourceRecordKey key, String schemaVersion, Instant retrievedAt, String sanitizedErrorCode) {
        return terminal(key, schemaVersion, retrievedAt, SourceRecordState.UNAVAILABLE, sanitizedErrorCode);
    }

    /**
     * Builds a policy/plan-restriction marker that never overwrites a still-usable head.
     *
     * @param key identity of the record Icecat refused to serve
     * @param schemaVersion O4G contract version this head is written against
     * @param retrievedAt instant of the refused attempt
     * @param sanitizedErrorCode stable, non-sensitive reason code
     * @return a REJECTED terminal attempt
     */
    public SourceRecordMutation restricted(SourceRecordKey key, String schemaVersion, Instant retrievedAt, String sanitizedErrorCode) {
        return terminal(key, schemaVersion, retrievedAt, SourceRecordState.REJECTED, sanitizedErrorCode);
    }

    /**
     * Builds the stable record key for a given numeric Icecat product id.
     *
     * @param icecatId Icecat product identifier
     * @return the corresponding record key
     */
    public static SourceRecordKey keyFor(int icecatId) {
        return SourceRecordKey.of(SOURCE_ID, String.valueOf(icecatId));
    }

    private SourceRecordMutation terminal(
            SourceRecordKey key, String schemaVersion, Instant retrievedAt, SourceRecordState state, String sanitizedErrorCode) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(schemaVersion, "schemaVersion must not be null");
        Objects.requireNonNull(retrievedAt, "retrievedAt must not be null");
        Objects.requireNonNull(sanitizedErrorCode, "sanitizedErrorCode must not be null");
        SourceRecordHead head = new SourceRecordHead(
                key, schemaVersion, null, retrievedAt, retrievedAt, null, SourceRecordCompleteness.FULL, state,
                payloadHash(key, schemaVersion, null, List.of(), List.of(), state),
                evidenceReference(key), USAGE_POLICY, List.of(), List.of());
        return new SourceRecordMutation(head, List.of(), List.of(), sanitizedErrorCode);
    }

    private Optional<SourceRecordKey> recordKey(IceDataItem item) {
        if (item.generalInfo == null || item.generalInfo.icecatId <= 0) {
            return Optional.empty();
        }
        return Optional.of(keyFor(item.generalInfo.icecatId));
    }

    private List<SourceAssertion> assertions(IceDataItem item, SourceRecordKey key, LanguageTag language, String schemaVersion) {
        List<SourceAssertion> assertions = new ArrayList<>();
        Ordinals ordinals = new Ordinals();
        addGeneralInfo(item.generalInfo, language, schemaVersion, key, assertions, ordinals);
        addFeatures(nullToEmpty(item.featuresGroups), language, schemaVersion, key, assertions, ordinals);
        addMedia(item, language, schemaVersion, key, assertions, ordinals);
        return List.copyOf(assertions);
    }

    private void addGeneralInfo(GeneralInfo info, LanguageTag language, String schemaVersion, SourceRecordKey key,
            List<SourceAssertion> assertions, Ordinals ordinals) {
        if (info == null) {
            return;
        }
        addLocalizedText(assertions, ordinals, key, "title", info.title, language, schemaVersion, SourceContentType.TEXT);
        addLocalizedText(assertions, ordinals, key, "productName", info.productName, language, schemaVersion, SourceContentType.TEXT);
        addScalar(assertions, ordinals, key, "brand", firstNonBlank(
                info.brandInfo == null ? null : info.brandInfo.brandName, info.brand), null, schemaVersion, SourceContentType.IDENTITY);
        addScalar(assertions, ordinals, key, "model", info.brandPartCode, null, schemaVersion, SourceContentType.IDENTITY);

        if (info.productFamily != null) {
            LanguageTag familyLanguage = languageOrDefault(info.productFamily.language, language);
            addLocalizedText(assertions, ordinals, key, "productFamily", info.productFamily.value, familyLanguage, schemaVersion,
                    SourceContentType.IDENTITY);
        }
        if (info.productSeries != null) {
            LanguageTag seriesLanguage = languageOrDefault(info.productSeries.language, language);
            addLocalizedText(assertions, ordinals, key, "productSeries", info.productSeries.value, seriesLanguage, schemaVersion,
                    SourceContentType.IDENTITY);
        }
        if (info.category != null) {
            addScalar(assertions, ordinals, key, "category", info.category.categoryID, null, schemaVersion,
                    SourceContentType.CLASSIFICATION);
            if (info.category.name != null) {
                LanguageTag categoryLanguage = languageOrDefault(info.category.name.language, language);
                addLocalizedText(assertions, ordinals, key, "categoryName", info.category.name.value, categoryLanguage, schemaVersion,
                        SourceContentType.CLASSIFICATION);
            }
        }
        if (StringUtils.isNotBlank(info.releaseDate)) {
            addScalar(assertions, ordinals, key, "releaseDate", info.releaseDate, null, schemaVersion, SourceContentType.ATTRIBUTE);
        }
        if (StringUtils.isNotBlank(info.endOfLifeDate)) {
            addScalar(assertions, ordinals, key, "endOfLifeDate", info.endOfLifeDate, null, schemaVersion, SourceContentType.ATTRIBUTE);
        }

        String description = firstNonBlank(
                info.summaryDescription == null ? null : info.summaryDescription.longSummaryDescription,
                info.summaryDescription == null ? null : info.summaryDescription.shortSummaryDescription,
                info.description == null ? null : info.description.longDesc,
                info.description == null ? null : info.description.middleDesc);
        addLocalizedText(assertions, ordinals, key, "description", description, language, schemaVersion, SourceContentType.TEXT);

        List<String> bullets = info.bulletPoints != null && !nullToEmpty(info.bulletPoints.values).isEmpty()
                ? info.bulletPoints.values
                : info.generatedBulletPoints != null ? info.generatedBulletPoints.values : null;
        LanguageTag bulletLanguage = languageOrDefault(info.bulletPoints == null ? null : info.bulletPoints.language, language);
        for (String bullet : nullToEmpty(bullets)) {
            if (StringUtils.isNotBlank(bullet)) {
                addLocalizedText(assertions, ordinals, key, "bulletPoint", bullet, bulletLanguage, schemaVersion, SourceContentType.TEXT);
            }
        }
    }

    private void addFeatures(List<FeaturesGroups> groups, LanguageTag language, String schemaVersion, SourceRecordKey key,
            List<SourceAssertion> assertions, Ordinals ordinals) {
        for (FeaturesGroups group : groups) {
            if (group == null || group.features == null) {
                continue;
            }
            for (Feature feature : group.features) {
                if (feature == null) {
                    continue;
                }
                String featureId = firstNonBlank(feature.categoryFeatureId,
                        feature.featureDetail == null ? null : feature.featureDetail.id, feature.id);
                if (StringUtils.isBlank(featureId)) {
                    continue;
                }
                String rawValue = firstNonBlank(feature.rawValue, feature.value, feature.localValue);
                if (StringUtils.isBlank(rawValue)) {
                    continue;
                }
                boolean localized = LOCALIZED_FLAG.equals(feature.localized);
                String unit = feature.featureDetail == null ? null : StringUtils.trimToNull(feature.featureDetail.sign);
                if (localized) {
                    addLocalizedText(assertions, ordinals, key, "feature:" + featureId, rawValue, language, schemaVersion,
                            SourceContentType.ATTRIBUTE);
                } else {
                    addScalar(assertions, ordinals, key, "feature:" + featureId, rawValue, unit, schemaVersion,
                            SourceContentType.ATTRIBUTE);
                }
            }
        }
    }

    private void addMedia(IceDataItem item, LanguageTag language, String schemaVersion, SourceRecordKey key,
            List<SourceAssertion> assertions, Ordinals ordinals) {
        if (item.image != null && StringUtils.isNotBlank(item.image.highPic) && !item.image.highPic.contains("brand")) {
            addMediaAssertion(assertions, ordinals, key, "image:primary", item.image.highPic, null, "primary", LanguageTag.UND,
                    schemaVersion);
        }
        for (Gallery gallery : nullToEmpty(item.gallery)) {
            if (gallery == null || StringUtils.isBlank(gallery.pic)) {
                continue;
            }
            String fieldKey = "gallery:" + firstNonBlank(gallery.id, String.valueOf(ordinals.next("gallery")));
            addMediaAssertion(assertions, ordinals, key, fieldKey, gallery.pic, null, gallery.type, LanguageTag.UND, schemaVersion);
        }
        for (Multimedia media : nullToEmpty(item.multimedia)) {
            if (media == null || StringUtils.isBlank(media.url)) {
                continue;
            }
            LanguageTag mediaLanguage = languageOrDefault(media.language, LanguageTag.UND);
            String fieldKey = "multimedia:" + firstNonBlank(media.id, String.valueOf(ordinals.next("multimedia")));
            addMediaAssertion(assertions, ordinals, key, fieldKey, media.url, media.contentType, media.type, mediaLanguage,
                    schemaVersion);
        }
        for (FeatureLogos logo : nullToEmpty(item.featureLogos)) {
            if (logo == null || StringUtils.isBlank(logo.logoPic)) {
                continue;
            }
            String fieldKey = "logo:" + firstNonBlank(logo.featureID, String.valueOf(ordinals.next("logo")));
            addMediaAssertion(assertions, ordinals, key, fieldKey, logo.logoPic, null, "logo", LanguageTag.UND, schemaVersion);
        }
        Description description = item.generalInfo == null ? null : item.generalInfo.description;
        if (description != null && StringUtils.isNotBlank(description.leafletPDFURL)) {
            addMediaAssertion(assertions, ordinals, key, "document:leaflet", description.leafletPDFURL, "application/pdf",
                    "leaflet", LanguageTag.UND, schemaVersion);
        }
        if (description != null && StringUtils.isNotBlank(description.manualPDFURL)) {
            addMediaAssertion(assertions, ordinals, key, "document:manual", description.manualPDFURL, "application/pdf",
                    "manual", LanguageTag.UND, schemaVersion);
        }
    }

    private void addMediaAssertion(List<SourceAssertion> assertions, Ordinals ordinals, SourceRecordKey key, String fieldKey,
            String url, String mediaType, String role, LanguageTag language, String schemaVersion) {
        URI uri = toAbsoluteUri(url);
        if (uri == null) {
            return;
        }
        SourceFieldId field = new SourceFieldId(FIELD_NAMESPACE, fieldKey, schemaVersion);
        SourceEvidence evidence = new MediaEvidence(uri, StringUtils.trimToNull(mediaType), StringUtils.trimToNull(role), language);
        assertions.add(SourceAssertion.of(key, field, ordinals.next(fieldKey), SourceContentType.MEDIA, evidence));
    }

    private void addScalar(List<SourceAssertion> assertions, Ordinals ordinals, SourceRecordKey key, String fieldKey,
            String value, String unit, String schemaVersion, SourceContentType contentType) {
        if (StringUtils.isBlank(value)) {
            return;
        }
        SourceFieldId field = new SourceFieldId(FIELD_NAMESPACE, fieldKey, schemaVersion);
        assertions.add(SourceAssertion.of(key, field, ordinals.next(fieldKey), contentType,
                new ScalarEvidence(value.trim(), unit, LanguageTag.UND)));
    }

    private void addLocalizedText(List<SourceAssertion> assertions, Ordinals ordinals, SourceRecordKey key, String fieldKey,
            String value, LanguageTag language, String schemaVersion, SourceContentType contentType) {
        if (StringUtils.isBlank(value)) {
            return;
        }
        String composedKey = fieldKey + "#" + language.value();
        SourceFieldId field = new SourceFieldId(FIELD_NAMESPACE, composedKey, schemaVersion);
        assertions.add(SourceAssertion.of(key, field, ordinals.next(composedKey), contentType,
                new LocalizedTextEvidence(value.trim(), language)));
    }

    private List<GtinLink> gtinLinks(GeneralInfo info) {
        if (info == null) {
            return List.of();
        }
        Map<String, GtinLink> links = new LinkedHashMap<>();
        for (String gtin : nullToEmpty(info.gtin)) {
            addGtinLink(links, gtin, GtinMatchConfidence.EXACT);
        }
        for (GTIN gtin : nullToEmpty(info.gtins)) {
            if (gtin == null) {
                continue;
            }
            addGtinLink(links, gtin.gtin, gtin.isApproved ? GtinMatchConfidence.EXACT : GtinMatchConfidence.UNVERIFIED);
        }
        return List.copyOf(links.values());
    }

    private void addGtinLink(Map<String, GtinLink> links, String rawGtin, GtinMatchConfidence confidence) {
        if (StringUtils.isBlank(rawGtin)) {
            return;
        }
        String trimmed = rawGtin.trim();
        try {
            GtinLink candidate = new GtinLink(new Gtin(trimmed), confidence, GtinMatchMethod.DECLARED_IDENTIFIER, null);
            GtinLink existing = links.get(trimmed);
            if (existing == null || confidence.ordinal() < existing.confidence().ordinal()) {
                links.put(trimmed, candidate);
            }
        } catch (IllegalArgumentException exception) {
            // Not a valid GTIN checksum/shape; skip rather than attach a broken identity link.
        }
    }

    private URI evidenceReference(SourceRecordKey key) {
        return URI.create(EVIDENCE_BASE + encode(key.sourceRecordId().value()));
    }

    private URI toAbsoluteUri(String url) {
        try {
            URI uri = URI.create(url.trim());
            return uri.isAbsolute() ? uri : null;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private LanguageTag languageOrDefault(String rawLanguage, LanguageTag fallback) {
        if (StringUtils.isBlank(rawLanguage)) {
            return fallback;
        }
        try {
            return new LanguageTag(rawLanguage);
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private static <T> List<T> nullToEmpty(List<T> values) {
        return values == null ? List.of() : values;
    }

    private PayloadHash payloadHash(
            SourceRecordKey key, String schemaVersion, String providerVersion, List<GtinLink> gtinLinks,
            List<SourceAssertion> assertions, SourceRecordState state) {
        try {
            byte[] bytes = DataReferenceJson.mapper()
                    .writeValueAsBytes(new HashInput(key, schemaVersion, providerVersion, gtinLinks, assertions, state));
            return new PayloadHash("SHA-256", HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("the JDK must provide SHA-256", exception);
        }
    }

    private static String encode(String value) {
        return HexFormat.of().formatHex(value.getBytes(StandardCharsets.UTF_8));
    }

    /** Assigns stable, per-field-key ordinals so repeated values keep provider order. */
    private static final class Ordinals {
        private final Map<String, Integer> next = new LinkedHashMap<>();

        int next(String fieldKey) {
            return next.merge(fieldKey, 0, (existing, increment) -> existing + 1);
        }
    }

    private record HashInput(
            SourceRecordKey key,
            String schemaVersion,
            String providerVersion,
            List<GtinLink> gtinLinks,
            List<SourceAssertion> assertions,
            SourceRecordState state) {
    }
}
