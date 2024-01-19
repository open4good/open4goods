
package org.open4goods.api.config.yml;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.open4goods.config.BrandConfiguration;
import org.open4goods.config.yml.GithubConfiguration;
import org.open4goods.config.yml.ui.DescriptionsAggregationConfig;
import org.open4goods.crawler.config.yml.FetcherProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.validation.annotation.Validated;

import ch.qos.logback.classic.Level;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Configuration
@ConfigurationProperties
@Validated
/**
 * The global API application properties
 * @author goulven
 *
 */
public class ApiProperties {

	
	@Autowired Environment env;	

	/**
	 * The location where dedicated snapshots will be stored
	 */
	@NotBlank
	private String rootFolder="/opt/open4goods/";

	/**
	 * Folder where verticals configurations are stored
	 */
	private String verticalsFolder=rootFolder+ File.separator+"config"+File.separator+"verticals"+File.separator;

	/**
	 * Folder where datasources definitions are stored
	 */
	private String datasourcesfolder=rootFolder+ File.separator+ "config"+File.separator+"datasources"+File.separator;

	/**
	 * The folder where cached resource will be stored
	 */
	private String datafragmentsBackupFolder = rootFolder + "backup"+File.separator+"data"+File.separator;

		
	/*
	 * Proxy, if neededpsule
	 */
	private String proxyhost;
	private Integer proxyport;
	private String proxyusername;
	private String proxypassword;

	/**
	 * Elastic search host
	 */
	private String elasticSearchHost = "localhost";

	/**
	 * Elastic search port
	 */
	private Integer elasticSearchPort= 9200;

	/**
	 * The list of crawler api keys to authorize
	 */
	private List<String> crawlerKeys = new ArrayList<>();


	/**
	 * The list of crawler api keys to authorize
	 */
	@NotBlank
	private String adminKey;


	/**
	 * The buffer size for AggregatedDatas indexation
	 */
	private Integer aggregatedDataElasticBuffer = 200;

	/**
	 * The datafragments dequeue size
	 */
	private Integer dataFragmentsDequeueSize = 200;

	/**
	 * The datafragments dequeue worker size
	 */
	private Integer dataFragmentsDequeueWorkers = 4;

	/**
	 * The datafragments worker dequeue poll period, in ms
	 */
	private Integer dataFragmentsDequeuePeriodMs = 2000;

	/**
	 * The CSV datafragments older than this age will be deleted on cleanupAndBackup pass
	 */
	private Long cleanupCsvDatafragmentsOldness = 1000 * 3600L * 24 * 5;

	/**
	 * The behaviour of descriptions aggregation for relation datas
	 */
	private DescriptionsAggregationConfig relationDatadescriptionsAggregationConfig  = new DescriptionsAggregationConfig();

	/**
	 *  If true and if the TextualisationService is involved, indicates if must operates spellchecking and nlp categorization (and so TagClouds, based on categorization)
	 */
	private boolean operatesNlpProcessing = true;


	/**
	 *  If true and if the operatesNlpProcessing is true, indicates wether or not to detect the language of texts (heavy cpu usage). If false, will "trust" the datasource provided language, and will compute only if not provided
	 */
	private boolean operatesLanguageDetection = false;


	/**
	 * Configuration for the brand service
	 */

	private BrandConfiguration brandConfig;

	private GithubConfiguration githubConfig;

	/**
	 * The log level for aggregation
     *
	 */
	private String aggregationLogLevel = "INFO";
	
	
	public Level aggLogLevel() {
		return Level.toLevel(aggregationLogLevel);
	}

	
	
	public Environment getEnv() {
		return env;
	}
	public void setEnv(Environment env) {
		this.env = env;
	}
	public String getAggregationLogLevel() {
		return aggregationLogLevel;
	}
	public void setAggregationLogLevel(String aggregationLogLevel) {
		this.aggregationLogLevel = aggregationLogLevel;
	}



	/**
	 * The local crawler configuration
	 */
	@NotNull
	private FetcherProperties fetcherProperties;


	/**
	 * Indicates if the application is in dev mode
	 * @return
	 */
	public boolean isDevMode() {
		return ArrayUtils.contains(env.getActiveProfiles(), "dev") || ArrayUtils.contains(env.getActiveProfiles(), "devsec");
        
	}
	public String workFolder() {
		return rootFolder+"/.work/";
	}

	public String dataFragmentsQueueFolderLocation() {
		return workFolder()+"filequeue/";
	}

	public String logsFolder() {
		return rootFolder+File.separator+"logs"+File.separator;
	}

