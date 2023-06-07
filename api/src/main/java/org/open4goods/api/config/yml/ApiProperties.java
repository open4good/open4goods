
package org.open4goods.api.config.yml;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.config.yml.GlobalAttributeAggregationConfig;
import org.open4goods.config.yml.ui.DescriptionsAggregationConfig;
import org.open4goods.crawler.config.yml.FetcherProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

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

	/**
	 * The location where dedicated snapshots will be stored
	 */
	@NotBlank
	private String rootFolder="/opt/open4goods/";

	/**
	 * Folder where verticals configurations are stored
	 */
	private String verticalsFolder=rootFolder+"/config/verticals/";

	/**
	 * Folder where datasources definitions are stored
	 */
	private String datasourcesfolder=rootFolder+"/config/datasources/";

	/**
	 * The folder where cached resource will be stored
	 */
	private String datafragmentsBackupFolder = rootFolder + "backup/data/";

	
	/**
	 * If true, all dedicated loggers will be redirected to console (usefull for dev mode)
	 */
	private boolean dedicatedLoggerToConsole = false;
	
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
	 * The local crawler configuration
	 */
	@NotNull
	private FetcherProperties fetcherProperties;


	/////////////////////////
	// Realtime aggregators configuration
	/////////////////////////
	private GlobalAttributeAggregationConfig attributeAggregationConfig = new GlobalAttributeAggregationConfig();




	/**
	 * Return the datafragments retrieving query against global tags
	 * @param provider
	 * @param inclusions
	 * @param exclusions
	 * @return
	 */
	public String getIncludeExcludeSegmentQuery(final String provider, final List<String> inclusions,
			final List<String> exclusions) {
		StringBuilder ret = new StringBuilder();


		if (StringUtils.isEmpty(provider)) {
			ret.append("datasourceName:*");
		} else {
			ret.append("datasourceName:\"").append(provider).append("\"");
		}


		if (null != inclusions &&  inclusions.size() > 0) {

			ret.append(" AND (").append(StringUtils.join(inclusions.stream()
					.filter(e -> !StringUtils.isEmpty(e))
					.map(e -> "productTags:\"*" + e.trim().toUpperCase() + "*\"")
					.collect(Collectors.toSet()), " OR ")).append(")") ;
		}

		if (null != exclusions && exclusions.size() > 0) {
			ret.append(" AND (").append(StringUtils.join(exclusions.stream()
					.filter(e -> !StringUtils.isEmpty(e))
					.map(e -> "NOT productTags:\"*" + e.trim().toUpperCase() + "*\"")
					.collect(Collectors.toSet()), " AND ")).append(")");
		}
		return ret.toString();
	}



	public String workFolder() {
		return rootFolder+"/.work/";
	}

	public String dataFragmentsQueueFolderLocation() {
		return workFolder()+"filequeue/";
	}

	public String logsFolder() {
		return rootFolder+"/logs/";
	}

	public String remoteCachingFolder() {
		return rootFolder+"/.cached/";
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




	public GlobalAttributeAggregationConfig getAttributeAggregationConfig() {
		return attributeAggregationConfig;
	}



	public void setAttributeAggregationConfig(GlobalAttributeAggregationConfig attributeAggregationConfig) {
		this.attributeAggregationConfig = attributeAggregationConfig;
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



	public boolean isDedicatedLoggerToConsole() {
		return dedicatedLoggerToConsole;
	}



	public void setDedicatedLoggerToConsole(boolean dedicatedLoggerToConsole) {
		this.dedicatedLoggerToConsole = dedicatedLoggerToConsole;
	}






}
