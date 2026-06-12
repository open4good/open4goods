package org.open4goods.brand.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Category of a generic sourced fact ({@link XMeta}) stored on a company for
 * further enrichment and front-end exposure.
 */
public enum XMetaType {

    CERTIFICATION,
    CONTROVERSY,
    FACT,
    NEWS;

    @JsonValue
    public String jsonValue() {
        return name().toLowerCase();
    }

    @JsonCreator
    public static XMetaType fromValue(String value) {
        if (value == null || value.isBlank()) {
            return FACT;
        }
        return XMetaType.valueOf(value.trim().toUpperCase());
    }
}
