package org.open4goods.icecat.config.yml;

/**
 * Configuration for the Icecat live API completion service.
 *
 * <p>The URL prefix must include the username and language parameters.
 * The GTIN is appended at request time. The language parameter should be
 * passed dynamically in production; this default is for backward compatibility.
 */
public class IcecatCompletionConfig {

    /** Default live API URL prefix (Open Icecat free tier, language must be appended dynamically). */
    private String iceCatUrlPrefix = "https://live.icecat.biz/api?UserName=openIcecat-live&Language=fr&GTIN=";

    /** Minimum delay in milliseconds between consecutive Icecat API calls (politeness). */
    private Integer politenessDelayMs = 500;

    public String getIceCatUrlPrefix() {
        return iceCatUrlPrefix;
    }

    public void setIceCatUrlPrefix(String iceCatUrlPrefix) {
        this.iceCatUrlPrefix = iceCatUrlPrefix;
    }

    public Integer getPolitenessDelayMs() {
        return politenessDelayMs;
    }

    public void setPolitenessDelayMs(Integer politenessDelayMs) {
        this.politenessDelayMs = politenessDelayMs;
    }


}
