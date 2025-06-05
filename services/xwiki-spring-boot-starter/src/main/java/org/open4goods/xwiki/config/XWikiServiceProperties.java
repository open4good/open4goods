package org.open4goods.xwiki.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotNull;

/**
 * Properties from configuration file
 * 
 * @author Thierry.Ledan
 */

@ConfigurationProperties(prefix = "xwiki")
@Validated
public class XWikiServiceProperties{
	
	// TODO : Critical : Must be configurable in config
	public final static String SPRING_CACHE_NAME = "ONE_HOUR_LOCAL_CACHE";
	
	@NotNull
	public String baseUrl;

	@NotNull
	public String username;
	
	@NotNull
	public String password;
	
	@NotNull
	public boolean httpsOnly;
	
	@NotNull
	public String media = "json";	
	
	@NotNull
	public String apiEntrypoint = "rest";	
	
	@NotNull
	public String apiWiki = "xwiki";

	
	public String getApiWiki() {
		return apiWiki;
	}

	public void setApiWiki(String apiWiki) {
		this.apiWiki = apiWiki;
	}

	public String getApiEntrypoint() {
		return apiEntrypoint;
	}

	public void setApiEntrypoint(String apiEntrypoint) {
		this.apiEntrypoint = apiEntrypoint;
	}

	public String getMedia() {
		return media;
	}

	public void setMedia(String media) {
		this.media = media;
	}

	public boolean isHttpsOnly() {
		return httpsOnly;
	}

	public void setHttpsOnly(boolean httpsOnly) {
		this.httpsOnly = httpsOnly;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getBaseUrl() {
		return baseUrl;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}
	
	
}

