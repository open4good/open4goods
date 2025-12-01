package org.open4goods.ui.config.yml;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.open4goods.commons.config.yml.BanCheckerConfig;
import org.open4goods.commons.config.yml.DevModeConfiguration;
import org.open4goods.commons.config.yml.ui.OpenSearchConfig;
import org.open4goods.commons.config.yml.ui.WebConfig;
import org.open4goods.model.Localisable;
import org.open4goods.model.priceevents.PriceRestitutionConfig;
import org.open4goods.model.vertical.SiteNaming;
import org.open4goods.services.blog.config.BlogConfiguration;
import org.open4goods.services.feedservice.config.AffiliationConfig;
import org.open4goods.services.feedservice.config.FeedConfiguration;
import org.open4goods.ui.interceptors.ImageResizeInterceptor;
import org.open4goods.xwiki.config.XWikiServiceProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

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

@Validated
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



	/** A localised list of fun facts **/
	private Localisable<String,FunFactsConfig> funFacts = new Localisable<>();

	/**
	 * Folder where datasources definitions are stored
	 */
	private String datasourcesfolder=rootFolder+ File.separator+ "config"+File.separator+"datasources"+File.separator;


	/**
	 * Folder where AI generated images are stored
	 */
	private String generatedImagesFolder = rootFolder + "generated-images" + File.separator;

	/**
	 * The list of authorized dynamic image resizing suffixes
	 */
        private Set<String> allowedImagesSizeSuffixes = new HashSet<>();

        /**
         * Base URL used by the {@link ImageResizeInterceptor} to fetch source images.
         */
        private String imageBaseUrl = "https://static.nudger.fr";

        /** Map of language code to Google taxonomy URL */
        private Map<String, String> googleTaxonomy = new HashMap<>();

	/**
	 * The mapped wiki pages
	 */
	@JsonMerge
	private Map<String,String> pages = new HashMap<>();


	/**
	 * The URL namings
	 */

	private SiteNaming namings;

	private OpenSearchConfig openSearchConfig = new OpenSearchConfig();


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

	/**
	 * For the T O D O service, the taglist.xml url to be scanned
	 */
	private String tagListUrl = "";
    /**
     * Secret and key  for captcha system
     */
	private String captchaKey;
	private String captchaSecret;

	/**
	 * The google API, for indexing services
	 */
	private String googleApiJson;

	//	/**
	//	 * Relativ pageSize the web ( trackings id, ....)
	//	 */
	//
	private WebConfig webConfig = new WebConfig();


	private PriceRestitutionConfig priceConfig = new PriceRestitutionConfig();

	/**
	 * Config for url checking
	 */
	private UrlCheckConfig urlcheck;


	private AffiliationConfig affiliationConfig = new AffiliationConfig();


	private Map<String, FeedConfiguration> feedConfigs = new HashMap<>();

	/***
	 * Config for IP and UA banChecking
	 */
	private BanCheckerConfig bancheckerConfig = new BanCheckerConfig();

	/**
	 * The Xwiki instance configuration
	 */
	private XWikiServiceProperties wikiConfig = new XWikiServiceProperties();

	/**
	 * The localised mapping of exposed spaces in the wiki. Eg : "/mentions-legales" -> "/", "legalspace" -> "/legals""
	 */
	private Map<String,Localisable<String,String>> wikiPagesMapping = new HashMap<>();


	/**
	 * Containing the project members, for restitution in /team
	 */
	private TeamConfig teamConfig = new TeamConfig();


	private BlogConfiguration blogConfig = new BlogConfiguration();


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


	/**
	 * Configuration for Amazon Links & Tags
	 */
	private AmazonConfig amazonConfig = new AmazonConfig();

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

	public String getGoogleIndexationMarkerFile() {
		return getRemoteCachingFolder() + "google-indexation-timestamp";
	}


	public String logsFolder() {
		return rootFolder + File.separator+ "logs" + File.separator;
	}


	public File siteMapFolder() {
		return new File(rootFolder + File.separator + "sitemap"+ File.separator);
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

	public String getGeneratedImagesFolder() {
		return generatedImagesFolder;
	}

	public void setGeneratedImagesFolder(String generatedImagesFolder) {
		this.generatedImagesFolder = generatedImagesFolder;
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



	public BlogConfiguration getBlogConfig() {
		return blogConfig;
	}


	public void setBlogConfig(BlogConfiguration blogConfig) {
		this.blogConfig = blogConfig;
	}




	public DevModeConfiguration getDevModeConfig() {
		return devModeConfig;
	}


	public void setDevModeConfig(DevModeConfiguration devModeConfig) {
		this.devModeConfig = devModeConfig;
	}



	public String getCaptchaSecret() {
		return captchaSecret;
	}


	public void setCaptchaSecret(String captchaSecret) {
		this.captchaSecret = captchaSecret;
	}


	public String getCaptchaKey() {
		return captchaKey;
	}


	public void setCaptchaKey(String captchaKey) {
		this.captchaKey = captchaKey;
	}





	public Map<String, Localisable<String, String>> getWikiPagesMapping() {
		return wikiPagesMapping;
	}


	public void setWikiPagesMapping(Map<String, Localisable<String, String>> wikiPagesMapping) {
		this.wikiPagesMapping = wikiPagesMapping;
	}


	public String getTagListUrl() {
		return tagListUrl;
	}


	public void setTagListUrl(String tagListUrl) {
		this.tagListUrl = tagListUrl;
	}



	public String getGoogleApiJson() {
		return googleApiJson;
	}


	public void setGoogleApiJson(String googleApiJson) {
		this.googleApiJson = googleApiJson;
	}


	public BanCheckerConfig getBancheckerConfig() {
		return bancheckerConfig;
	}


	public void setBancheckerConfig(BanCheckerConfig bancheckerConfig) {
		this.bancheckerConfig = bancheckerConfig;
	}


	public TeamConfig getTeamConfig() {
		return teamConfig;
	}


	public void setTeamConfig(TeamConfig teamConfig) {
		this.teamConfig = teamConfig;
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


	public UrlCheckConfig getUrlcheck() {
		return urlcheck;
	}


	public void setUrlcheck(UrlCheckConfig urlcheck) {
		this.urlcheck = urlcheck;
	}


	public PriceRestitutionConfig getPriceConfig() {
		return priceConfig;
	}


	public void setPriceConfig(PriceRestitutionConfig priceConfig) {
		this.priceConfig = priceConfig;
	}


	public AffiliationConfig getAffiliationConfig() {
		return affiliationConfig;
	}


	public void setAffiliationConfig(AffiliationConfig affiliationConfig) {
		this.affiliationConfig = affiliationConfig;
	}


	public Map<String, FeedConfiguration> getFeedConfigs() {
		return feedConfigs;
	}


	public void setFeedConfigs(Map<String, FeedConfiguration> feedConfigs) {
		this.feedConfigs = feedConfigs;
	}



	public Localisable<String, FunFactsConfig> getFunFacts() {
		return funFacts;
	}


	public void setFunFacts(Localisable<String, FunFactsConfig> funFacts) {
		this.funFacts = funFacts;
	}


	public AmazonConfig getAmazonConfig() { return amazonConfig; }

	public void setAmazonConfig(AmazonConfig amazon) { this.amazonConfig = amazon; }
}