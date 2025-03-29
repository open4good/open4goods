package org.open4goods.commons.config.yml.datasource;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.Set;

@Configuration
@ConfigurationProperties(prefix = "feedconfig")
public class FeedConfigProperties {

    private Map<String, FeedProviderProperties> providers;

    public Map<String, FeedProviderProperties> getProviders() {
        return providers;
    }

    public void setProviders(Map<String, FeedProviderProperties> providers) {
        this.providers = providers;
    }

    public static class FeedProviderProperties {
        private String catalogUrl;
        private String datasourceKeyAttribute;
        private String datasourceUrlAttribute;
        private String datasourceFeedNameAttribute;
        private String datasourceRegionAttribute;
        private String datasourceLanguageAttribute;
        private Map<String, String> filterAttributes;
        private Set<String> excludeFeedKeyContains;
        private CsvDataSourceProperties defaultCsvProperties;
        private String cron; // Optional custom cron expression

        // Getters and setters
        public String getCatalogUrl() { return catalogUrl; }
        public void setCatalogUrl(String catalogUrl) { this.catalogUrl = catalogUrl; }
        public String getDatasourceKeyAttribute() { return datasourceKeyAttribute; }
        public void setDatasourceKeyAttribute(String datasourceKeyAttribute) { this.datasourceKeyAttribute = datasourceKeyAttribute; }
        public String getDatasourceUrlAttribute() { return datasourceUrlAttribute; }
        public void setDatasourceUrlAttribute(String datasourceUrlAttribute) { this.datasourceUrlAttribute = datasourceUrlAttribute; }
        public String getDatasourceFeedNameAttribute() { return datasourceFeedNameAttribute; }
        public void setDatasourceFeedNameAttribute(String datasourceFeedNameAttribute) { this.datasourceFeedNameAttribute = datasourceFeedNameAttribute; }
        public String getDatasourceRegionAttribute() { return datasourceRegionAttribute; }
        public void setDatasourceRegionAttribute(String datasourceRegionAttribute) { this.datasourceRegionAttribute = datasourceRegionAttribute; }
        public String getDatasourceLanguageAttribute() { return datasourceLanguageAttribute; }
        public void setDatasourceLanguageAttribute(String datasourceLanguageAttribute) { this.datasourceLanguageAttribute = datasourceLanguageAttribute; }
        public Map<String, String> getFilterAttributes() { return filterAttributes; }
        public void setFilterAttributes(Map<String, String> filterAttributes) { this.filterAttributes = filterAttributes; }
        public Set<String> getExcludeFeedKeyContains() { return excludeFeedKeyContains; }
        public void setExcludeFeedKeyContains(Set<String> excludeFeedKeyContains) { this.excludeFeedKeyContains = excludeFeedKeyContains; }
        public CsvDataSourceProperties getDefaultCsvProperties() { return defaultCsvProperties; }
        public void setDefaultCsvProperties(CsvDataSourceProperties defaultCsvProperties) { this.defaultCsvProperties = defaultCsvProperties; }
        public String getCron() { return cron; }
        public void setCron(String cron) { this.cron = cron; }
    }
}
