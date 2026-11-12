
package org.open4goods.api.config.yml;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.apache.commons.lang3.ArrayUtils;
import org.open4goods.services.feedservice.config.AffiliationConfig;
import org.open4goods.commons.config.yml.DevModeConfiguration;
import org.open4goods.icecat.config.yml.IcecatCompletionConfig;
import org.open4goods.icecat.config.yml.IcecatConfiguration;
import org.open4goods.services.wikidataservice.config.WikidataServiceProperties;
import org.open4goods.model.vertical.DescriptionsAggregationConfig;
import org.open4goods.services.productrepository.config.IndexationConfig;
import org.open4goods.services.prompt.config.PromptServiceConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.validation.annotation.Validated;

import ch.qos.logback.classic.Level;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Configuration
@ConfigurationProperties
@Validated
/**
 * The global API application properties
 *
 * @author goulven
 *
 */
public class ApiProperties {

	@Autowired
	Environment env;

	/**
	 * The location where dedicated snapshots will be stored
	 */
	@NotBlank
	private String rootFolder = "/opt/open4goods/";

	/**
	 * Folder where datasources definitions are stored
	 */
	private String datasourcesfolder = rootFolder + File.separator + "config" + File.separator + "datasources"
			+ File.separator;

	/**
	 * The folder where cached resource will be stored
	 */
	private String datafragmentsBackupFolder = rootFolder + "backup" + File.separator + "data" + File.separator;

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
	private Integer elasticSearchPort = 9200;

	/**
	 * The list of test api keys to authorize.
	 */
	private List<String> testKeys = new ArrayList<>();

	/**
	 * The admin API key to authorize.
	 */
	@NotBlank
	private String adminKey;

	/**
	 * The dedicated key of the Metriks workflow (GOU-1412): it opens {@code /api/metriks/**} only. Absent or blank,
	 * no Metriks key is accepted.
	 */
	private String metriksKey;

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
	 * The CSV datafragments older than this age will be deleted on cleanupAndBackup
	 * pass
	 */
	private Long cleanupCsvDatafragmentsOldness = 1000 * 3600L * 24 * 5;

	/**
	 * The behaviour of descriptions aggregation for relation datas
	 */
	private DescriptionsAggregationConfig relationDatadescriptionsAggregationConfig = new DescriptionsAggregationConfig();

	/**
	 * If true and if the TextualisationService is involved, indicates if must
	 * operates spellchecking and nlp categorization (and so TagClouds, based on
	 * categorization)
	 */
	private boolean operatesNlpProcessing = true;

	/**
	 * If true and if the operatesNlpProcessing is true, indicates wether or not to
	 * detect the language of texts (heavy cpu usage). If false, will "trust" the
	 * datasource provided language, and will compute only if not provided
	 */
	private boolean operatesLanguageDetection = false;

	private AffiliationConfig affiliationConfig = new AffiliationConfig();

	/**
	 * The configuration for developpement mode services
	 */
	private DevModeConfiguration devModeConfig = new DevModeConfiguration();

	/** Map of language to Google taxonomy URL */
	private Map<String, String> googleTaxonomy = new HashMap<>();

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
	 *
	 */
	private IcecatConfiguration icecatFeatureConfig = new IcecatConfiguration();

	/**
	 * Configuration for the Wikidata completion service.
	 */
	private WikidataServiceProperties wikidataConfig = new WikidataServiceProperties();

	/**
	 * The list of hosts allowed for CORS
	 */
	private List<String> corsAllowedHosts = new ArrayList<>();

	/**
	 * Configuration for indexation (number of threads, batch size, ...)
	 */
	private IndexationConfig indexationConfig = new IndexationConfig();

	/**
	 * Configuration for gen ai
	 */
	private PromptServiceConfig genAiConfig = new PromptServiceConfig();

	/**
	 * Duration of the pause to apply beetween 2 subsequent GenAI generation
	 */
	private long genAiPauseDurationMs = 0L;

	/**
	 * Config for verticals generation service
	 *
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
	 * Indicates if the application is in dev mode
	 *
	 * @return
	 */
	public boolean isDevMode() {
		return ArrayUtils.contains(env.getActiveProfiles(), "dev")
				|| ArrayUtils.contains(env.getActiveProfiles(), "devsec");

	}

	public String workFolder() {
		return rootFolder + "/.work/";
	}

	public String dataFragmentsQueueFolderLocation() {
		return workFolder() + "filequeue/";
	}

	public String logsFolder() {
		return rootFolder + File.separator + "logs" + File.separator;
	}

