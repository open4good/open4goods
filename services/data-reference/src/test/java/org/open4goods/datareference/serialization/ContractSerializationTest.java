package org.open4goods.datareference.serialization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.open4goods.datareference.model.SourceAssertion;
import org.open4goods.datareference.model.SourceRecordHead;
import org.open4goods.datareference.model.evidence.SourceEvidenceKind;
import org.open4goods.datareference.model.projection.ProductReferenceProjection;
import org.open4goods.datareference.model.resolution.ResolvedValue;
import org.open4goods.datareference.model.value.CanonicalValueType;
import org.open4goods.datareference.model.value.DecimalValue;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * The serialized shape of the public contract is frozen.
 *
 * <p>These documents are stored data, not a wire format between two versions of
 * the same deployment. A renamed discriminator, a changed id encoding or a
 * reordered collection makes every document already written unreadable or, worse,
 * silently misread, so each is pinned by a golden document rather than by a
 * round-trip alone: a round-trip passes just as happily against a shape both
 * sides got wrong.
 */
class ContractSerializationTest {

    private final ObjectMapper mapper = DataReferenceJson.mapper();

    @Test
    void headMatchesItsGoldenDocument() {
        String actual = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(ContractFixtures.head());

        assertThat(actual.strip()).isEqualTo(golden("source-record-head.json").strip());
    }

    @Test
    void projectionMatchesItsGoldenDocument() {
        String actual = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(ContractFixtures.projection());

        assertThat(actual.strip()).isEqualTo(golden("product-reference-projection.json").strip());
    }

    @Test
    void aStoredHeadIsReadBackUnchanged() {
        SourceRecordHead read = mapper.readValue(golden("source-record-head.json"), SourceRecordHead.class);

        assertThat(read).isEqualTo(ContractFixtures.head());
    }

    @Test
    void aStoredProjectionIsReadBackUnchanged() {
        ProductReferenceProjection read =
                mapper.readValue(golden("product-reference-projection.json"), ProductReferenceProjection.class);

        assertThat(read).isEqualTo(ContractFixtures.projection());
    }

    @Test
    void everyEvidenceKindIsCoveredByTheFrozenDocument() {
        Set<SourceEvidenceKind> covered = ContractFixtures.head().assertions().stream()
                .map(assertion -> assertion.evidence().kind())
                .collect(Collectors.toSet());

        assertThat(covered).containsExactlyInAnyOrderElementsOf(Set.of(SourceEvidenceKind.values()));
    }

    @Test
    void everyCanonicalValueTypeIsCoveredByTheFrozenDocument() {
        Set<CanonicalValueType> covered = ContractFixtures.projection().resolvedValues().stream()
                .map(resolved -> resolved.value().type())
                .collect(Collectors.toSet());

        assertThat(covered).containsExactlyInAnyOrderElementsOf(Set.of(CanonicalValueType.values()));
    }

    @Test
    void assertionOrderIsPreserved() {
        SourceRecordHead read = mapper.readValue(golden("source-record-head.json"), SourceRecordHead.class);

        assertThat(read.assertions()).extracting(SourceAssertion::ordinal).containsExactly(0, 1, 2, 3, 4, 5);
    }

    @Test
    void candidateOrderIsPreserved() {
        ProductReferenceProjection read =
                mapper.readValue(golden("product-reference-projection.json"), ProductReferenceProjection.class);

        assertThat(read.resolvedValues()).extracting(resolved -> resolved.attribute().slug())
                .containsExactly("name", "energy-star", "port-count", "rating", "width", "energy-class",
                        "release-date", "official-page");
    }

    @Test
    void gtinLinkOrderIsPreserved() {
        SourceRecordHead read = mapper.readValue(golden("source-record-head.json"), SourceRecordHead.class);

        // Order is the resolver's tie-break between links of equal confidence.
        assertThat(read.gtinLinks()).extracting(link -> link.gtin().value())
                .containsExactly("4006381333931", "5901234123457");
    }

    @Test
    void sliceOrderIsPreserved() {
        ProductReferenceProjection read =
                mapper.readValue(golden("product-reference-projection.json"), ProductReferenceProjection.class);

        assertThat(read.slices().keySet()).containsExactly("offers", "scores");
        assertThat(read.slices().get("scores").fields().keySet())
                .containsExactly("impact", "repairability");
    }

