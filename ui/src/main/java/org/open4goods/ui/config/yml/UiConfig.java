package org.open4goods.ui.config.yml;

import java.io.File;
import java.util.Locale;

import org.open4goods.config.yml.TagCloudConfig;
import org.open4goods.config.yml.XwikiConfiguration;
import org.open4goods.config.yml.ui.ApiConfig;
import org.open4goods.config.yml.ui.OpenSearchConfig;
import org.open4goods.config.yml.ui.SiteNaming;
import org.open4goods.config.yml.ui.WebConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties
/**
 * Le parametrage Yaml de la capsule. Celui ci dispose soit de propriétés de
 * premier niveau, soit de sous-objets de configuration.
 * 
 * @author goulven
 *
 */

public class UiConfig {

	private static final Logger LOGGER = LoggerFactory.getLogger(UiConfig.class);

	/**
	 * The local folder where capsule data will be stored.
	 */
	private String rootFolder = "/opt/open4goods/";

	/**
	 * Folder where verticals configurations are stored
	 */
	private String verticalsFolder=rootFolder+"/config/verticals/";
	
	/**
	 * Folder where datasources definitions are stored
	 */
	private String datasourcesfolder=rootFolder+"/config/datasources/";
	
	/**
	 * The URL namings
	 */
	
	private SiteNaming namings;
	
	private OpenSearchConfig openSearchConfig = new OpenSearchConfig();

	private ApiConfig apiConfig = new ApiConfig();

	/**
	 * Elastic search host
	 */
	private String elasticSearchHost = "localhost";

	/**
	 * Elastic search port
	 */
	private Integer elasticSearchPort = 9200;

	/**
	 * The email where to send emails
	 */
	private String email;
	
	
//	/**
//	 * Relativ to the web ( trackings id, ....)
//	 */
//
	private WebConfig webConfig = new WebConfig();


	/**
	 * The Xwiki instance configuration
	 */
	private XwikiConfiguration wikiConfig;

	/**
	 * The config for generated tagcloud
	 */
	private TagCloudConfig tagCloudConfig = new TagCloudConfig();
	
	
	/**
	 * Return the root url for a given sitelocale, with the "default" behavior
	 *
	 * @param siteLocale
	 * @return
	 */
	public String getBaseUrl(final Locale siteLocale) {
		return namings.getBaseUrls().getOrDefault(siteLocale.getLanguage(), namings.getBaseUrls().get("default"));
	}

	public String getBaseUrl(final String siteLocale) {
		return namings.getBaseUrls().getOrDefault(siteLocale, namings.getBaseUrls().get("default"));
	}

	public String getRemoteCachingFolder() {
		return rootFolder + "/.cached/";
	}

	public String logsFolder() {
		return rootFolder + "/logs/";
	}
	

	public File siteMapFolder() {
		return new File(rootFolder + "/sitemap/");
	}
	
	public File openDataFile() {
		return new File(rootFolder + "/opendata/full.zip");
	}

	public File tmpOpenDataFile() {
		return new File(rootFolder + "/opendata/full-tmp.zip");
	}
	
	
	public String getRootFolder() {
		return rootFolder;
	}

	public void setRootFolder(String rootFolder) {
		this.rootFolder = rootFolder;
	}

	public OpenSearchConfig getOpenSearchConfig() {
		return openSearchConfig;
	}

	public void setOpenSearchConfig(OpenSearchConfig openSearchConfig) {
		this.openSearchConfig = openSearchConfig;
	}

	public ApiConfig getApiConfig() {
		return apiConfig;
	}

	public void setApiConfig(ApiConfig apiConfig) {
		this.apiConfig = apiConfig;
	}

	public WebConfig getWebConfig() {
		return webConfig;
	}

	public void setWebConfig(WebConfig webConfig) {
		this.webConfig = webConfig;
	}

	public String getElasticSearchHost() {
		return elasticSearchHost;
	}

	public void setElasticSearchHost(String elasticSearchHost) {
		this.elasticSearchHost = elasticSearchHost;
	}

	public Integer getElasticSearchPort() {
		return elasticSearchPort;
	}

	public void setElasticSearchPort(Integer elasticSearchPort) {
		this.elasticSearchPort = elasticSearchPort;
	}

	public SiteNaming getNamings() {
		return namings;
	}

	public void setNamings(SiteNaming namings) {
		this.namings = namings;
	}

	public TagCloudConfig getTagCloudConfig() {
		return tagCloudConfig;
	}

	public void setTagCloudConfig(TagCloudConfig tagCloudConfig) {
		this.tagCloudConfig = tagCloudConfig;
	}

	public String getVerticalsFolder() {
		return verticalsFolder;
	}

	public void setVerticalsFolder(String verticalsFolder) {
		this.verticalsFolder = verticalsFolder;
	}

	public String getDatasourcesfolder() {
		return datasourcesfolder;
	}

	public void setDatasourcesfolder(String datasourcesfolder) {
		this.datasourcesfolder = datasourcesfolder;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public XwikiConfiguration getWikiConfig() {
		return wikiConfig;
	}

	public void setWikiConfig(XwikiConfiguration wikiConfig) {
		this.wikiConfig = wikiConfig;
	}
	
	
	
	

}