	public String remoteCachingFolder() {
		return rootFolder + File.separator + ".cached" + File.separator;
	}

	public String remoteCachingDeletionFolder() {
		return rootFolder + File.separator + ".cached-deletion" + File.separator;
	}

	/**
	 * Image width suffixes allowed for resizing, used to build cache keys during cleanup.
	 */
	private List<Integer> allowedImagesSizeSuffixes = List.of(30, 50, 100, 360, 1000);

	/**
	 * Safety grace period in milliseconds. Cached files modified within this window
	 * will not be processed during cleanup.
	 */
	private long resourceCleanupGracePeriodMs = 86400000L;

	/**
	 * Safety floor for the orphan resource cleanup. The batch aborts without touching
	 * a single file if the number of products actually streamed from Elasticsearch falls
	 * below this fraction of the index count taken before the stream started.
	 * <p>
	 * This guards against a silently truncated or empty stream (index rebuild, alias
	 * pointing at a half-built index, PIT expiry) being interpreted as "no product owns
	 * any resource", which would otherwise evict the whole cache.
	 */
	@DecimalMin("0.5")
	@DecimalMax("1.0")
	private double resourceCleanupMinProductRatio = 0.9d;

	/**
	 * Thresholds of the automatic ImpactScore checks run after each vertical batch (GOU-1895).
	 */
	private ImpactScoreGuardProperties impactScoreGuard = new ImpactScoreGuardProperties();

	/**
	 * Clearing of the scores held by products outside the batched verticals, at the end of the batch (GOU-2133).
	 */
	private ScoreCleanupProperties scoreCleanup = new ScoreCleanupProperties();

	/**
	 * Retention, in milliseconds, of files parked in the deletion folder by the
	 * {@link org.open4goods.api.services.ResourceCleanupMode#MOVE} mode. Files older than
	 * this are purged for real, which is what actually reclaims disk space.
	 */
	private long resourceDeletionFolderRetentionMs = 604800000L;

	/**
	 * JSON file where the datasource purge (GOU-1815) writes its status at each progress step and at the end, and
	 * which it reads back at startup. Defaults to {@code <rootFolder>/batch-reports/datasource-purge-status.json}.
	 */
	private String datasourcePurgeStatusFile;

	/**
	 * JSON file where the api keeps the date of the last offer of each datasource (GOU-2037), served on
	 * {@code GET /merchants/offer-activity}. Defaults to {@code <rootFolder>/batch-reports/datasource-offer-activity.json}.
	 */
	private String datasourceOfferActivityFile;

	/**
	 * File present while a full batch runs (GOU-2106), read by the beta deployment to postpone the api restart until
	 * the end of the batch. Absent or blank, no marker is written.
	 */
	private String batchRunningMarker;

	/**
	 * The datasource purge logs and persists its progress every this many scanned products (and at least every
	 * {@code DatasourcePurgeService.PROGRESS_MAX_DELAY}).
	 */
	@Min(1)
	private long datasourcePurgeProgressInterval = 50_000;

	/**
	 * Concurrency level for product completion tasks.
	 */
	private int completionConcurrency = 10;

	/**
	 * Number of days before a product associated with an EPREL record is searched again.
	 * A product completed within this window is skipped to avoid redundant searches.
	 */
	@Min(1)
	private int eprelRefreshDays = 1;

	/**
	 * Number of days before a product that got no EPREL association is searched again, as long as its GTIN and
	 * model candidates did not change (a change triggers a new search at the next batch). Such a search runs every
	 * model strategy, so repeating it every day made the batch last hours (GOU-2099).
	 */
	@Min(1)
	private int eprelMissRefreshDays = 7;

	public List<Integer> getAllowedImagesSizeSuffixes() {
		return allowedImagesSizeSuffixes;
	}

	public void setAllowedImagesSizeSuffixes(List<Integer> allowedImagesSizeSuffixes) {
		this.allowedImagesSizeSuffixes = allowedImagesSizeSuffixes;
	}

	public long getResourceCleanupGracePeriodMs() {
		return resourceCleanupGracePeriodMs;
	}

	public void setResourceCleanupGracePeriodMs(long resourceCleanupGracePeriodMs) {
		this.resourceCleanupGracePeriodMs = resourceCleanupGracePeriodMs;
	}

	public ImpactScoreGuardProperties getImpactScoreGuard() {
		return impactScoreGuard;
	}

	public void setImpactScoreGuard(ImpactScoreGuardProperties impactScoreGuard) {
		this.impactScoreGuard = impactScoreGuard;
	}

