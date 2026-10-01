package org.open4goods.api.services.feed;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Currency;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

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
import org.open4goods.datareference.model.SourceRecordId;
import org.open4goods.datareference.model.SourceRecordKey;
import org.open4goods.datareference.model.SourceRecordMutation;
import org.open4goods.datareference.model.SourceRecordState;
import org.open4goods.datareference.model.SourceUsagePolicyRef;
import org.open4goods.datareference.model.evidence.LocalizedTextEvidence;
import org.open4goods.datareference.model.evidence.ScalarEvidence;
import org.open4goods.datareference.model.evidence.SourceEvidence;
import org.open4goods.datareference.serialization.DataReferenceJson;
import org.open4goods.pricehistory.model.OfferAvailability;
import org.open4goods.pricehistory.model.OfferCondition;
import org.open4goods.pricehistory.model.OfferKey;
import org.open4goods.pricehistory.model.OfferObservation;
import org.open4goods.services.feedservice.definition.ColumnResolution;
import org.open4goods.services.feedservice.definition.ColumnTarget;
import org.open4goods.services.feedservice.definition.FeedDefinition;
import org.springframework.stereotype.Component;

/**
 * Translates one {@link FeedDefinition}-resolved feed row into the data-reference and
 * price-history contracts, replacing the legacy {@code DataFragmentStoreService} content-hash
 * path for merchant feeds.
 *
 * <p>{@link ColumnTarget.ReferenceField}-mapped columns become {@link SourceAssertion} entries
 * on a {@link SourceRecordHead}; {@link ColumnTarget.OfferField}-mapped columns (price,
 * availability, offer condition) become one {@link OfferObservation}, keyed by a
 * {@code providerOfferId} derived from the feed's own key columns. The two never cross: a
 * reference field is never embedded in the offer observation and an offer field never produces a
 * {@link SourceAssertion} (AC3), because each column's {@link ColumnTarget} variant is a sealed
 * choice between the two shapes.
 *
 * <p>Both outputs carry their own content digest ({@link SourceRecordHead#payloadHash()} and
 * {@link OfferObservation#contentHash()}); this adapter never reads or writes
 * {@code Product.datasourceCodes} (AC6). Persisting the translated head and observation through
 * {@code SourceRecordHeadStore} and {@code PriceObservationService} is the caller's
 * responsibility.
 */
@Component
public class FeedRowReferenceOfferAdapter {

    private static final URI EVIDENCE_BASE = URI.create("urn:o4g:feed:record:");

