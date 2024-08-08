package org.open4goods.config.yml.datasource;

import java.util.HashSet;
import java.util.Set;

import org.open4goods.model.crawlers.BasicHeader;



public class CrawlProperties {

	/**
	 * Number of concurrent threads to use to crawl this datasource
	 */
	private Integer threads = 2;

	/**
	 * Politeness delay in milliseconds (delay between sending two requests to the
	 * same host).
	 */
	private Integer politenessDelay = 50;

	/**
	 * If true, crawling will respect website robots.txt
	 */
	private Boolean robotsTxtCompliance = true;

	/**
	 * If this feature is enabled, you would be able to resume a previously
	 * stopped/crashed crawl. However, it makes crawling slightly slower
	 */
	private Boolean resumableCrawling = false;

	/**
	 * user-agent string that is used for representing your crawler to web servers.
	 * See http://en.wikipedia.org/wiki/User_agent for more details
	 */
	private String userAgentString = "comparatif-tv.fr";


	/**
	 * Should we fetch binary content such as images, audio, ...?
	 */
	private Boolean includeBinaryContentInCrawling = false;

	/**
	 * Should we process binary content such as image, audio, ... using TIKA?
	 */
	private Boolean processBinaryContentInCrawling = false;

	/**
	 * Maximum Connections per host
	 */
	private Integer maxConnectionsPerHost = 100;

	/**
	 * Maximum total connections
	 */
	private Integer maxTotalConnections = 100;

	/**
	 * Socket timeout in milliseconds
	 */
	private Integer socketTimeout = 20000;

	/**
	 * Connection timeout in milliseconds
	 */
	private Integer connectionTimeout = 30000;

	/**
	 * Max number of outgoing links which are processed from a page
	 */
	private Integer maxOutgoingLinksToFollow = 5000;

	/**
	 * Max allowed size of a page. Pages larger than this size will not be fetched.
	 */
	private Integer maxDownloadSize = 1048576;

	/**
	 * Should we follow redirects?
	 */
	private Boolean followRedirects = true;

	/**
	 * Should the TLD list be updated automatically on each run? Alternatively, it
	 * can be loaded from the embedded tld-names.zip file that was obtained from
	 * https://publicsuffix.org/list/effective_tld_names.dat
	 */
	private Boolean onlineTldListUpdate = false;

	/**
	 * Should the crawler stop running when the queue is empty?
	 */
	private Boolean shutdownOnEmptyQueue = true;

	/**
	 * Wait this long before checking the status of the worker threads.
	 */
	private Integer threadMonitoringDelaySeconds = 10;

	/**
	 * Wait this long to verify the craweler threads are finished working.
	 */
	private Integer threadShutdownDelaySeconds = 10;

	/**
	 * Wait this long in seconds before launching cleanup.
	 */
	private Integer cleanupDelaySeconds = 10;

	/**
	 * If crawler should run behind a proxy, this parameter can be used for
	 * specifying the proxy host.
	 */
	private String proxyHost = null;

	/**
	 * If crawler should run behind a proxy, this parameter can be used for
	 * specifying the proxy port.
	 */
	private Integer proxyPort = 80;

	/**
	 * If crawler should run behind a proxy and user/pass is needed for
	 * authentication in proxy, this parameter can be used for specifying the
	 * username.
	 */
	private String proxyUsername = null;

	/**
	 * If crawler should run behind a proxy and user/pass is needed for
	 * authentication in proxy, this parameter can be used for specifying the
	 * password.
	 */
	private String proxyPassword = null;

	/**
	 * Whether to honor "nofollow" flag
	 */
	private Boolean respectNoFollow = true;



	/**
	 * The encoding to use for selenium fetched content
	 */
	private String seleniumPageEncoding = "ISO-8859-1";


	/**
	 * Additional headers to be sent
	 */
	private Set<BasicHeader> defaultHeaders = new HashSet<>();

	public Boolean getRobotsTxtCompliance() {
		return robotsTxtCompliance;
	}

	public void setRobotsTxtCompliance(final Boolean robotsTxtCompliance) {
		this.robotsTxtCompliance = robotsTxtCompliance;
	}

	public Boolean getResumableCrawling() {
		return resumableCrawling;
	}

	public void setResumableCrawling(final Boolean resumableCrawling) {
		this.resumableCrawling = resumableCrawling;
	}

	public String getUserAgentString() {
		return userAgentString;
	}

	public void setUserAgentString(final String userAgentString) {
		this.userAgentString = userAgentString;
	}

	public Integer getPolitenessDelay() {
		return politenessDelay;
	}

	public void setPolitenessDelay(final Integer politenessDelay) {
		this.politenessDelay = politenessDelay;
	}

	public Boolean getIncludeBinaryContentInCrawling() {
		return includeBinaryContentInCrawling;
	}

