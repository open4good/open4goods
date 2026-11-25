package org.open4goods.ui.config.yml;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.open4goods.commons.config.yml.ui.WebConfig;
import org.open4goods.model.vertical.SiteNaming;
import org.open4goods.ui.interceptors.ImageResizeInterceptor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * Root YAML configuration of the ui service.
 *
 * <p>ui only serves the static resources of static.nudger.fr (cached files and resized images), redirects the open
 * data downloads and writes the sitemaps read by the Nuxt frontend; this class only holds the properties those jobs
 * read.
 */
@Configuration
@ConfigurationProperties
@Validated
public class UiConfig {

	/**
	 * The local folder where ui data is stored.
	 */
	private String rootFolder = File.separator + "opt" + File.separator + "open4goods" + File.separator;

	/**
	 * Folder where datasources definitions are stored.
	 */
	private String datasourcesfolder = rootFolder + File.separator + "config" + File.separator + "datasources"
			+ File.separator;

	/**
	 * The list of authorized dynamic image resizing suffixes.
	 */
	private Set<String> allowedImagesSizeSuffixes = new HashSet<>();

	/**
	 * Base URL used by the {@link ImageResizeInterceptor} to fetch source images.
	 */
	private String imageBaseUrl = "https://static.nudger.fr";

	/** Map of language code to Google taxonomy URL. */
	private Map<String, String> googleTaxonomy = new HashMap<>();

	/**
	 * The URL namings, used to build the sitemaps.
	 */
	private SiteNaming namings;

	/**
	 * Web settings (authentication toggle, CORS hosts).
	 */
	private WebConfig webConfig = new WebConfig();

	public String getRemoteCachingFolder() {
		return rootFolder + File.separator + ".cached" + File.separator;
	}

	public String logsFolder() {
		return rootFolder + File.separator + "logs" + File.separator;
	}

	public File siteMapFolder() {
		return new File(rootFolder + File.separator + "sitemap" + File.separator);
	}

	public String getRootFolder() {
		return rootFolder;
	}

	public void setRootFolder(String rootFolder) {
		this.rootFolder = rootFolder;
	}

	public String getDatasourcesfolder() {
		return datasourcesfolder;
	}

	public void setDatasourcesfolder(String datasourcesfolder) {
		this.datasourcesfolder = datasourcesfolder;
	}

	public Set<String> getAllowedImagesSizeSuffixes() {
		return allowedImagesSizeSuffixes;
	}

	public void setAllowedImagesSizeSuffixes(Set<String> allowedImagesSizeSuffixes) {
		this.allowedImagesSizeSuffixes = allowedImagesSizeSuffixes;
	}

	public String getImageBaseUrl() {
		return imageBaseUrl;
	}

	public void setImageBaseUrl(String imageBaseUrl) {
		this.imageBaseUrl = imageBaseUrl;
	}

	public Map<String, String> getGoogleTaxonomy() {
		return googleTaxonomy;
	}

	public void setGoogleTaxonomy(Map<String, String> googleTaxonomy) {
		this.googleTaxonomy = googleTaxonomy;
	}

	public SiteNaming getNamings() {
		return namings;
	}

	public void setNamings(SiteNaming namings) {
		this.namings = namings;
	}

	public WebConfig getWebConfig() {
		return webConfig;
	}

	public void setWebConfig(WebConfig webConfig) {
		this.webConfig = webConfig;
	}
}
