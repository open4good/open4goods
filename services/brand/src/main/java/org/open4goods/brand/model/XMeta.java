package org.open4goods.brand.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Generic, extensible and sourced metadata entry attached to a company
 * (certifications, controversies, facts, news). This is the "x-metas" slot used
 * to accumulate brand intelligence without growing the rigid schema.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class XMeta {

    private String key;
    private XMetaType type = XMetaType.FACT;
    private String value;
    private String url;
    private String retrievedAt;
    private String validUntil;
    private String lang;

    /**
     * @param now reference instant
     * @return true when this entry has no expiry or its expiry is in the future
     */
    public boolean isValid(Instant now) {
        if (validUntil == null || validUntil.isBlank()) {
            return true;
        }
        try {
            Instant expiry = LocalDate.parse(validUntil).atStartOfDay(ZoneOffset.UTC).toInstant();
            return !expiry.isBefore(now);
        } catch (Exception e) {
            return true;
        }
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public XMetaType getType() {
        return type;
    }

    public void setType(XMetaType type) {
        this.type = type == null ? XMetaType.FACT : type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getRetrievedAt() {
        return retrievedAt;
    }

    public void setRetrievedAt(String retrievedAt) {
        this.retrievedAt = retrievedAt;
    }

    public String getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(String validUntil) {
        this.validUntil = validUntil;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }
}
