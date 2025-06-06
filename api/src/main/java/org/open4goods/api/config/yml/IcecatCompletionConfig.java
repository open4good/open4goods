package org.open4goods.api.config.yml;

/**
 * Configuration for Icecat completion service.
 */
public record IcecatCompletionConfig(String iceCatUrlPrefix, Integer politnessDelayMs) {

    /** Default constructor setting sane defaults. */
    public IcecatCompletionConfig() {
        this("https://live.icecat.biz/api?UserName=openIcecat-live&Language=fr&GTIN=", 500);
    }

    /** Canonical constructor applying defaults when null. */
    public IcecatCompletionConfig {
        iceCatUrlPrefix = iceCatUrlPrefix == null
                ? "https://live.icecat.biz/api?UserName=openIcecat-live&Language=fr&GTIN="
                : iceCatUrlPrefix;
        politnessDelayMs = politnessDelayMs == null ? 500 : politnessDelayMs;
    }

    // Compatibility accessors -------------------------------------------------
    public String getIceCatUrlPrefix() { return iceCatUrlPrefix; }
    public Integer getPolitnessDelayMs() { return politnessDelayMs; }
}
