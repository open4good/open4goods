package org.open4goods.crawler.services;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.validation.constraints.NotBlank;

import org.open4goods.model.constants.UrlConstants;
import org.open4goods.model.data.DataFragment;
import org.open4goods.model.dto.api.IndexationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

public class IndexationService {

	private final static Logger log = LoggerFactory.getLogger(IndexationService.class);

	/**
	 * The map that contains indexation attempt by datasource
	 */
	protected Map<String, Long> byProviderCounters = new ConcurrentHashMap<>();

	/**
	 * Map containing the last indexed data fragment per provider
	 */
	protected Map<String, DataFragment> lastIndexed = new ConcurrentHashMap<>();

	private String indexationEndpoint;
	private String apiKey;

	public IndexationService() {
		super();
	}

	public IndexationService(final String indexationEndpoint, final String apiKey) {
		super();
		this.indexationEndpoint = indexationEndpoint;
		this.apiKey = apiKey;
	}

	public void index(final DataFragment data, final String datasourceConfigName) {
		// Incrementing the counters
		byProviderCounters.compute(datasourceConfigName, (k, v) -> v == null ? 1 : v + 1);
		lastIndexed.put(datasourceConfigName, data);

		// Effectiv indexation
		indexInternal(data);
	}


	protected void indexInternal(final DataFragment data) {
		try {
			Unirest.post(indexationEndpoint).header("accept", "application/json")
					.header("Content-Type", "application/json").header(UrlConstants.APIKEY_PARAMETER, apiKey).body(data)
					.asObject(IndexationResponse.class);
		} catch (final UnirestException e) {
			log.error("Cannot send {} to master API indexation endpoint : {}", data, e.getMessage());
		}
	}

	public Long getIndexed(final String dataSourceName) {
		final Long ret = byProviderCounters.get(dataSourceName);
		return ret == null ? 0L : ret;
	}

	public void clearIndexedCounter(final String providerName) {
		byProviderCounters.remove(providerName);
	}

	public DataFragment getLastIndexed(@NotBlank final String datasourceName) {
		return lastIndexed.get(datasourceName);
	}

}
