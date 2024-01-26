package org.open4goods.config.yml.datasource;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

}
