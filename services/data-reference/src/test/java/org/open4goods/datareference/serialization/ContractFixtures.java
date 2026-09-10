package org.open4goods.datareference.serialization;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.open4goods.datareference.model.CanonicalAttributeId;
import org.open4goods.datareference.model.Gtin;
import org.open4goods.datareference.model.GtinMatchConfidence;
import org.open4goods.datareference.model.GtinMatchMethod;
import org.open4goods.datareference.model.GtinLink;
import org.open4goods.datareference.model.LanguageTag;
import org.open4goods.datareference.model.PayloadHash;
import org.open4goods.datareference.model.ProjectionSurface;
import org.open4goods.datareference.model.RuleVersion;
import org.open4goods.datareference.model.SourceAssertion;
import org.open4goods.datareference.model.SourceContentType;
import org.open4goods.datareference.model.SourceFieldId;
import org.open4goods.datareference.model.SourceRecordCompleteness;
import org.open4goods.datareference.model.SourceRecordHead;
import org.open4goods.datareference.model.SourceRecordKey;
import org.open4goods.datareference.model.SourceRecordState;
import org.open4goods.datareference.model.SourceUsagePolicyRef;
import org.open4goods.datareference.model.evidence.ClassificationEvidence;
import org.open4goods.datareference.model.evidence.LocalizedTextEvidence;
import org.open4goods.datareference.model.evidence.MediaEvidence;
import org.open4goods.datareference.model.evidence.RelationEvidence;
import org.open4goods.datareference.model.evidence.ScalarEvidence;
import org.open4goods.datareference.model.projection.DomainSlice;
import org.open4goods.datareference.model.projection.ProductReferenceProjection;
import org.open4goods.datareference.model.registry.RegistryVersion;
import org.open4goods.datareference.model.resolution.ResolutionReason;
import org.open4goods.datareference.model.resolution.ResolvedValue;
import org.open4goods.datareference.model.value.BooleanValue;
import org.open4goods.datareference.model.value.CodeValue;
import org.open4goods.datareference.model.value.DateValue;
import org.open4goods.datareference.model.value.DecimalValue;
import org.open4goods.datareference.model.value.IntegerValue;
import org.open4goods.datareference.model.value.LocalizedTextValue;
import org.open4goods.datareference.model.value.QuantityValue;
import org.open4goods.datareference.model.value.UriValue;

/**
 * Instances covering every discriminator of the public contract.
 *
 * <p>Shared by the round-trip and golden-JSON tests so that a new subtype added
 * to a sealed hierarchy without a fixture shows up as a gap in both at once.
 */
final class ContractFixtures {

    static final SourceRecordKey KEY = SourceRecordKey.of("icecat", "REC-42");
    static final SourceFieldId FIELD = new SourceFieldId("icecat", "1234", "v2");

    private ContractFixtures() {
    }

    /** @return a head carrying one assertion of every evidence kind */
    static SourceRecordHead head() {
        return new SourceRecordHead(
                KEY,
                "1",
                "icecat-2026-01",
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-02T00:00:00Z"),
                Instant.parse("2026-07-01T00:00:00Z"),
                SourceRecordCompleteness.FULL,
                SourceRecordState.ACTIVE,
                new PayloadHash("SHA-256", "9f86d081884c7d659a2feaa0c55ad015"),
                URI.create("urn:o4g:evidence:icecat/REC-42"),
                new SourceUsagePolicyRef("icecat-standard", "3"),
                List.of(
                        new GtinLink(new Gtin("4006381333931"), GtinMatchConfidence.EXACT,
                                GtinMatchMethod.DECLARED_IDENTIFIER, URI.create("urn:o4g:evidence:gtin")),
                        new GtinLink(new Gtin("5901234123457"), GtinMatchConfidence.WEAK,
                                GtinMatchMethod.TEXT_EVIDENCE, null)),
                List.of(
                        SourceAssertion.of(KEY, FIELD, 0, SourceContentType.IDENTITY,
                                new ScalarEvidence("Samsung", null, LanguageTag.UND)),
                        SourceAssertion.of(KEY, FIELD, 1, SourceContentType.ATTRIBUTE,
                                new ScalarEvidence("55", "cm", LanguageTag.UND)),
                        SourceAssertion.of(KEY, FIELD, 2, SourceContentType.TEXT,
                                new LocalizedTextEvidence("Téléviseur 4K", new LanguageTag("fr"))),
                        SourceAssertion.of(KEY, FIELD, 3, SourceContentType.MEDIA,
                                new MediaEvidence(URI.create("https://example.invalid/a.jpg"),
                                        "image/jpeg", "gallery-1", LanguageTag.UND)),
                        SourceAssertion.of(KEY, FIELD, 4, SourceContentType.CLASSIFICATION,
                                new ClassificationEvidence("icecat-category", "1234", "Televisions",
                                        new LanguageTag("en"))),
                        SourceAssertion.of(KEY, FIELD, 5, SourceContentType.RELATION,
                                new RelationEvidence("variant-of", "icecat-record", "REC-41",
                                        LanguageTag.UND))));
    }

    /** @return a projection carrying one resolved value of every canonical value type */
    static ProductReferenceProjection projection() {
        return new ProductReferenceProjection(
                new Gtin("4006381333931"),
                ProjectionSurface.NUDGER_WEB,
                new RegistryVersion(7),
                Instant.parse("2026-02-01T00:00:00Z"),
                List.of(
                        resolved("name", new LocalizedTextValue("Téléviseur 4K", new LanguageTag("fr"))),
                        resolved("energy-star", new BooleanValue(true)),
                        resolved("port-count", new IntegerValue(BigInteger.valueOf(4))),
                        resolved("rating", new DecimalValue(new BigDecimal("4.50"))),
                        resolved("width", QuantityValue.of(new BigDecimal("0.55"), "LENGTH", "m")),
                        resolved("energy-class", new CodeValue("eu-energy-label", "A")),
                        resolved("release-date", new DateValue(LocalDate.of(2026, 3, 15))),
                        resolved("official-page", new UriValue(URI.create("https://example.invalid/p")))),
                slices());
    }

    /**
     * Two slices in a stated order, so that the frozen document pins map ordering
     * rather than accidentally passing on a single entry.
     *
     * @return slices keyed by name, in insertion order
     */
    private static Map<String, DomainSlice> slices() {
        Map<String, DomainSlice> slices = new LinkedHashMap<>();
        slices.put("offers", new DomainSlice("offers",
                Map.of("minimumPrice", new DecimalValue(new BigDecimal("499.00")))));
        Map<String, org.open4goods.datareference.model.value.CanonicalValue> scores = new LinkedHashMap<>();
        scores.put("impact", new DecimalValue(new BigDecimal("3.20")));
        scores.put("repairability", new DecimalValue(new BigDecimal("7.10")));
        slices.put("scores", new DomainSlice("scores", scores));
        return slices;
    }

    private static ResolvedValue resolved(String slug,
            org.open4goods.datareference.model.value.CanonicalValue value) {
        var assertionId = org.open4goods.datareference.model.AssertionId.of(KEY, FIELD, 0);
        return new ResolvedValue(
                new CanonicalAttributeId(slug),
                value,
                assertionId,
                List.of(assertionId),
                new RuleVersion("reference-resolution", 2),
                ResolutionReason.SOURCE_AUTHORITY,
                false);
    }
}
