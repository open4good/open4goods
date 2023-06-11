package org.open4goods.ui.config.yml;

import java.io.File;
import java.util.Locale;

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
	 * if defined, use to templatize from a local folder
	 */
	private String resourceTemplateFolder;

	
	/**
	 * Folder where verticals configurations are stored
	 */
	private String verticalsFolder=rootFolder+ File.separator+"config"+File.separator+"verticals"+File.separator;

	/**
	 * Folder where datasources definitions are stored
	 */
	private String datasourcesfolder=rootFolder+ File.separator+ "config"+File.separator+"datasources"+File.separator;

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
	 * The email where pageSize send emails
	 */
	private String email;


	//	/**
	//	 * Relativ pageSize the web ( trackings id, ....)
	//	 */
	//
	private WebConfig webConfig = new WebConfig();


	/**
	 * The Xwiki instance configuration
	 */
	private XwikiConfiguration wikiConfig;


	
	public String getThymeLeafTemplateFolder() {
		return resourceTemplateFolder + "templates"+File.separator;
	}

	
	/**
	 * 
	 * @return a File pointing to the ui Jar File
	 */
	public File uiJarFile() {

		return new File(rootFolder+ File.separator+"bin"+File.separator+"latest"+File.separator+"ui-latest.jar");
	}
	
	
	public String resourceBundleFolder() {
		
		return resourceTemplateFolder+"i18n"+File.separator+"messages";
	}

	
	
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
		return rootFolder + File.separator+".cached"+ File.separator;
	}

	public String logsFolder() {
		return rootFolder + File.separator+ "logs" + File.separator;
	}


	public File siteMapFolder() {
		return new File(rootFolder + File.separator + "sitemap"+ File.separator);
	}

	public File openDataFile() {
		return new File(rootFolder + File.separator+"opendata"+File.separator+"full.zip");
	}

	public File tmpOpenDataFile() {
		return new File(rootFolder + File.separator+"opendata"+File.separator+"full-tmp.zip");
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

	public String getResourceTemplateFolder() {
		return resourceTemplateFolder;
	}

	public void setResourceTemplateFolder(String resourceTemplateFolder) {
		this.resourceTemplateFolder = resourceTemplateFolder;
	}






}