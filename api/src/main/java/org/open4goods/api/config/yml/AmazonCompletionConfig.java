//package org.open4goods.api.config.yml;
//
///**
// * Configuration for Amazon completion service.
// */
//public record AmazonCompletionConfig(
//        String accessKey,
//        String secretKey,
//        String partnerTag,
//        String host,
//        String region,
//        Long sleepDuration,
//        int maxCallsPerBatch,
//        String datasourceName) {
//
//    /**
//     * Creates a config with default values.
//     */
//    public AmazonCompletionConfig() {
//        this(null, null, null, null, null, 1100L, 8640, "amazon.fr.yml");
//    }
//
//    /**
//     * Canonical constructor applying defaults when values are missing.
//     */
//    public AmazonCompletionConfig {
//        sleepDuration = sleepDuration == null ? 1100L : sleepDuration;
//        maxCallsPerBatch = maxCallsPerBatch == 0 ? 8640 : maxCallsPerBatch;
//        datasourceName = datasourceName == null ? "amazon.fr.yml" : datasourceName;
//    }
//
//    // Compatibility accessors -------------------------------------------------
//    public String getAccessKey() { return accessKey; }
//    public String getSecretKey() { return secretKey; }
//    public String getPartnerTag() { return partnerTag; }
//    public String getHost() { return host; }
//    public String getRegion() { return region; }
//    public Long getSleepDuration() { return sleepDuration; }
//    public int getMaxCallsPerBatch() { return maxCallsPerBatch; }
//    public String getDatasourceName() { return datasourceName; }
//}
