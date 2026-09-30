package org.open4goods.datareference.service;

import java.math.BigDecimal;
import java.text.ParsePosition;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.open4goods.datareference.model.CanonicalAttributeId;
import org.open4goods.datareference.model.LanguageTag;
import org.open4goods.datareference.model.RuleVersion;
import org.open4goods.datareference.model.SourceAssertion;
import org.open4goods.datareference.model.SourceContentType;
import org.open4goods.datareference.model.SourceId;
import org.open4goods.datareference.model.UcumCode;
import org.open4goods.datareference.model.evidence.ScalarEvidence;
import org.open4goods.datareference.model.normalization.LanguageNormalization;
import org.open4goods.datareference.model.normalization.NormalizationResult;
import org.open4goods.datareference.model.normalization.NormalizationRequest;
import org.open4goods.datareference.model.normalization.NormalizationStatus;
import org.open4goods.datareference.model.registry.CanonicalAttributeDefinition;
import org.open4goods.datareference.model.registry.RegistryExternalMapping;
import org.open4goods.datareference.model.value.CanonicalValueType;
import org.open4goods.datareference.model.value.QuantityValue;
import org.open4goods.datareference.model.resolution.NormalizedValue;
import org.open4goods.datareference.port.CanonicalRegistryLookup;
import org.open4goods.datareference.port.NormalizationPort;

import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.util.ULocale;

/**
 * Lossless, locale-aware quantity normalizer shared by every new source adapter.
 *
 * <p>The service reads but never alters the assertion's scalar evidence. A
 * failed parse is returned as a typed result, so callers cannot accidentally
 * replace an unknown unit or malformed localized number with a guessed value.
 */
public final class SharedNormalizationService implements NormalizationPort {

    private static final RuleVersion RULE = new RuleVersion("quantity-normalization", 1);
    private static final Map<String, UcumCode> COMMON_UNITS = Map.ofEntries(
            Map.entry("m", new UcumCode("m")), Map.entry("metre", new UcumCode("m")),
            Map.entry("meter", new UcumCode("m")), Map.entry("cm", new UcumCode("cm")),
            Map.entry("centimetre", new UcumCode("cm")), Map.entry("centimeter", new UcumCode("cm")),
            Map.entry("mm", new UcumCode("mm")), Map.entry("millimetre", new UcumCode("mm")),
            Map.entry("millimeter", new UcumCode("mm")), Map.entry("g", new UcumCode("g")));

    private final CanonicalRegistryLookup registry;
    private final Map<SourceId, Map<String, UcumCode>> sourceUnitAliases;

    /**
     * Creates a normalizer with explicit source-specific unit aliases.
     *
     * @param registry versioned canonical registry lookup
     * @param sourceUnitAliases aliases keyed by source then exact provider spelling
     */
    public SharedNormalizationService(
            CanonicalRegistryLookup registry, Map<SourceId, Map<String, UcumCode>> sourceUnitAliases) {
        this.registry = Objects.requireNonNull(registry, "registry must not be null");
        this.sourceUnitAliases = sourceUnitAliases.entrySet().stream().collect(java.util.stream.Collectors.toUnmodifiableMap(
                Map.Entry::getKey, entry -> Map.copyOf(entry.getValue())));
    }

    @Override
    public NormalizationResult normalize(NormalizationRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        return normalizeQuantity(request.sourceId(), request.assertion(), request.sourceLocale(),
                request.providerLanguage(), request.mappingEffectiveOn());
    }

