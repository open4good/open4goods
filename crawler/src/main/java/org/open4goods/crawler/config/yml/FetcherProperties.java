
package org.open4goods.crawler.config.yml;

import org.open4goods.config.yml.datasource.CrawlProperties;
import org.open4goods.model.constants.UrlConstants;
import org.open4goods.model.crawlers.ApiSynchConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

/**
 * Config of the fetcher (ex crawler) components
 * @author Goulven.Furet
 *
 */
@Configuration
@ConfigurationProperties
@Validated
public class FetcherProperties {

	protected static final Logger logger = LoggerFactory.getLogger(FetcherProperties.class);

	/**
	 * The URL of Indexation API
	 */
	@NotBlank
	private String masterEndpoint = "http://localhost:8081";

	/**
	 * The key to authenticate with against master
	 */
	@NotBlank
	private String apiKey;


	/**
	 * the configuration for synchronisation
	 */
	private ApiSynchConfig apiSynchConfig = new ApiSynchConfig();

	
	
	//TODO : gof(p2,0.5,portability) : the 3 bellow props, like api or ui,  should be dependant on root folder
	/**
	 * The general logs folder
	 */
	private String logsDir = "/opt/open4goods/logs/";


	/**
	 * The folder where crawlers dedicated logs are stored
	 */
	private String crawlerLogDir = "/opt/open4goods/logs/crawler";


	/**
	 * The folder where crawlers dedicated datas are stored
	 */
	private String crawlerStorage = "/opt/open4goods/.work/crawlersData";



	/** The max number of running data fetcher **/
	private Integer concurrentFetcherTask=3;

	/**
	 * The default crawl4j configuration that will be applied to the crawlers
	 */
	private CrawlProperties defaultCrawlConfig = new CrawlProperties();

	/** if true, then the url that match crawl pattern will be logged **/
	private boolean logAccepted = true;
	/** if true, then the url that do not match the crawl pattern will be logged **/

	private boolean logRejected = false;
	
	/**
	 * The user agent to be used when Selenium mode
	 */
	private String seleniumUseragent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.116 Safari/537.36";

	/**
	 * 
	 */
	private String chromeDriverPath="/home/goulven/Bureau/chromedriver";

	public String indexationEndpoint() {
		return masterEndpoint+UrlConstants.API_INDEXATION_ENDPOINT;
	}

	/////////////////////////////////////////
	// Getters / setters
	/////////////////////////////////////////


	public CrawlProperties getDefaultCrawlConfig() {
		return defaultCrawlConfig;
	}

	public void setDefaultCrawlConfig(final CrawlProperties defaultCrawlConfig) {
		this.defaultCrawlConfig = defaultCrawlConfig;
	}



	public String getCrawlerLogDir() {
		return crawlerLogDir;
	}

	public void setCrawlerLogDir(final String crawlerLogDir) {
		this.crawlerLogDir = crawlerLogDir;
	}


	public ApiSynchConfig getApiSynchConfig() {
		return apiSynchConfig;
	}

	public void setApiSynchConfig(final ApiSynchConfig apiSynchConfig) {
		this.apiSynchConfig = apiSynchConfig;
	}

	public String getMasterEndpoint() {
		return masterEndpoint;
	}

	public void setMasterEndpoint(final String masterEndpoint) {
		this.masterEndpoint = masterEndpoint;
	}

	public String getCrawlerStorage() {
		return crawlerStorage;
	}

	public void setCrawlerStorage(final String crawlerStorage) {
		this.crawlerStorage = crawlerStorage;
	}


	public boolean isLogAccepted() {
		return logAccepted;
	}

	public void setLogAccepted(final boolean logAccepted) {
		this.logAccepted = logAccepted;
	}

	public boolean isLogRejected() {
		return logRejected;
	}

	public void setLogRejected(final boolean logRejected) {
		this.logRejected = logRejected;
	}

	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(final String apiKey) {
		this.apiKey = apiKey;
	}

	public Integer getConcurrentFetcherTask() {
		return concurrentFetcherTask;
	}

	public void setConcurrentFetcherTask(final Integer concurrentIndexationTask) {
		concurrentFetcherTask = concurrentIndexationTask;
	}

	public String getLogsDir() {
		return logsDir;
	}

	public void setLogsDir(final String logsDir) {
		this.logsDir = logsDir;
	}

	public String getSeleniumUseragent() {
		return seleniumUseragent;
	}

	public void setSeleniumUseragent(String seleniumUseragent) {
		this.seleniumUseragent = seleniumUseragent;
	}

	public String getChromeDriverPath() {
		return chromeDriverPath;
	}

	public void setChromeDriverPath(String chromeDriverPath) {
		this.chromeDriverPath = chromeDriverPath;
	}






}