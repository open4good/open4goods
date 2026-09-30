package org.open4goods.datareference.model.evidence;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * What a source actually said, before any O4G interpretation.
 *
 * <p>Evidence is deliberately not typed as a canonical value. It preserves the
 * provider's own lexical spelling, unit and language so that a corrected
 * normalization rule can be replayed against the original words without
 * fetching the provider again. Normalization produces
 * {@link org.open4goods.datareference.model.value.CanonicalValue}; this is its
 * input, never its output.
 *
 * <p>The permitted set is closed: a new provider shape is a change to this
 * contract and to every store that persists it, not a free-form extension.
 */
// As.PROPERTY rather than EXISTING_PROPERTY: kind() is an accessor, not a record
// component, so Jackson never sees it as a property. Letting Jackson own the
// discriminator keeps it present in every document and consumed on read.
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ScalarEvidence.class, name = "SCALAR"),
        @JsonSubTypes.Type(value = LocalizedTextEvidence.class, name = "LOCALIZED_TEXT"),
        @JsonSubTypes.Type(value = MediaEvidence.class, name = "MEDIA"),
        @JsonSubTypes.Type(value = ClassificationEvidence.class, name = "CLASSIFICATION"),
        @JsonSubTypes.Type(value = RelationEvidence.class, name = "RELATION")})
public sealed interface SourceEvidence permits ScalarEvidence, LocalizedTextEvidence, MediaEvidence,
        ClassificationEvidence, RelationEvidence {

    /**
     * Returns the frozen discriminator of this evidence shape.
     *
     * @return evidence kind
     */
    SourceEvidenceKind kind();

    /**
     * Returns the language the provider stated for this evidence.
     *
     * @return BCP 47 tag, or {@code und} for non-linguistic evidence
     */
    org.open4goods.datareference.model.LanguageTag language();
}