	public void setIncludeBinaryContentInCrawling(final Boolean includeBinaryContentInCrawling) {
		this.includeBinaryContentInCrawling = includeBinaryContentInCrawling;
	}

	public Boolean getProcessBinaryContentInCrawling() {
		return processBinaryContentInCrawling;
	}

	public void setProcessBinaryContentInCrawling(final Boolean processBinaryContentInCrawling) {
		this.processBinaryContentInCrawling = processBinaryContentInCrawling;
	}

	public Integer getMaxConnectionsPerHost() {
		return maxConnectionsPerHost;
	}

	public void setMaxConnectionsPerHost(final Integer maxConnectionsPerHost) {
		this.maxConnectionsPerHost = maxConnectionsPerHost;
	}

	public Integer getMaxTotalConnections() {
		return maxTotalConnections;
	}

	public void setMaxTotalConnections(final Integer maxTotalConnections) {
		this.maxTotalConnections = maxTotalConnections;
	}

	public Integer getSocketTimeout() {
		return socketTimeout;
	}

	public void setSocketTimeout(final Integer socketTimeout) {
		this.socketTimeout = socketTimeout;
	}

	public Integer getConnectionTimeout() {
		return connectionTimeout;
	}

	public void setConnectionTimeout(final Integer connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}

	public Integer getMaxOutgoingLinksToFollow() {
		return maxOutgoingLinksToFollow;
	}

	public void setMaxOutgoingLinksToFollow(final Integer maxOutgoingLinksToFollow) {
		this.maxOutgoingLinksToFollow = maxOutgoingLinksToFollow;
	}

	public Integer getMaxDownloadSize() {
		return maxDownloadSize;
	}

	public void setMaxDownloadSize(final Integer maxDownloadSize) {
		this.maxDownloadSize = maxDownloadSize;
	}

	public Boolean getFollowRedirects() {
		return followRedirects;
	}

	public void setFollowRedirects(final Boolean followRedirects) {
		this.followRedirects = followRedirects;
	}

	public Boolean getOnlineTldListUpdate() {
		return onlineTldListUpdate;
	}

	public void setOnlineTldListUpdate(final Boolean onlineTldListUpdate) {
		this.onlineTldListUpdate = onlineTldListUpdate;
	}

	public Boolean getShutdownOnEmptyQueue() {
		return shutdownOnEmptyQueue;
	}

	public void setShutdownOnEmptyQueue(final Boolean shutdownOnEmptyQueue) {
		this.shutdownOnEmptyQueue = shutdownOnEmptyQueue;
	}

	public Integer getThreadMonitoringDelaySeconds() {
		return threadMonitoringDelaySeconds;
	}

	public void setThreadMonitoringDelaySeconds(final Integer threadMonitoringDelaySeconds) {
		this.threadMonitoringDelaySeconds = threadMonitoringDelaySeconds;
	}

	public Integer getThreadShutdownDelaySeconds() {
		return threadShutdownDelaySeconds;
	}

	public void setThreadShutdownDelaySeconds(final Integer threadShutdownDelaySeconds) {
		this.threadShutdownDelaySeconds = threadShutdownDelaySeconds;
	}

	public Integer getCleanupDelaySeconds() {
		return cleanupDelaySeconds;
	}

	public void setCleanupDelaySeconds(final Integer cleanupDelaySeconds) {
		this.cleanupDelaySeconds = cleanupDelaySeconds;
	}

	public String getProxyHost() {
		return proxyHost;
	}

	public void setProxyHost(final String proxyHost) {
		this.proxyHost = proxyHost;
	}

	public Integer getProxyPort() {
		return proxyPort;
	}

	public void setProxyPort(final Integer proxyPort) {
		this.proxyPort = proxyPort;
	}

	public String getProxyUsername() {
		return proxyUsername;
	}

	public void setProxyUsername(final String proxyUsername) {
		this.proxyUsername = proxyUsername;
	}

	public String getProxyPassword() {
		return proxyPassword;
	}

	public void setProxyPassword(final String proxyPassword) {
		this.proxyPassword = proxyPassword;
	}

	public Boolean getRespectNoFollow() {
		return respectNoFollow;
	}

	public void setRespectNoFollow(final Boolean respectNoFollow) {
		this.respectNoFollow = respectNoFollow;
	}

	public Set<BasicHeader> getDefaultHeaders() {
		return defaultHeaders;
	}

	public void setDefaultHeaders(final Set<BasicHeader> defaultHeaders) {
		this.defaultHeaders = defaultHeaders;
	}

	public Integer getThreads() {
		return threads;
	}

	public void setThreads(final Integer threads) {
		this.threads = threads;
	}

	public String getSeleniumPageEncoding() {
		return seleniumPageEncoding;
	}

	public void setSeleniumPageEncoding(String seleniumPageEncoding) {
		this.seleniumPageEncoding = seleniumPageEncoding;
	}





}