    /**
     * Translates one resolved row.
     *
     * @param definition feed's stable column contract
     * @param resolution header-to-{@link ColumnTarget} resolution for this feed's actual headers
     * @param row one data row, keyed by actual header name
     * @param gtin product identity the row was attached to, resolved by the caller
     * @param currency currency the feed's price column is expressed in
     * @param retrievedAt instant this row was retrieved
     * @param usagePolicyRef usage policy in force for this feed at retrieval time
     * @return the reference mutation and/or offer observation this row produces; either side is
     *         empty when the row carries no columns of that kind
     */
    public FeedRowTranslation translate(
            FeedDefinition definition,
            ColumnResolution resolution,
            Map<String, String> row,
            Gtin gtin,
            Currency currency,
            Instant retrievedAt,
            SourceUsagePolicyRef usagePolicyRef) {
        Objects.requireNonNull(definition, "definition must not be null");
        Objects.requireNonNull(resolution, "resolution must not be null");
        Objects.requireNonNull(row, "row must not be null");
        Objects.requireNonNull(gtin, "gtin must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
        Objects.requireNonNull(retrievedAt, "retrievedAt must not be null");
        Objects.requireNonNull(usagePolicyRef, "usagePolicyRef must not be null");

        String providerRecordId = providerRecordId(definition, row);
        Optional<SourceRecordMutation> referenceMutation =
                referenceMutation(definition, resolution, row, gtin, providerRecordId, retrievedAt, usagePolicyRef);
        Optional<OfferObservation> offerObservation =
                offerObservation(definition, resolution, row, gtin, currency, providerRecordId, retrievedAt, usagePolicyRef);
        return new FeedRowTranslation(referenceMutation, offerObservation);
    }

    /**
     * Derives the stable per-row identity shared by the source record and the offer, from the
     * feed's declared key columns. Stable because the key columns are the feed's own contract for
     * record identity (AC1); the same row always yields the same id.
     */
    private String providerRecordId(FeedDefinition definition, Map<String, String> row) {
        List<String> parts = new ArrayList<>();
        for (String keyColumn : definition.keyColumns()) {
            String value = row.get(keyColumn);
            if (StringUtils.isBlank(value)) {
                throw new IllegalArgumentException("row is missing a value for key column " + keyColumn);
            }
            parts.add(value.trim());
        }
        return String.join("|", parts);
    }

    private Optional<SourceRecordMutation> referenceMutation(
            FeedDefinition definition, ColumnResolution resolution, Map<String, String> row, Gtin gtin,
            String providerRecordId, Instant retrievedAt, SourceUsagePolicyRef usagePolicyRef) {
        boolean feedHasReferenceColumns =
                resolution.matchedTargets().values().stream().anyMatch(ColumnTarget.ReferenceField.class::isInstance);
        if (!feedHasReferenceColumns) {
            return Optional.empty();
        }
        SourceRecordKey key = SourceRecordKey.of(definition.sourceId().value(), providerRecordId);
        List<SourceAssertion> assertions = referenceAssertions(definition, resolution, row, key);
        List<GtinLink> gtinLinks = List.of(new GtinLink(gtin, GtinMatchConfidence.EXACT, GtinMatchMethod.DECLARED_IDENTIFIER, null));

        SourceRecordHead head = new SourceRecordHead(
                key,
                definition.providerSchemaVersion(),
                null,
                retrievedAt,
                retrievedAt,
                null,
                SourceRecordCompleteness.FULL,
                SourceRecordState.ACTIVE,
                payloadHash(key, definition.providerSchemaVersion(), gtinLinks, assertions, SourceRecordState.ACTIVE),
                evidenceReference(key),
                usagePolicyRef,
                gtinLinks,
                assertions);
        return Optional.of(SourceRecordMutation.full(head));
    }

    private List<SourceAssertion> referenceAssertions(
            FeedDefinition definition, ColumnResolution resolution, Map<String, String> row, SourceRecordKey key) {
        List<SourceAssertion> assertions = new ArrayList<>();
        Map<String, Integer> ordinals = new LinkedHashMap<>();
        for (Map.Entry<String, ColumnTarget> entry : resolution.matchedTargets().entrySet()) {
            if (!(entry.getValue() instanceof ColumnTarget.ReferenceField referenceField)) {
                // ColumnTarget.OfferField columns never produce a SourceAssertion (AC3).
                continue;
            }
            String rawValue = row.get(entry.getKey());
            if (StringUtils.isBlank(rawValue)) {
                continue;
            }
            SourceFieldId fieldId = new SourceFieldId(
                    definition.sourceId().value(), referenceField.canonicalFieldId(), definition.providerSchemaVersion());
            int ordinal = ordinals.merge(referenceField.canonicalFieldId(), 0, (existing, increment) -> existing + 1);
            SourceEvidence evidence = referenceField.contentType() == SourceContentType.TEXT
                    ? new LocalizedTextEvidence(rawValue.trim(), languageTag(definition.language()))
                    : new ScalarEvidence(rawValue.trim(), definition.unitColumns().get(entry.getKey()), LanguageTag.UND);
            assertions.add(SourceAssertion.of(key, fieldId, ordinal, referenceField.contentType(), evidence));
        }
        return List.copyOf(assertions);
    }

    private Optional<OfferObservation> offerObservation(
            FeedDefinition definition, ColumnResolution resolution, Map<String, String> row, Gtin gtin,
            Currency currency, String providerRecordId, Instant retrievedAt, SourceUsagePolicyRef usagePolicyRef) {
        String priceColumn = columnFor(resolution, ColumnTarget.OfferFieldKind.PRICE);
        if (priceColumn == null) {
            return Optional.empty();
        }
        String rawPrice = row.get(priceColumn);
        if (StringUtils.isBlank(rawPrice)) {
            return Optional.empty();
        }
        java.math.BigDecimal amount;
        try {
            amount = new java.math.BigDecimal(rawPrice.trim().replace(',', '.'));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("column " + priceColumn + " does not carry a numeric price: " + rawPrice, exception);
        }

        String conditionColumn = columnFor(resolution, ColumnTarget.OfferFieldKind.OFFER_CONDITION);
        String availabilityColumn = columnFor(resolution, ColumnTarget.OfferFieldKind.AVAILABILITY);
        OfferCondition condition = condition(conditionColumn == null ? null : row.get(conditionColumn));
        OfferAvailability availability = availability(availabilityColumn == null ? null : row.get(availabilityColumn));

        OfferKey offerKey = new OfferKey(gtin, definition.sourceId(), providerRecordId);
        PayloadHash contentHash = offerPayloadHash(offerKey, condition, currency, amount, availability);
        OfferObservation observation = new OfferObservation(
                offerKey, condition, currency, amount, availability, retrievedAt, retrievedAt, contentHash, usagePolicyRef);
        return Optional.of(observation);
    }

    private String columnFor(ColumnResolution resolution, ColumnTarget.OfferFieldKind kind) {
        for (Map.Entry<String, ColumnTarget> entry : resolution.matchedTargets().entrySet()) {
            if (entry.getValue() instanceof ColumnTarget.OfferField offerField && offerField.field() == kind) {
                // ColumnTarget.ReferenceField columns never feed an offer field (AC3).
                return entry.getKey();
            }
        }
        return null;
    }

    private OfferCondition condition(String rawValue) {
        if (StringUtils.isBlank(rawValue)) {
            return OfferCondition.UNKNOWN;
        }
        String normalized = rawValue.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "new", "neuf" -> OfferCondition.NEW;
            case "occasion", "used", "refurbished" -> OfferCondition.OCCASION;
            default -> OfferCondition.UNKNOWN;
        };
    }

