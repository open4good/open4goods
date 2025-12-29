package org.open4goods.icecat.config.yml;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "icecat-feature-config")
public class IcecatConfiguration {

	// XML file URIs (Basic Auth)
	private String featuresListFileUri;
	private String categoryFeatureListFileUri;
	private String languageListFileUri;
	private String brandsListFileUri;
	private String categoriesListFileUri;
	private String featureGroupsFileUri;

	// Basic Auth credentials for XML file downloads
	private String user;
	private String password;

	// Retailer API configuration (OAuth 2.0)
	private String retailerApiBaseUrl = "https://retailer-api.icecat.biz";
	private String tokenEndpoint = "/cdm-cedemo-authenticationservice/connect/token";
	private String clientId;
	private String clientSecret;
	private String oauthUsername;
	private String oauthPassword;
	private String organizationId;

	// HTTP client configuration
	private int connectTimeoutMs = 10000;
	private int readTimeoutMs = 30000;
	private int maxRetries = 3;
	private int rateLimitRequestsPerSecond = 5;

	public String getFeaturesListFileUri() {
		return featuresListFileUri;
	}

	public void setFeaturesListFileUri(String featuresListFileUri) {
		this.featuresListFileUri = featuresListFileUri;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getLanguageListFileUri() {
		return languageListFileUri;
	}

	public void setLanguageListFileUri(String languagesListFileUri) {
		this.languageListFileUri = languagesListFileUri;
	}

	public String getCategoryFeatureListFileUri() {
		return categoryFeatureListFileUri;
	}

	public void setCategoryFeatureListFileUri(String categoryFeatureListFileUri) {
		this.categoryFeatureListFileUri = categoryFeatureListFileUri;
	}

	public String getBrandsListFileUri() {
		return brandsListFileUri;
	}

	public void setBrandsListFileUri(String brandsListFileUri) {
		this.brandsListFileUri = brandsListFileUri;
	}

	public String getCategoriesListFileUri() {
		return categoriesListFileUri;
	}

	public void setCategoriesListFileUri(String categoriesListFileUri) {
		this.categoriesListFileUri = categoriesListFileUri;
	}

	public String getFeatureGroupsFileUri() {
		return featureGroupsFileUri;
	}

	public void setFeatureGroupsFileUri(String featureGroupsFileUri) {
		this.featureGroupsFileUri = featureGroupsFileUri;
	}

	public String getRetailerApiBaseUrl() {
		return retailerApiBaseUrl;
	}

	public void setRetailerApiBaseUrl(String retailerApiBaseUrl) {
		this.retailerApiBaseUrl = retailerApiBaseUrl;
	}

	public String getTokenEndpoint() {
		return tokenEndpoint;
	}

	public void setTokenEndpoint(String tokenEndpoint) {
		this.tokenEndpoint = tokenEndpoint;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getClientSecret() {
		return clientSecret;
	}

	public void setClientSecret(String clientSecret) {
		this.clientSecret = clientSecret;
	}

	public String getOauthUsername() {
		return oauthUsername;
	}

	public void setOauthUsername(String oauthUsername) {
		this.oauthUsername = oauthUsername;
	}

	public String getOauthPassword() {
		return oauthPassword;
	}

	public void setOauthPassword(String oauthPassword) {
		this.oauthPassword = oauthPassword;
	}

	public String getOrganizationId() {
		return organizationId;
	}

	public void setOrganizationId(String organizationId) {
		this.organizationId = organizationId;
	}

	public int getConnectTimeoutMs() {
		return connectTimeoutMs;
	}

	public void setConnectTimeoutMs(int connectTimeoutMs) {
		this.connectTimeoutMs = connectTimeoutMs;
	}

	public int getReadTimeoutMs() {
		return readTimeoutMs;
	}

	public void setReadTimeoutMs(int readTimeoutMs) {
		this.readTimeoutMs = readTimeoutMs;
	}

	public int getMaxRetries() {
		return maxRetries;
	}

	public void setMaxRetries(int maxRetries) {
		this.maxRetries = maxRetries;
	}

	public int getRateLimitRequestsPerSecond() {
		return rateLimitRequestsPerSecond;
	}

	public void setRateLimitRequestsPerSecond(int rateLimitRequestsPerSecond) {
		this.rateLimitRequestsPerSecond = rateLimitRequestsPerSecond;
	}
}
