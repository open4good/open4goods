package org.open4goods.services.eprelservice.service;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

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
import org.open4goods.datareference.model.evidence.ScalarEvidence;
import org.open4goods.datareference.model.evidence.SourceEvidence;
import org.open4goods.datareference.serialization.DataReferenceJson;
import org.open4goods.model.eprel.EprelProduct;
import org.springframework.stereotype.Component;

/**
 * Converts one EPREL catalogue row into a replaceable, source-neutral record head.
 *
 * <p>The adapter deliberately retains EPREL parameter names as source field ids. It does not
 * infer an O4G attribute from a label, select a winning value, or write a legacy product. Those
 * are registry and resolution concerns that run after the source boundary.
 */
@Component
public class EprelSourceRecordAdapter {

    /** EPREL source identity used by the policy and source-record contracts. */
    public static final String SOURCE_ID = "eprel";
    /** Policy reference is intentionally deny-by-default until the owner approves it. */
    public static final SourceUsagePolicyRef USAGE_POLICY = new SourceUsagePolicyRef("eprel-public-api", "1");

    private static final String FIELD_NAMESPACE = "eprel";
    private static final URI EVIDENCE_BASE = URI.create("urn:o4g:eprel:record:");

    /**
     * Maps a complete catalogue row to a FULL source-record mutation.
     *
     * @param product parsed EPREL row
     * @param schemaVersion EPREL catalogue schema version selected by the importer
     * @param retrievedAt instant at which this catalogue was retrieved
     * @return a full replacement, or empty when EPREL supplied no stable model record identifier
     */
    public Optional<SourceRecordMutation> adapt(EprelProduct product, String schemaVersion, Instant retrievedAt) {
        Objects.requireNonNull(product, "product must not be null");
        Objects.requireNonNull(schemaVersion, "schemaVersion must not be null");
        Objects.requireNonNull(retrievedAt, "retrievedAt must not be null");

        Optional<String> recordId = recordId(product);
        if (recordId.isEmpty()) {
            return Optional.empty();
        }

        SourceRecordKey key = SourceRecordKey.of(SOURCE_ID, recordId.orElseThrow());
        List<SourceAssertion> assertions = assertions(product, key, schemaVersion);
        Instant observedAt = observedAt(product, retrievedAt);
        SourceRecordState state = state(product);
        SourceRecordHead head = new SourceRecordHead(
                key,
                schemaVersion,
                providerVersion(product),
                observedAt,
                retrievedAt,
                null,
                SourceRecordCompleteness.FULL,
                state,
                payloadHash(key, schemaVersion, providerVersion(product), gtinLinks(product), assertions, state),
                URI.create(EVIDENCE_BASE + encode(recordId.orElseThrow())),
                USAGE_POLICY,
                state == SourceRecordState.DELETED ? List.of() : gtinLinks(product),
                state == SourceRecordState.DELETED ? List.of() : assertions);
        return Optional.of(SourceRecordMutation.full(head));
    }

    private Optional<String> recordId(EprelProduct product) {
        if (hasText(product.getEprelRegistrationNumber())) {
            return Optional.of(product.getEprelRegistrationNumber());
        }
        if (product.getProductModelCoreId() != null) {
            return Optional.of("model:" + product.getProductModelCoreId());
        }
        return Optional.empty();
    }