    private OfferAvailability availability(String rawValue) {
        if (StringUtils.isBlank(rawValue)) {
            return OfferAvailability.UNKNOWN;
        }
        String normalized = rawValue.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "true", "1", "yes", "available", "in_stock" -> OfferAvailability.AVAILABLE;
            case "false", "0", "no", "unavailable", "out_of_stock" -> OfferAvailability.UNAVAILABLE;
            default -> OfferAvailability.UNKNOWN;
        };
    }

    private LanguageTag languageTag(java.util.Locale locale) {
        try {
            return new LanguageTag(locale.toLanguageTag());
        } catch (IllegalArgumentException exception) {
            return LanguageTag.UND;
        }
    }

    private URI evidenceReference(SourceRecordKey key) {
        return URI.create(EVIDENCE_BASE + encode(key.sourceRecordId().value()));
    }

    private static String encode(String value) {
        return HexFormat.of().formatHex(value.getBytes(StandardCharsets.UTF_8));
    }

    private PayloadHash payloadHash(
            SourceRecordKey key, String schemaVersion, List<GtinLink> gtinLinks, List<SourceAssertion> assertions,
            SourceRecordState state) {
        try {
            byte[] bytes = DataReferenceJson.mapper()
                    .writeValueAsBytes(new ReferenceHashInput(key, schemaVersion, gtinLinks, assertions, state));
            return new PayloadHash("SHA-256", HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("the JDK must provide SHA-256", exception);
        }
    }

    private PayloadHash offerPayloadHash(
            OfferKey key, OfferCondition condition, Currency currency, java.math.BigDecimal amount, OfferAvailability availability) {
        try {
            byte[] bytes = DataReferenceJson.mapper()
                    .writeValueAsBytes(new OfferHashInput(key, condition, currency.getCurrencyCode(), amount.toPlainString(), availability));
            return new PayloadHash("SHA-256", HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("the JDK must provide SHA-256", exception);
        }
    }

    private record ReferenceHashInput(
            SourceRecordKey key, String schemaVersion, List<GtinLink> gtinLinks, List<SourceAssertion> assertions,
            SourceRecordState state) {
    }

    private record OfferHashInput(
            OfferKey key, OfferCondition condition, String currencyCode, String amount, OfferAvailability availability) {
    }

    /**
     * Outcome of translating one feed row.
     *
     * @param referenceMutation reference-field assertions for this row, when any were mapped
     * @param offerObservation price/availability/condition observation for this row, when a price
     *        column was mapped and carried a value
     */
    public record FeedRowTranslation(Optional<SourceRecordMutation> referenceMutation, Optional<OfferObservation> offerObservation) {
    }
}
