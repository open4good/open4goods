package org.open4goods.api.services.migration.legacybackup;

import java.math.BigDecimal;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Currency;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.open4goods.api.services.migration.legacybackup.LegacyBackupInputManifest.ConversionRules;
import org.open4goods.datareference.model.Gtin;
import org.open4goods.datareference.model.GtinLink;
import org.open4goods.datareference.model.GtinMatchConfidence;
import org.open4goods.datareference.model.GtinMatchMethod;
import org.open4goods.datareference.model.PayloadHash;
import org.open4goods.datareference.model.SourceAssertion;
import org.open4goods.datareference.model.SourceContentType;
import org.open4goods.datareference.model.SourceFieldId;
import org.open4goods.datareference.model.SourceRecordCompleteness;
import org.open4goods.datareference.model.SourceRecordHead;
import org.open4goods.datareference.model.SourceRecordKey;
import org.open4goods.datareference.model.SourceRecordMutation;
import org.open4goods.datareference.model.SourceRecordState;
import org.open4goods.datareference.model.evidence.ScalarEvidence;
import org.open4goods.datareference.model.evidence.SourceEvidence;
import org.open4goods.pricehistory.model.LegacyMinimumPricePoint;
import org.open4goods.pricehistory.model.OfferCondition;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Converts one legacy backup JSONL line into a source-record mutation and, when present, a
 * legacy minimum price point, applying the reviewed field-disposition map field by field.
 *
 * <p>This first slice converts GTIN identity and the small reviewed subset of directly
 * convertible native scalar fields (name, brand, model, ...). Full per-legacy-attribute mapping
 * through the 94-entry registry is explicitly deferred; affected fields are reported as
 * non-fatal {@link LegacyBackupDeadLetterReason#ATTRIBUTE_MAPPING_DEFERRED} dead letters rather
 * than silently dropped, so nothing observed disappears from operator visibility.
 */
public final class LegacyBackupRecordConverter {

    /** Currency assumed for legacy minimum price points; the legacy Product store was EUR-only. */
    private static final Currency LEGACY_PRICE_CURRENCY = Currency.getInstance("EUR");

    private static final List<String> GTIN_FIELDS = List.of("gtin", "ean", "ean13", "barcode");

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ConversionRules conversionRules;

    public LegacyBackupRecordConverter(ConversionRules conversionRules) {
        this.conversionRules = conversionRules;
    }

    /**
     * Converts one raw JSONL line.
     *
     * @param datasetId dataset the line belongs to
     * @param fileName archive file the line came from
     * @param lineNumber 1-based line position within {@code fileName}
     * @param rawLine raw JSONL line text
     * @param observedAt instant shared by every record in this pinned backup generation
     * @return the conversion outcome
     */
    public LegacyBackupRecordConversion convert(
            String datasetId, String fileName, long lineNumber, String rawLine, Instant observedAt) {
        JsonNode record;
        try {
            record = MAPPER.readTree(rawLine);
        } catch (JsonProcessingException exception) {
            return unconvertible(datasetId, fileName, lineNumber,
                    LegacyBackupDeadLetterReason.MALFORMED_JSON_LINE, "invalid JSON", observedAt);
        }
        if (record == null || !record.isObject()) {
            return unconvertible(datasetId, fileName, lineNumber,
                    LegacyBackupDeadLetterReason.MALFORMED_JSON_LINE, "line is not a JSON object", observedAt);
        }

        Optional<Gtin> gtin = extractGtin(record);
        if (gtin.isEmpty()) {
            return unconvertible(datasetId, fileName, lineNumber,
                    LegacyBackupDeadLetterReason.MISSING_OR_INVALID_GTIN, "no valid GTIN field", observedAt);
        }

        List<LegacyBackupDeadLetter> deadLetters = new ArrayList<>();
        List<SourceAssertion> assertions = new ArrayList<>();
        Optional<LegacyMinimumPricePoint> price = Optional.empty();

        SourceRecordKey key = SourceRecordKey.of(LegacyBackupUsagePolicy.SOURCE_ID.value(), "gtin:" + gtin.orElseThrow().value());

        Iterator<Map.Entry<String, JsonNode>> fields = record.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String fieldName = field.getKey();
            JsonNode value = field.getValue();
            if (value == null || value.isNull()) {
                continue;
            }
            LegacyFieldDisposition disposition = LegacyBackupFieldDispositionRegistry.classify(fieldName);
            switch (disposition) {
                case UNATTRIBUTED_IDENTITY, RECOMPUTED -> {
                    // Never converted: identity fields are represented only via the GTIN link,
                    // and recomputed fields are derived downstream, never carried over.
                }
                case QUARANTINE -> {
                    if (fieldName.toLowerCase(Locale.ROOT).contains("amazon")) {
                        deadLetters.add(deadLetter(datasetId, fileName, lineNumber,
                                LegacyBackupDeadLetterReason.FORBIDDEN_SOURCE_CONTENT, fieldName, observedAt));
                    }
                }
                case LEGACY_MINIMUM_PRICE -> {
                    if (price.isEmpty()) {
                        price = extractPrice(gtin.orElseThrow(), fieldName, value, key, observedAt);
                    }
                }
                case NATIVE_EVIDENCED_CONVERSION -> {
                    if (LegacyBackupFieldDispositionRegistry.isDirectlyConvertibleNativeField(fieldName) && value.isValueNode()) {
                        assertions.add(nativeAssertion(key, fieldName, value));
                    } else {
                        deadLetters.add(deadLetter(datasetId, fileName, lineNumber,
                                LegacyBackupDeadLetterReason.ATTRIBUTE_MAPPING_DEFERRED, fieldName, observedAt));
                    }
                }
            }
        }

        SourceRecordHead head = new SourceRecordHead(
                key,
                conversionRules.mappingVersion(),
                null,
                observedAt,
                observedAt,
                null,
                SourceRecordCompleteness.FULL,
                SourceRecordState.ACTIVE,
                payloadHash(key, assertions),
                URI.create("urn:o4g:legacy-backup:" + encode(datasetId) + ":" + encode(fileName) + ":" + lineNumber),
                LegacyBackupUsagePolicy.POLICY.reference(),
                List.of(new GtinLink(gtin.orElseThrow(), GtinMatchConfidence.EXACT, GtinMatchMethod.DECLARED_IDENTIFIER, null)),
                List.copyOf(assertions));

        return new LegacyBackupRecordConversion(
                Optional.of(SourceRecordMutation.full(head)), price, deadLetters);
    }

    private Optional<Gtin> extractGtin(JsonNode record) {
        for (String field : GTIN_FIELDS) {
            if (record.hasNonNull(field)) {
                Optional<Gtin> candidate = LegacyGtinValidator.validate(record.get(field).asText());
                if (candidate.isPresent()) {
                    return candidate;
                }
            }
        }
        JsonNode gtinInfos = record.get("gtinInfos");
        if (gtinInfos != null && gtinInfos.isObject()) {
            if (gtinInfos.hasNonNull("normalizedGtin14")) {
                Optional<Gtin> candidate = LegacyGtinValidator.validate(gtinInfos.get("normalizedGtin14").asText());
                if (candidate.isPresent()) {
                    return candidate;
                }
            }
            JsonNode strings = gtinInfos.get("gtinStrings");
            if (strings != null && strings.isArray()) {
                for (JsonNode candidateNode : strings) {
                    Optional<Gtin> candidate = LegacyGtinValidator.validate(candidateNode.asText());
                    if (candidate.isPresent()) {
                        return candidate;
                    }
                }
            }
        }
        return Optional.empty();
    }

    private Optional<LegacyMinimumPricePoint> extractPrice(
            Gtin gtin, String fieldName, JsonNode value, SourceRecordKey key, Instant observedAt) {
        if (!value.isNumber()) {
            return Optional.empty();
        }
        BigDecimal amount = value.decimalValue();
        if (amount.signum() < 0) {
            return Optional.empty();
        }
        return Optional.of(new LegacyMinimumPricePoint(
                gtin, OfferCondition.UNKNOWN, LEGACY_PRICE_CURRENCY, amount, observedAt,
                LegacyBackupUsagePolicy.POLICY.reference(), key.externalForm() + "#" + fieldName));
    }

    private SourceAssertion nativeAssertion(SourceRecordKey key, String fieldName, JsonNode value) {
        SourceFieldId field = new SourceFieldId("legacy-backup", fieldName, conversionRules.mappingVersion());
        SourceEvidence evidence = ScalarEvidence.of(value.asText());
        return SourceAssertion.of(key, field, 0, SourceContentType.TEXT, evidence);
    }

    private LegacyBackupRecordConversion unconvertible(
            String datasetId, String fileName, long lineNumber, LegacyBackupDeadLetterReason reason, String detail,
            Instant recordedAt) {
        return new LegacyBackupRecordConversion(Optional.empty(), Optional.empty(),
                List.of(deadLetter(datasetId, fileName, lineNumber, reason, detail, recordedAt)));
    }

    private LegacyBackupDeadLetter deadLetter(
            String datasetId, String fileName, long lineNumber, LegacyBackupDeadLetterReason reason, String detail,
            Instant recordedAt) {
        return new LegacyBackupDeadLetter(datasetId, fileName, lineNumber, reason, detail, recordedAt);
    }

    private PayloadHash payloadHash(SourceRecordKey key, List<SourceAssertion> assertions) {
        try {
            StringBuilder material = new StringBuilder(key.externalForm());
            for (SourceAssertion assertion : assertions) {
                material.append('\u0000').append(assertion.field().externalForm()).append('=').append(assertion.evidence());
            }
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(material.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return new PayloadHash("SHA-256", HexFormat.of().formatHex(digest));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("the JDK must provide SHA-256", exception);
        }
    }

    private static String encode(String value) {
        return HexFormat.of().formatHex(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