	public ScoreCleanupProperties getScoreCleanup() {
		return scoreCleanup;
	}

	public void setScoreCleanup(ScoreCleanupProperties scoreCleanup) {
		this.scoreCleanup = scoreCleanup;
	}

	public double getResourceCleanupMinProductRatio() {
		return resourceCleanupMinProductRatio;
	}

	public void setResourceCleanupMinProductRatio(double resourceCleanupMinProductRatio) {
		this.resourceCleanupMinProductRatio = resourceCleanupMinProductRatio;
	}

	public long getResourceDeletionFolderRetentionMs() {
		return resourceDeletionFolderRetentionMs;
	}

	public void setResourceDeletionFolderRetentionMs(long resourceDeletionFolderRetentionMs) {
		this.resourceDeletionFolderRetentionMs = resourceDeletionFolderRetentionMs;
	}

	/**
	 * @return the configured datasource purge status file, or {@code <rootFolder>/batch-reports/datasource-purge-status.json}
	 */
	public String getDatasourcePurgeStatusFile() {
		if (datasourcePurgeStatusFile == null || datasourcePurgeStatusFile.isBlank()) {
			return rootFolder + File.separator + "batch-reports" + File.separator + "datasource-purge-status.json";
		}
		return datasourcePurgeStatusFile;
	}

	public void setDatasourcePurgeStatusFile(String datasourcePurgeStatusFile) {
		this.datasourcePurgeStatusFile = datasourcePurgeStatusFile;
	}

	/**
	 * @return the configured datasource offer activity file, or {@code <rootFolder>/batch-reports/datasource-offer-activity.json}
	 */
	public String getDatasourceOfferActivityFile() {
		if (datasourceOfferActivityFile == null || datasourceOfferActivityFile.isBlank()) {
			return rootFolder + File.separator + "batch-reports" + File.separator + "datasource-offer-activity.json";
		}
		return datasourceOfferActivityFile;
	}

	public void setDatasourceOfferActivityFile(String datasourceOfferActivityFile) {
		this.datasourceOfferActivityFile = datasourceOfferActivityFile;
	}

	public String getBatchRunningMarker() {
		return batchRunningMarker;
	}

	public void setBatchRunningMarker(String batchRunningMarker) {
		this.batchRunningMarker = batchRunningMarker;
	}

	public long getDatasourcePurgeProgressInterval() {
		return datasourcePurgeProgressInterval;
	}

	public void setDatasourcePurgeProgressInterval(long datasourcePurgeProgressInterval) {
		this.datasourcePurgeProgressInterval = datasourcePurgeProgressInterval;
	}

	public int getCompletionConcurrency() {
		return completionConcurrency;
	}

	public void setCompletionConcurrency(int completionConcurrency) {
		this.completionConcurrency = completionConcurrency;
	}

	public int getEprelRefreshDays() {
		return eprelRefreshDays;
	}

	public void setEprelRefreshDays(int eprelRefreshDays) {
		this.eprelRefreshDays = eprelRefreshDays;
	}

	public int getEprelMissRefreshDays() {
		return eprelMissRefreshDays;
	}

	public void setEprelMissRefreshDays(int eprelMissRefreshDays) {
		this.eprelMissRefreshDays = eprelMissRefreshDays;
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

	public String getMetriksKey() {
		return metriksKey;
	}

	public void setMetriksKey(final String metriksKey) {
		this.metriksKey = metriksKey;
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

	public Map<String, String> getGoogleTaxonomy() {
		return googleTaxonomy;
	}

	public void setGoogleTaxonomy(Map<String, String> googleTaxonomy) {
		this.googleTaxonomy = googleTaxonomy;
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

	public WikidataServiceProperties getWikidataConfig() {
		return wikidataConfig;
	}

	public void setWikidataConfig(WikidataServiceProperties wikidataConfig) {
		this.wikidataConfig = wikidataConfig;
	}

	public String getGeneratedImagesFolder() {
		return generatedImagesFolder;
	}

	public void setGeneratedImagesFolder(String generatedImagesFolder) {
		this.generatedImagesFolder = generatedImagesFolder;
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

	public PromptServiceConfig getGenAiConfig() {
		return genAiConfig;
	}

	public void setGenAiConfig(PromptServiceConfig genAiConfig) {
		this.genAiConfig = genAiConfig;
	}

	public AffiliationConfig getAffiliationConfig() {
		return affiliationConfig;
	}

	public void setAffiliationConfig(AffiliationConfig affiliationConfig) {
		this.affiliationConfig = affiliationConfig;
	}

}
