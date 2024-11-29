
package org.open4goods.api.config.yml;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.open4goods.commons.config.BrandsConfiguration;
import org.open4goods.commons.config.yml.DevModeConfiguration;
import org.open4goods.commons.config.yml.GithubConfiguration;
import org.open4goods.commons.config.yml.IcecatConfiguration;
import org.open4goods.commons.config.yml.IndexationConfig;
import org.open4goods.commons.config.yml.ui.DescriptionsAggregationConfig;
import org.open4goods.commons.config.yml.ui.ImageGenerationConfig;
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
	 * Folder where datasources definitions are stored
	 */
	private String datasourcesfolder=rootFolder+ File.separator+ "config"+File.separator+"datasources"+File.separator;

	/**
	 * The folder where cached resource will be stored
	 */
	private String datafragmentsBackupFolder = rootFolder + "backup"+File.separator+"data"+File.separator;

	/**
	 * Folder where AI generated images are stored
	 */
	private String generatedImagesFolder = rootFolder + "generated-images" + File.separator;
	
	/**
	 * Configuration for resource completion
	 */
	private ResourceCompletionConfig resourceCompletionConfig = new ResourceCompletionConfig();
	
	
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
	private List<String> testKeys = new ArrayList<>();


	
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
	 * Config gor IA Generated images
	 */
	private ImageGenerationConfig imageGenerationConfig = new ImageGenerationConfig();
	
	/**
	 * Configuration for the brand service
	 */

	private BrandsConfiguration brandConfig;

	private GithubConfiguration githubConfig;

	/**
	 * The configuration for developpement mode services
	 */
	private DevModeConfiguration devModeConfig = new DevModeConfiguration();
	
	
	/**
	 * The log level for aggregation
     *
	 */
	private String aggregationLogLevel = "INFO";
	
	/**
	 * The configuration for amazon completion
	 */
	private AmazonCompletionConfig amazonConfig = new AmazonCompletionConfig();
	
	
	/**
	 * The configuration for icecat
	 */
	private IcecatCompletionConfig icecatCompletionConfig = new IcecatCompletionConfig();
	
	/**
	 * The configuration for icecat features
     
	 */
	private IcecatConfiguration icecatFeatureConfig = new IcecatConfiguration();
	

	/**
	 * The list of hosts allowed for CORS
	 */
	private List<String> corsAllowedHosts = new ArrayList<>();
	
	
	/**
	 * Configuration for indexation (number of threads, batch size, ...)
	 */
	private IndexationConfig indexationConfig = new IndexationConfig();
	
	
	/**
	 * Duration of the pause to apply beetween 2 subsequent GenAI generation
	 */
	private long genAiPauseDurationMs = 0L;
	
	/**
	 * Options for Wiki backup
	 */
	private BackupConfig backupConfig = new BackupConfig();
	
	
	
	
	/**
	 * Config for verticals generation service
	 * @return
	 */
	
	private VerticalsGenerationConfig verticalsGenerationConfig = new VerticalsGenerationConfig();
	
	
	
	
	
	
	
	
	
	public AmazonCompletionConfig getAmazonConfig() {
		return amazonConfig;
	}



	public void setAmazonConfig(AmazonCompletionConfig amazonConfig) {
		this.amazonConfig = amazonConfig;
	}



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


	public String getDatasourcesfolder() {
		return datasourcesfolder;
	}



	public void setDatasourcesfolder(String datasourcesfolder) {
		this.datasourcesfolder = datasourcesfolder;
	}






	public BrandsConfiguration getBrandConfig() {
		return brandConfig;
	}



	public void setBrandConfig(BrandsConfiguration brandConfig) {
		this.brandConfig = brandConfig;
	}



	public GithubConfiguration getGithubConfig() {
		return githubConfig;
	}



	public void setGithubConfig(GithubConfiguration githubConfig) {
		this.githubConfig = githubConfig;
	}



	public List<String> getTestKeys() {
		return testKeys;
	}



	public void setTestKeys(List<String> testKeys) {
		this.testKeys = testKeys;
	}



	public DevModeConfiguration getDevModeConfig() {
		return devModeConfig;
	}



	public void setDevModeConfig(DevModeConfiguration devModeConfig) {
		this.devModeConfig = devModeConfig;
	}



	public IcecatCompletionConfig getIcecatCompletionConfig() {
		return icecatCompletionConfig;
	}



	public void setIcecatCompletionConfig(IcecatCompletionConfig icecatConfig) {
		this.icecatCompletionConfig = icecatConfig;
	}



	public IcecatConfiguration getIcecatFeatureConfig() {
		return icecatFeatureConfig;
	}



	public void setIcecatFeatureConfig(IcecatConfiguration icecatFeatureConfig) {
		this.icecatFeatureConfig = icecatFeatureConfig;
	}



	public String getGeneratedImagesFolder() {
		return generatedImagesFolder;
	}



	public void setGeneratedImagesFolder(String generatedImagesFolder) {
		this.generatedImagesFolder = generatedImagesFolder;
	}



	public ImageGenerationConfig getImageGenerationConfig() {
		return imageGenerationConfig;
	}



	public void setImageGenerationConfig(ImageGenerationConfig imageGenerationConfig) {
		this.imageGenerationConfig = imageGenerationConfig;
	}



	public ResourceCompletionConfig getResourceCompletionConfig() {
		return resourceCompletionConfig;
	}



	public void setResourceCompletionConfig(ResourceCompletionConfig resourceCompletionConfig) {
		this.resourceCompletionConfig = resourceCompletionConfig;
	}



	public List<String> getCorsAllowedHosts() {
		return corsAllowedHosts;
	}



	public void setCorsAllowedHosts(List<String> corsAllowedHosts) {
		this.corsAllowedHosts = corsAllowedHosts;
	}



	public BackupConfig getBackupConfig() {
		return backupConfig;
	}



	public void setBackupConfig(BackupConfig backupConfig) {
		this.backupConfig = backupConfig;
	}



	public long getGenAiPauseDurationMs() {
		return genAiPauseDurationMs;
	}



	public void setGenAiPauseDurationMs(long genAiPauseDurationMs) {
		this.genAiPauseDurationMs = genAiPauseDurationMs;
	}



	public IndexationConfig getIndexationConfig() {
		return indexationConfig;
	}



	public void setIndexationConfig(IndexationConfig indexationConfig) {
		this.indexationConfig = indexationConfig;
	}



	public VerticalsGenerationConfig getVerticalsGenerationConfig() {
		return verticalsGenerationConfig;
	}



	public void setVerticalsGenerationConfig(VerticalsGenerationConfig verticalsGenerationConfig) {
		this.verticalsGenerationConfig = verticalsGenerationConfig;
	}






}