    /**
     * Normalizes a scalar quantity using the provider's declared locale.
     *
     * @param sourceId source of the assertion
     * @param assertion raw, immutable source assertion
     * @param providerLocale declared source locale for number parsing
     * @param providerLanguage optional provider language tag
     * @param effectiveOn registry mapping date
     * @return typed success or auditable failure
     */
    public NormalizationResult normalizeQuantity(
            SourceId sourceId, SourceAssertion assertion, Locale providerLocale, String providerLanguage,
            LocalDate effectiveOn) {
        Objects.requireNonNull(sourceId, "sourceId must not be null");
        Objects.requireNonNull(assertion, "assertion must not be null");
        Objects.requireNonNull(providerLocale, "providerLocale must not be null");
        Objects.requireNonNull(effectiveOn, "effectiveOn must not be null");
        LanguageNormalization language = LanguageNormalization.fromProvider(providerLanguage);
        if (!(assertion.evidence() instanceof ScalarEvidence scalar)
                || assertion.contentType() != SourceContentType.ATTRIBUTE) {
            return failure(assertion, language.language(), NormalizationStatus.UNSUPPORTED_EVIDENCE,
                    "quantity normalization requires scalar attribute evidence");
        }
        var mapping = registry.findReviewedMapping(sourceId.value(), assertion.field().key(), effectiveOn);
        if (mapping.isEmpty() || !(mapping.orElseThrow().conceptId() instanceof CanonicalAttributeId attributeId)) {
            return failure(assertion, language.language(), NormalizationStatus.NO_MAPPING, "no reviewed attribute mapping");
        }
        CanonicalAttributeDefinition attribute = registry.findAttribute(attributeId).orElseThrow();
        if (attribute.valueType() != CanonicalValueType.QUANTITY) {
            return failure(assertion, language.language(), NormalizationStatus.INCOMPATIBLE_DIMENSION,
                    "mapped attribute is not a quantity");
        }
        BigDecimal amount = parseDecimal(scalar.lexicalValue(), providerLocale);
        if (amount == null) {
            return failure(assertion, language.language(), NormalizationStatus.INVALID_NUMBER,
                    "invalid or ambiguous localized number: " + scalar.lexicalValue());
        }
        UcumCode sourceUnit = resolveUnit(sourceId, scalar.lexicalUnit());
        if (sourceUnit == null) {
            return failure(assertion, language.language(), NormalizationStatus.UNKNOWN_UNIT,
                    "unknown source unit: " + scalar.lexicalUnit());
        }
        BigDecimal converted = convertLength(amount, sourceUnit, attribute.canonicalUnit(), attribute.dimension());
        if (converted == null) {
            return failure(assertion, language.language(), NormalizationStatus.INCOMPATIBLE_DIMENSION,
                    "unit " + sourceUnit + " is incompatible with " + attribute.dimension());
        }
        if ((attribute.constraints().minimum() != null && converted.compareTo(attribute.constraints().minimum()) < 0)
                || (attribute.constraints().maximum() != null && converted.compareTo(attribute.constraints().maximum()) > 0)) {
            return failure(assertion, language.language(), NormalizationStatus.OUT_OF_RANGE, "value outside registry bounds");
        }
        NormalizedValue value = new NormalizedValue(assertion.assertionId(), attribute.id(),
                new QuantityValue(converted, attribute.dimension(), attribute.canonicalUnit()), RULE);
        return new NormalizationResult(assertion.assertionId(), registry.version(), language.language(),
                NormalizationStatus.SUCCESS, language.diagnostic(), value);
    }

    private NormalizationResult failure(SourceAssertion assertion, LanguageTag language, NormalizationStatus status,
            String diagnostic) {
        return new NormalizationResult(assertion.assertionId(), registry.version(), language, status, diagnostic, null);
    }

    private UcumCode resolveUnit(SourceId sourceId, String lexicalUnit) {
        if (lexicalUnit == null) {
            return null;
        }
        UcumCode sourceAlias = sourceUnitAliases.getOrDefault(sourceId, Map.of()).get(lexicalUnit);
        return sourceAlias != null ? sourceAlias : COMMON_UNITS.get(lexicalUnit.toLowerCase(Locale.ROOT));
    }

    private static BigDecimal parseDecimal(String raw, Locale locale) {
        DecimalFormat parser = (DecimalFormat) NumberFormat.getNumberInstance(ULocale.forLocale(locale));
        parser.setParseBigDecimal(true);
        ParsePosition position = new ParsePosition(0);
        Number parsed = parser.parse(raw, position);
        if (parsed == null || position.getIndex() != raw.length()) {
            return null;
        }
        try {
            return new BigDecimal(parsed.toString());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static BigDecimal convertLength(BigDecimal amount, UcumCode from, UcumCode to, String dimension) {
        if (!"LENGTH".equals(dimension)) {
            return null;
        }
        BigDecimal fromMetres = switch (from.value()) {
            case "m" -> BigDecimal.ONE;
            case "cm" -> new BigDecimal("0.01");
            case "mm" -> new BigDecimal("0.001");
            default -> null;
        };
        BigDecimal toMetres = switch (to.value()) {
            case "m" -> BigDecimal.ONE;
            case "cm" -> new BigDecimal("0.01");
            case "mm" -> new BigDecimal("0.001");
            default -> null;
        };
        return fromMetres == null || toMetres == null ? null : amount.multiply(fromMetres).divide(toMetres);
    }
}