    @Test
    void anAssertionIdBelongingToAnotherRecordFails() {
        String document = golden("source-record-head.json")
                .replace("\"icecat/REC-42#icecat:1234:v2#0\"", "\"eprel/OTHER#icecat:1234:v2#0\"");

        assertThatThrownBy(() -> mapper.readValue(document, SourceRecordHead.class))
                .isInstanceOf(JacksonException.class);
    }

    @Test
    void decimalScaleIsPreserved() {
        // 4.50 and 4.5 are the same number and a different assertion about precision.
        DecimalValue read = (DecimalValue) mapper.readValue(golden("product-reference-projection.json"),
                ProductReferenceProjection.class).resolvedValues().stream()
                .map(ResolvedValue::value)
                .filter(DecimalValue.class::isInstance)
                .findFirst()
                .orElseThrow();

        assertThat(read.value()).isEqualByComparingTo(new BigDecimal("4.5"));
        assertThat(read.value().scale()).isEqualTo(2);
    }

    @Test
    void anUnrecognizedEvidenceDiscriminatorFails() {
        String document = golden("source-record-head.json").replace("\"kind\" : \"SCALAR\"", "\"kind\" : \"BLOB\"");

        assertThatThrownBy(() -> mapper.readValue(document, SourceRecordHead.class))
                .isInstanceOf(JacksonException.class)
                .hasMessageContaining("BLOB");
    }

    @Test
    void anUnrecognizedCanonicalValueDiscriminatorFails() {
        String document = golden("product-reference-projection.json")
                .replace("\"type\" : \"BOOLEAN\"", "\"type\" : \"TRISTATE\"");

        assertThatThrownBy(() -> mapper.readValue(document, ProductReferenceProjection.class))
                .isInstanceOf(JacksonException.class)
                .hasMessageContaining("TRISTATE");
    }

    @Test
    void anUnknownPropertyFails() {
        String document = golden("source-record-head.json")
                .replace("\"schemaVersion\" : \"1\",", "\"schemaVersion\" : \"1\",\n  \"newField\" : \"x\",");

        assertThatThrownBy(() -> mapper.readValue(document, SourceRecordHead.class))
                .isInstanceOf(JacksonException.class)
                .hasMessageContaining("newField");
    }

    @Test
    void aMalformedGtinFails() {
        String document = golden("source-record-head.json").replace("\"4006381333931\"", "\"40063813\"X\"");

        assertThatThrownBy(() -> mapper.readValue(document, SourceRecordHead.class))
                .isInstanceOf(JacksonException.class);
    }

    @Test
    void aGtinOfTheWrongLengthFails() {
        String document = golden("source-record-head.json").replace("\"4006381333931\"", "\"400638133\"");

        assertThatThrownBy(() -> mapper.readValue(document, SourceRecordHead.class))
                .isInstanceOf(JacksonException.class);
    }

    @Test
    void aMalformedCanonicalAttributeIdFails() {
        String document = golden("product-reference-projection.json")
                .replace("\"o4g:attribute:name\"", "\"o4g:class:name\"");

        assertThatThrownBy(() -> mapper.readValue(document, ProductReferenceProjection.class))
                .isInstanceOf(JacksonException.class);
    }

    @Test
    void aMalformedRuleVersionFails() {
        String document = golden("product-reference-projection.json")
                .replace("\"reference-resolution@2\"", "\"reference-resolution\"");

        assertThatThrownBy(() -> mapper.readValue(document, ProductReferenceProjection.class))
                .isInstanceOf(JacksonException.class);
    }

    @Test
    void anInvalidChronologyFails() {
        String document = golden("source-record-head.json")
                .replace("\"retrievedAt\" : \"2026-01-02T00:00:00Z\"", "\"retrievedAt\" : \"2025-01-02T00:00:00Z\"");

        assertThatThrownBy(() -> mapper.readValue(document, SourceRecordHead.class))
                .isInstanceOf(JacksonException.class);
    }

    @Test
    void aDuplicateAssertionCoordinateFails() {
        String document = golden("source-record-head.json").replace("\"ordinal\" : 1,", "\"ordinal\" : 0,");

        assertThatThrownBy(() -> mapper.readValue(document, SourceRecordHead.class))
                .isInstanceOf(JacksonException.class);
    }

    /**
     * Reads one frozen document from the test classpath.
     *
     * @param name file name under {@code golden/}
     * @return document content
     */
    private static String golden(String name) {
        try (InputStream stream = ContractSerializationTest.class.getResourceAsStream("/golden/" + name)) {
            if (stream == null) {
                throw new IllegalStateException("missing golden document: " + name);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("cannot read golden document: " + name, exception);
        }
    }
}
