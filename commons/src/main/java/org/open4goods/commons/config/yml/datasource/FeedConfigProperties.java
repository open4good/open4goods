package org.open4goods.commons.config.yml.datasource;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.Set;

@ConfigurationProperties(prefix = "feedconfig")
public record FeedConfigProperties(Map<String, FeedProviderProperties> providers) {

    public record FeedProviderProperties(
            String catalogUrl,
            String datasourceKeyAttribute,
            String datasourceUrlAttribute,
            String datasourceFeedNameAttribute,
            String datasourceRegionAttribute,
            String datasourceLanguageAttribute,
            Map<String, String> filterAttributes,
            Set<String> excludeFeedKeyContains,
            CsvDataSourceProperties defaultCsvProperties,
            String cron) {
    }
}
