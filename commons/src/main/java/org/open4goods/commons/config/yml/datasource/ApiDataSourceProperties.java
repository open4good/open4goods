package org.open4goods.commons.config.yml.datasource;

import org.springframework.validation.annotation.Validated;

/**
 * Specific configuration for an Web (crawlable) DataSource. ApiDataSources reacts on already aggregated datasources.
 * @author Goulven.Furet
 *
 */
@Validated
public class ApiDataSourceProperties {

	/**
	 * In case of of a datasource triggered on each builded aggregated datas sets, the working class name. (must extends AbstractAggregatedDataWorker)
	 * TODO(gof) : put in a separate HtmlDataSourceProperties
	 */
	private String aggregatedDataBackingClass;

	/** In case of api datasource, the api key **/
	private String apiKey;

	/** In case of api datasource, the api secret **/
	private String apiSecret;

	/** In case of api datasource, the api application name**/
	private String apiAppName = "achat-durable.fr";

	/** In case of api datasource, the delay beetween 2 consecutiv calls**/
	private Long apiDelaySeconds = 1L;

	public String getAggregatedDataBackingClass() {
		return aggregatedDataBackingClass;
	}

	public void setAggregatedDataBackingClass(final String aggregatedDataBackingClass) {
		this.aggregatedDataBackingClass = aggregatedDataBackingClass;
	}

	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(final String apiKey) {
		this.apiKey = apiKey;
	}

	public String getApiSecret() {
		return apiSecret;
	}

	public void setApiSecret(final String apiSecret) {
		this.apiSecret = apiSecret;
	}

	public String getApiAppName() {
		return apiAppName;
	}

	public void setApiAppName(final String apiAppName) {
		this.apiAppName = apiAppName;
	}

	public Long getApiDelaySeconds() {
		return apiDelaySeconds;
	}

	public void setApiDelaySeconds(final Long apiDelaySeconds) {
		this.apiDelaySeconds = apiDelaySeconds;
	}


}