	public String remoteCachingFolder() {
		return rootFolder+File.separator+".cached"+File.separator;
	}


	public Integer getAggregatedDataElasticBuffer() {
		return aggregatedDataElasticBuffer;
	}

	public void setAggregatedDataElasticBuffer(final Integer dedicatedOfferIndexBuffer) {
		aggregatedDataElasticBuffer = dedicatedOfferIndexBuffer;
	}

	public String getProxyhost() {
		return proxyhost;
	}

	public void setProxyhost(final String proxyHost) {
		proxyhost = proxyHost;
	}

	public Integer getProxyport() {
		return proxyport;
	}

	public void setProxyport(final Integer proxyPort) {
		proxyport = proxyPort;
	}

	public String getProxyusername() {
		return proxyusername;
	}

	public void setProxyusername(final String proxyUsername) {
		proxyusername = proxyUsername;
	}

	public String getProxypassword() {
		return proxypassword;
	}

	public void setProxypassword(final String proxyUserpassword) {
		proxypassword = proxyUserpassword;
	}


	public List<String> getCrawlerKeys() {
		return crawlerKeys;
	}


	public void setCrawlerKeys(final List<String> crawlerKeys) {
		this.crawlerKeys = crawlerKeys;
	}



	public Integer getDataFragmentsDequeueSize() {
		return dataFragmentsDequeueSize;
	}

	public void setDataFragmentsDequeueSize(final Integer dataFragmentsDequeueSize) {
		this.dataFragmentsDequeueSize = dataFragmentsDequeueSize;
	}

	public Integer getDataFragmentsDequeuePeriodMs() {
		return dataFragmentsDequeuePeriodMs;
	}

	public void setDataFragmentsDequeuePeriodMs(final Integer dataFragmentsDequeuePeriodMs) {
		this.dataFragmentsDequeuePeriodMs = dataFragmentsDequeuePeriodMs;
	}




	public FetcherProperties getFetcherProperties() {
		return fetcherProperties;
	}

	public void setFetcherProperties(final FetcherProperties fetcherProperties) {
		this.fetcherProperties = fetcherProperties;
	}

	public DescriptionsAggregationConfig getRelationDatadescriptionsAggregationConfig() {
		return relationDatadescriptionsAggregationConfig;
	}


	public void setRelationDatadescriptionsAggregationConfig(
			final DescriptionsAggregationConfig relationDatadescriptionsAggregationConfig) {
		this.relationDatadescriptionsAggregationConfig = relationDatadescriptionsAggregationConfig;
	}

	public String getAdminKey() {
		return adminKey;
	}


	public void setAdminKey(final String adminKey) {
		this.adminKey = adminKey;
	}



	public String getDatafragmentsBackupFolder() {
		return datafragmentsBackupFolder;
	}

	public void setDatafragmentsBackupFolder(final String datafragmentsBackupFolder) {
		this.datafragmentsBackupFolder = datafragmentsBackupFolder;
	}

	public Long getCleanupCsvDatafragmentsOldness() {
		return cleanupCsvDatafragmentsOldness;
	}

	public void setCleanupCsvDatafragmentsOldness(final Long cleanupCsvDatafragmentsOldness) {
		this.cleanupCsvDatafragmentsOldness = cleanupCsvDatafragmentsOldness;
	}

	public String getRootFolder() {
		return rootFolder;
	}

	public void setRootFolder(final String rootFolder) {
		this.rootFolder = rootFolder;
	}





	public Integer getDataFragmentsDequeueWorkers() {
		return dataFragmentsDequeueWorkers;
	}



	public void setDataFragmentsDequeueWorkers(Integer dataFragmentsDequeueWorker) {
		dataFragmentsDequeueWorkers = dataFragmentsDequeueWorker;
	}



	public boolean isOperatesNlpProcessing() {
		return operatesNlpProcessing;
	}



	public void setOperatesNlpProcessing(boolean operatesNlpProcessing) {
		this.operatesNlpProcessing = operatesNlpProcessing;
	}



	public boolean isOperatesLanguageDetection() {
		return operatesLanguageDetection;
	}



	public void setOperatesLanguageDetection(boolean operatesLanguageDetection) {
		this.operatesLanguageDetection = operatesLanguageDetection;
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






	public BrandConfiguration getBrandConfig() {
		return brandConfig;
	}



	public void setBrandConfig(BrandConfiguration brandConfig) {
		this.brandConfig = brandConfig;
	}



	public GithubConfiguration getGithubConfig() {
		return githubConfig;
	}



	public void setGithubConfig(GithubConfiguration githubConfig) {
		this.githubConfig = githubConfig;
	}






}
