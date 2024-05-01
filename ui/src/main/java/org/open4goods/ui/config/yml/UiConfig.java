package org.open4goods.ui.config.yml;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.open4goods.config.BrandConfiguration;
import org.open4goods.config.yml.BlogConfiguration;
import org.open4goods.config.yml.DevModeConfiguration;
import org.open4goods.config.yml.FeedbackConfiguration;
import org.open4goods.config.yml.XwikiConfiguration;
import org.open4goods.config.yml.ui.ApiConfig;
import org.open4goods.config.yml.ui.OpenSearchConfig;
import org.open4goods.config.yml.ui.SiteNaming;
import org.open4goods.config.yml.ui.WebConfig;
import org.open4goods.xwiki.config.XWikiServiceProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.annotation.JsonMerge;

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
	private String rootFolder = File.separator+"opt"+File.separator+"open4goods"+File.separator;

	
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
	 * The custom pages names and associated templates for this vertical
	 */
	@JsonMerge
	private Map<String,String> pages = new HashMap<>();
	
	
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
	private XWikiServiceProperties wikiConfig = new XWikiServiceProperties();


	private BlogConfiguration blogConfig = new BlogConfiguration();
	
	private FeedbackConfiguration feedbackConfig = new FeedbackConfiguration();
	
	/**
	 * The configuration for developpement mode services
	 */
	private DevModeConfiguration devModeConfig = new DevModeConfiguration();
	
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
	 * Configuration for the brand service
	 */

	private BrandConfiguration brandConfig;
	
	/**
	 * The list of reversments (cashback) to ecologgical organisations
	 */
	private List<Reversement> reversements = new ArrayList<>();
	
	
	/**
	 * get the total amount of reversements
	 * @return
	 */
	public Double getTotalReversements() {
		return reversements.stream().mapToDouble(Reversement::getAmount).sum();
	}
	
	/**
	 * 	* get the total amount of reversements organisations
	 * @return
	 */
	public int getDistingReversementsOrganisation() {
		return reversements.stream().map(Reversement::getOrgName).distinct().toArray().length;
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



	public XWikiServiceProperties getWikiConfig() {
		return wikiConfig;
	}


	public void setWikiConfig(XWikiServiceProperties wikiConfig) {
		this.wikiConfig = wikiConfig;
	}


	public String getResourceTemplateFolder() {
		return resourceTemplateFolder;
	}

	public void setResourceTemplateFolder(String resourceTemplateFolder) {
		this.resourceTemplateFolder = resourceTemplateFolder;
	}


	public Map<String, String> getPages() {
		return pages;
	}


	public void setPages(Map<String, String> pages) {
		this.pages = pages;
	}


	public BrandConfiguration getBrandConfig() {
		return brandConfig;
	}


	public void setBrandConfig(BrandConfiguration brandConfig) {
		this.brandConfig = brandConfig;
	}


	public BlogConfiguration getBlogConfig() {
		return blogConfig;
	}


	public void setBlogConfig(BlogConfiguration blogConfig) {
		this.blogConfig = blogConfig;
	}


	public FeedbackConfiguration getFeedbackConfig() {
		return feedbackConfig;
	}


	public void setFeedbackConfig(FeedbackConfiguration feedbackConfig) {
		this.feedbackConfig = feedbackConfig;
	}


	public List<Reversement> getReversements() {
		return reversements;
	}


	public void setReversements(List<Reversement> reversements) {
		this.reversements = reversements;
	}


	public DevModeConfiguration getDevModeConfig() {
		return devModeConfig;
	}


	public void setDevModeConfig(DevModeConfiguration devModeConfig) {
		this.devModeConfig = devModeConfig;
	}






}