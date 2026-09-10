package org.open4goods.datareference.model.registry;

import java.util.Objects;

import org.open4goods.datareference.model.CanonicalAttributeId;
import org.open4goods.datareference.model.UcumCode;
import org.open4goods.datareference.model.value.CanonicalValueType;

/**
 * What the O4G registry says one canonical attribute is.
 *
 * <p>Authored in Git and projected to a runtime index; runtime writes never
 * redefine it. Normalization reads the declared type and, for a quantity, the
 * one canonical unit every source must be converted into before values from
 * different providers can be compared at all.
 *
 * @param id stable canonical attribute identifier
 * @param valueType canonical value type every normalized value must have
 * @param cardinality how many values may win
 * @param dimension physical dimension for a quantity, otherwise {@code null}
 * @param canonicalUnit canonical UCUM unit for a quantity, otherwise {@code null}
 */
public record CanonicalAttributeDefinition(
        CanonicalAttributeId id,
        CanonicalValueType valueType,
        CanonicalCardinality cardinality,
        String dimension,
        UcumCode canonicalUnit) {

    /**
     * Validates the definition and the quantity contract it implies.
     */
    public CanonicalAttributeDefinition {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(valueType, "valueType must not be null");
        Objects.requireNonNull(cardinality, "cardinality must not be null");
        boolean quantity = valueType == CanonicalValueType.QUANTITY;
        if (quantity && (dimension == null || dimension.isBlank() || canonicalUnit == null)) {
            throw new IllegalArgumentException(
                    "a quantity attribute must declare one dimension and one canonical UCUM unit: " + id);
        }
        if (!quantity && (dimension != null || canonicalUnit != null)) {
            throw new IllegalArgumentException(
                    "only a quantity attribute may declare a dimension or a unit: " + id);
        }
        if (dimension != null) {
            dimension = dimension.trim();
        }
    }
}
