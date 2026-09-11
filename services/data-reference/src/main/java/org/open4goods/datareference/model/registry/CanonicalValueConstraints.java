package org.open4goods.datareference.model.registry;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Validation bounds declared by a canonical attribute.
 *
 * @param minimum inclusive numeric lower bound, or {@code null} when unbounded
 * @param maximum inclusive numeric upper bound, or {@code null} when unbounded
 * @param allowedCodes allowed code values, empty when the attribute is not an enumeration
 */
public record CanonicalValueConstraints(
        BigDecimal minimum,
        BigDecimal maximum,
        List<String> allowedCodes) {

    /**
     * Validates ordered numeric bounds and a compact, duplicate-free code list.
     */
    public CanonicalValueConstraints {
        if (minimum != null && maximum != null && minimum.compareTo(maximum) > 0) {
            throw new IllegalArgumentException("constraint minimum must not exceed maximum");
        }
        allowedCodes = List.copyOf(Objects.requireNonNull(allowedCodes, "allowedCodes must not be null"));
        if (allowedCodes.stream().anyMatch(code -> code == null || code.isBlank())) {
            throw new IllegalArgumentException("allowed codes must not be blank");
        }
        if (allowedCodes.stream().distinct().count() != allowedCodes.size()) {
            throw new IllegalArgumentException("allowed codes must not contain duplicates");
        }
    }
}
