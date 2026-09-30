package org.open4goods.datareference.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;

import org.junit.jupiter.api.Test;

/**
 * A provider record is identified by its source and the id that source issued,
 * and by nothing else.
 */
class SourceRecordKeyTest {

    @Test
    void correctingAGtinAttachmentDoesNotChangeRecordIdentity() {
        SourceRecordKey key = SourceRecordKey.of("icecat", "REC-1");

        GtinLink wrong = new GtinLink(new Gtin("4006381333931"), GtinMatchConfidence.WEAK,
                GtinMatchMethod.TEXT_EVIDENCE, URI.create("urn:o4g:evidence:1"));
        GtinLink corrected = new GtinLink(new Gtin("5901234123457"), GtinMatchConfidence.EXACT,
                GtinMatchMethod.O4G_CORRECTION, URI.create("urn:o4g:evidence:2"));

        // The attachment changed; the record did not become a different record.
        assertThat(wrong.gtin()).isNotEqualTo(corrected.gtin());
        assertThat(SourceRecordKey.of("icecat", "REC-1")).isEqualTo(key);
    }

    @Test
    void twoSourcesMayIssueTheSameRecordId() {
        assertThat(SourceRecordKey.of("icecat", "1234"))
                .isNotEqualTo(SourceRecordKey.of("eprel", "1234"));
    }

    @Test
    void sourceIdIsCaseFoldedButProviderRecordIdIsNot() {
        assertThat(SourceRecordKey.of("ICECAT", "Ref-A").externalForm()).isEqualTo("icecat/Ref-A");
    }

    @Test
    void rejectsBlankComponents() {
        assertThatThrownBy(() -> SourceRecordKey.of("icecat", "  "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SourceRecordKey.of("", "REC-1"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsASourceIdThatIsNotAnIdentifier() {
        assertThatThrownBy(() -> new SourceId("icecat biz"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("source id");
    }
}
