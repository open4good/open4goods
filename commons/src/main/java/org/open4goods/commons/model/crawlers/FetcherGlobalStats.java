package org.open4goods.commons.model.crawlers;

import java.util.Map;

public class FetcherGlobalStats {

	/** The identity of the crawler node **/
	private ApiSynchConfig nodeConfig;


	/** The stats by crawler **/
	private Map<String, IndexationJobStat> crawlerStats;

	private long queueLength;

	private long totalProcessedDatas;


	private long totalIndexedDatas;

	/**
	 * Returns true if stats about the given datasourcename are found.
	 * @param datasource
	 * @return
	 */
	public boolean containsDatasource(final String datasource) {
		if (null == crawlerStats) {
			return false;
		}
		return crawlerStats.values().stream().anyMatch(e -> e.getDatasource().equals(datasource));
	}

	public Map<String, IndexationJobStat> getCrawlerStats() {
		return crawlerStats;
	}

	public void setCrawlerStats(final Map<String, IndexationJobStat> crawlerStats) {
		this.crawlerStats = crawlerStats;
	}

	public long getQueueLength() {
		return queueLength;
	}

	public void setQueueLength(final long totalAssignedPages) {
		queueLength = totalAssignedPages;
	}

	public long getTotalProcessedDatas() {
		return totalProcessedDatas;
	}

	public void setTotalProcessedDatas(final long totalProcessedPages) {
		totalProcessedDatas = totalProcessedPages;
	}

	public ApiSynchConfig getNodeConfig() {
		return nodeConfig;
	}

	public void setNodeConfig(final ApiSynchConfig nodeConfig) {
		this.nodeConfig = nodeConfig;
	}

	public long getTotalIndexedDatas() {
		return totalIndexedDatas;
	}

	public void setTotalIndexedDatas(final long totalIndexedPages) {
		totalIndexedDatas = totalIndexedPages;
	}






}
