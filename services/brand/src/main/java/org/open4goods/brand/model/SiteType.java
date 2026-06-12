package org.open4goods.brand.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Nature of a manufacturing site referenced by a company.
 */
public enum SiteType {

    FACTORY,
    ASSEMBLY,
    HQ;

    @JsonValue
    public String jsonValue() {
        return name().toLowerCase();
    }

    @JsonCreator
    public static SiteType fromValue(String value) {
        if (value == null || value.isBlank()) {
            return FACTORY;
        }
        return SiteType.valueOf(value.trim().toUpperCase());
    }
}