    private List<SourceAssertion> assertions(EprelProduct product, SourceRecordKey key, String schemaVersion) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        put(parameters, "eprelRegistrationNumber", product.getEprelRegistrationNumber());
        put(parameters, "productGroup", product.getProductGroup());
        put(parameters, "implementingAct", product.getImplementingAct());
        put(parameters, "supplierOrTrademark", product.getSupplierOrTrademark());
        put(parameters, "modelIdentifier", product.getModelIdentifier());
        put(parameters, "versionId", product.getVersionId());
        put(parameters, "versionNumber", product.getVersionNumber());
        put(parameters, "energyClass", product.getEnergyClass());
        put(parameters, "formType", product.getFormType());
        put(parameters, "status", product.getStatus());
        put(parameters, "gtinIdentifier", product.getGtinIdentifier());
        put(parameters, "eprelCategories", product.getEprelCategories());
        product.getCategorySpecificAttributes().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> put(parameters, entry.getKey(), entry.getValue()));

        List<SourceAssertion> assertions = new ArrayList<>();
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            SourceFieldId field = new SourceFieldId(FIELD_NAMESPACE, entry.getKey(), schemaVersion);
            List<SourceEvidence> values = evidence(entry.getKey(), entry.getValue());
            for (int ordinal = 0; ordinal < values.size(); ordinal++) {
                assertions.add(SourceAssertion.of(key, field, ordinal, contentType(entry.getKey()), values.get(ordinal)));
            }
        }
        return List.copyOf(assertions);
    }

    private List<SourceEvidence> evidence(String parameter, Object value) {
        if (value instanceof List<?> values) {
            List<SourceEvidence> result = new ArrayList<>();
            for (Object element : values) {
                result.addAll(evidence(parameter, element));
            }
            return result;
        }
        if (value instanceof Map<?, ?> values && hasScalarUnit(values)) {
            return List.of(new ScalarEvidence(String.valueOf(values.get("value")), String.valueOf(values.get("unit")),
                    LanguageTag.UND));
        }
        if (value instanceof Map<?, ?> values && isLocalizedText(values)) {
            List<SourceEvidence> result = new ArrayList<>();
            List<Map.Entry<?, ?>> entries = new ArrayList<>(values.entrySet());
            entries.sort(java.util.Comparator.comparing(entry -> (String) entry.getKey()));
            for (Map.Entry<?, ?> entry : entries) {
                result.add(new LocalizedTextEvidence((String) entry.getValue(), new LanguageTag((String) entry.getKey())));
            }
            return result;
        }
        String lexicalValue = value instanceof Map<?, ?>
                ? DataReferenceJson.mapper().writeValueAsString(sortedMap((Map<?, ?>) value))
                : String.valueOf(value);
        return List.of(new ScalarEvidence(lexicalValue, null, LanguageTag.UND));
    }

    private boolean isLocalizedText(Map<?, ?> values) {
        if (values.isEmpty()) {
            return false;
        }
        for (Map.Entry<?, ?> entry : values.entrySet()) {
            if (!(entry.getKey() instanceof String tag) || !(entry.getValue() instanceof String)) {
                return false;
            }
            try {
                new LanguageTag(tag);
            } catch (IllegalArgumentException exception) {
                return false;
            }
        }
        return true;
    }

    private boolean hasScalarUnit(Map<?, ?> values) {
        return values.size() == 2 && values.get("value") != null && values.get("unit") instanceof String unit
                && !unit.isBlank();
    }

    private Map<String, Object> sortedMap(Map<?, ?> values) {
        Map<String, Object> result = new java.util.TreeMap<>();
        for (Map.Entry<?, ?> entry : values.entrySet()) {
            result.put(String.valueOf(entry.getKey()), entry.getValue() instanceof Map<?, ?> nested
                    ? sortedMap(nested) : entry.getValue());
        }
        return result;
    }

    private SourceContentType contentType(String parameter) {
        return switch (parameter) {
            case "productGroup", "eprelCategories" -> SourceContentType.CLASSIFICATION;
            case "eprelRegistrationNumber", "supplierOrTrademark", "modelIdentifier", "gtinIdentifier" ->
                SourceContentType.IDENTITY;
            default -> SourceContentType.ATTRIBUTE;
        };
    }

    private List<GtinLink> gtinLinks(EprelProduct product) {
        if (!hasText(product.getGtinIdentifier())) {
            return List.of();
        }
        String gtin = product.getGtinIdentifier().trim();
        try {
            return List.of(new GtinLink(new Gtin(gtin), GtinMatchConfidence.EXACT,
                    GtinMatchMethod.DECLARED_IDENTIFIER, null));
        } catch (IllegalArgumentException exception) {
            return List.of();
        }
    }

    private Instant observedAt(EprelProduct product, Instant retrievedAt) {
        Long timestamp = firstPresent(product.getPublishedOnDateTs(), product.getPublishedOnDate(), product.getExportDateTs(),
                product.getImportedOn());
        if (timestamp == null || timestamp <= 0) {
            return retrievedAt;
        }
        Instant observed = timestamp > 10_000_000_000L ? Instant.ofEpochMilli(timestamp) : Instant.ofEpochSecond(timestamp);
        return observed.isAfter(retrievedAt) ? retrievedAt : observed;
    }

    private String providerVersion(EprelProduct product) {
        if (product.getVersionId() != null) {
            return "version-id:" + product.getVersionId();
        }
        return product.getVersionNumber() == null ? null : "version-number:" + product.getVersionNumber().toPlainString();
    }

    private SourceRecordState state(EprelProduct product) {
        if (!hasText(product.getStatus())) {
            return SourceRecordState.ACTIVE;
        }
        String status = product.getStatus().trim().toLowerCase(Locale.ROOT);
        return status.contains("withdraw") || status.contains("delete") || status.contains("remov")
                ? SourceRecordState.DELETED : SourceRecordState.ACTIVE;
    }

    private PayloadHash payloadHash(
            SourceRecordKey key,
            String schemaVersion,
            String providerVersion,
            List<GtinLink> gtinLinks,
            List<SourceAssertion> assertions,
            SourceRecordState state) {
        try {
            byte[] bytes = DataReferenceJson.mapper()
                    .writeValueAsBytes(new HashInput(key, schemaVersion, providerVersion, gtinLinks, assertions, state));
            return new PayloadHash("SHA-256", HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("the JDK must provide SHA-256", exception);
        }
    }

    private static Long firstPresent(Long... values) {
        for (Long value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static void put(Map<String, Object> target, String name, Object value) {
        if (value != null && (!(value instanceof String text) || !text.isBlank())
                && (!(value instanceof List<?> list) || !list.isEmpty())) {
            target.put(name, value);
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String encode(String value) {
        return HexFormat.of().formatHex(value.getBytes(StandardCharsets.UTF_8));
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
