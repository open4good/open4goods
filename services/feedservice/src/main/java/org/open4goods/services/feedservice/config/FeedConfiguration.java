package org.open4goods.services.feedservice.config;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.open4goods.commons.config.yml.datasource.CsvDataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "feed")
public class FeedConfiguration {

    private String catalogUrl;
    private String datasourceKeyAttribute;
    private String datasourceUrlAttribute;
    private String datasourceFeedNameAttribute;
    private String datasourceRegionAttribute;
    private String datasourceLanguageAttribute;
    private Map<String, String> filterAttributes = new HashMap<>();
    private Set<String> excludeFeedKeyContains = new HashSet<>();
    
    private CsvDataSourceProperties defaultCsvProperties;

    private AwinConfig awin = new AwinConfig();
    private EffiliationConfig effiliation = new EffiliationConfig();

    public static class AwinConfig {
        private String cron = "0 43 1 * * ?";
        private boolean enabled = true;
        private int cacheTtlDays = 1;

        public String getCron() { return cron; }
        public void setCron(String cron) { this.cron = cron; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public int getCacheTtlDays() { return cacheTtlDays; }
        public void setCacheTtlDays(int cacheTtlDays) { this.cacheTtlDays = cacheTtlDays; }
    }

    public static class EffiliationConfig {
        private String cron = "30 43 1 * * ?";
        private boolean enabled = true;
        private int cacheTtlDays = 1;

        public String getCron() { return cron; }
        public void setCron(String cron) { this.cron = cron; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public int getCacheTtlDays() { return cacheTtlDays; }
        public void setCacheTtlDays(int cacheTtlDays) { this.cacheTtlDays = cacheTtlDays; }
    }
    
	public String getCatalogUrl() {
		return catalogUrl;
	}
	public void setCatalogUrl(String catalogUrl) {
		this.catalogUrl = catalogUrl;
	}
	public String getDatasourceKeyAttribute() {
		return datasourceKeyAttribute;
	}
	public void setDatasourceKeyAttribute(String datasourceKeyAttribute) {
		this.datasourceKeyAttribute = datasourceKeyAttribute;
	}
	public String getDatasourceFeedNameAttribute() {
		return datasourceFeedNameAttribute;
	}
	public void setDatasourceFeedNameAttribute(String datasourceFeedNameAttribute) {
		this.datasourceFeedNameAttribute = datasourceFeedNameAttribute;
	}
	public String getDatasourceRegionAttribute() {
		return datasourceRegionAttribute;
	}
	public void setDatasourceRegionAttribute(String datasourceRegionAttribute) {
		this.datasourceRegionAttribute = datasourceRegionAttribute;
	}
	public String getDatasourceLanguageAttribute() {
		return datasourceLanguageAttribute;
	}
	public void setDatasourceLanguageAttribute(String datasourceLanguageAttribute) {
		this.datasourceLanguageAttribute = datasourceLanguageAttribute;
	}

	public Map<String, String> getFilterAttributes() {
		return filterAttributes;
	}
	public void setFilterAttributes(Map<String, String> filterAttributes) {
		this.filterAttributes = filterAttributes;
	}
	public String getDatasourceUrlAttribute() {
		return datasourceUrlAttribute;
	}
	public void setDatasourceUrlAttribute(String datasourceUrlAttribute) {
		this.datasourceUrlAttribute = datasourceUrlAttribute;
	}
	public CsvDataSourceProperties getDefaultCsvProperties() {
		return defaultCsvProperties;
	}
	public void setDefaultCsvProperties(CsvDataSourceProperties defaultCsvProperties) {
		this.defaultCsvProperties = defaultCsvProperties;
	}
	public Set<String> getExcludeFeedKeyContains() {
		return excludeFeedKeyContains;
	}
	public void setExcludeFeedKeyContains(Set<String> excludeFeedKeyContains) {
		this.excludeFeedKeyContains = excludeFeedKeyContains;
	}

    public AwinConfig getAwin() {
        return awin;
    }

    public void setAwin(AwinConfig awin) {
        this.awin = awin;
    }

    public EffiliationConfig getEffiliation() {
        return effiliation;
    }

    public void setEffiliation(EffiliationConfig effiliation) {
        this.effiliation = effiliation;
    }
}
