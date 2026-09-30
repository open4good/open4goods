package org.open4goods.datareference.model.value;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * A normalized value, after O4G has interpreted what a source said.
 *
 * <p>The union is closed and its discriminator is frozen: {@link #type()} is
 * written into every projection document and read back by every consumer, so a
 * renamed constant is a breaking change to stored data, not a refactor. An
 * unrecognized discriminator fails loudly rather than deserializing to null.
 *
 * <p>Distinct from {@link org.open4goods.datareference.model.evidence.SourceEvidence},
 * which holds the provider's own words. Evidence is the input to normalization;
 * this is its output.
 */
// As.PROPERTY rather than EXISTING_PROPERTY: type() is an accessor, not a record
// component, so Jackson never sees it as a property. Letting Jackson own the
// discriminator keeps it present in every document and consumed on read.
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = LocalizedTextValue.class, name = "LOCALIZED_TEXT"),
        @JsonSubTypes.Type(value = BooleanValue.class, name = "BOOLEAN"),
        @JsonSubTypes.Type(value = IntegerValue.class, name = "INTEGER"),
        @JsonSubTypes.Type(value = DecimalValue.class, name = "DECIMAL"),
        @JsonSubTypes.Type(value = QuantityValue.class, name = "QUANTITY"),
        @JsonSubTypes.Type(value = CodeValue.class, name = "CODE"),
        @JsonSubTypes.Type(value = DateValue.class, name = "DATE"),
        @JsonSubTypes.Type(value = UriValue.class, name = "URI")})
public sealed interface CanonicalValue permits BooleanValue, CodeValue, DateValue, DecimalValue,
        IntegerValue, LocalizedTextValue, QuantityValue, UriValue {

    /**
     * Returns the stable discriminator used by stores and public projections.
     *
     * @return canonical value type
     */
    CanonicalValueType type();
}
