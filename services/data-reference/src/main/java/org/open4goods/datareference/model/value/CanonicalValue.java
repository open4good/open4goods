package org.open4goods.datareference.model.value;

/**
 * Source-neutral structured value admitted by the O4G product reference.
 */
public sealed interface CanonicalValue permits BooleanValue, CodeValue, DateValue, DecimalValue,
        IntegerValue, LocalizedTextValue, QuantityValue, UriValue {

    /**
     * Returns the stable discriminator used by stores and public projections.
     *
     * @return canonical value type
     */
    CanonicalValueType type();
}
