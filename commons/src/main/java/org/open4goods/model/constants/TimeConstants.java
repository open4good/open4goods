package org.open4goods.model.constants;

/**
 * All shared urls's time constants
 * @author goulven
 *
 */
public class TimeConstants {


	public static final long CRAWLER_UPDATE_STATUS_TO_API_MS = 5000L;
	public static final int API_EXPIRED_UNSEEN_CRAWLERS_IN_SECONDS = 10;
	// The delay where terminated crawlers will be removed
	public static final long CRAWLER_REMOVE_FINISHED_CRAWLERS_MS = 3000L;
	// The Relations data (brands, conservateur, ...)  build frequency
	public static final long RELATION_DATA_AGGREGATION_PERIOD = 1000 * 3600 * 10;
	// Period of capsule update againt master API
	public static final long CAPSULE_QUERY_UPDATE_INTERVAL = 10000;

	// The caching duration for provider icons
	public static final long PROVIDER_ICO_DELAY_UPDATE = 1000 * 3600 * 24;


	// Constants to easyest cron manipulations
	public static final String CRON_DAY = "DAY";
	public static final String CRON_WEEK = "WEEK";
	public static final String CRON_MONTH = "MONTH";

	// Delay of capsule generation
	public static final long CAPSULE_GENERATION_DELAY_MS = 3600 * 1000 * 12;

	// Delay of brand logo downloads
	public static final long BRAND_LOGO_GENERATION = 3600 * 1000 * 24 * 10;

//	// Delay for updating maxmind geoloc service
//	public static final long MAXMIND_DB_UPDATE = 3600 * 1000 * 24 * 7;

	// Frequency against which mail should be send if there are alerting events
	public static final long ALERTING_MAIL_FREQUENCY  = 1000 *  60  * 30 * 1 ;

//	// In days, when to reload insee datas
//	public static final long INSEE_DATASOURCE_FETCHING = 90 * 3600 * 24 * 1000;

	// Frequency of export and cleanup
	public static final long DATAFRAGMENT_EXPORT_AND_CLEANUP_FREQUENCY = 3600 * 1000 * 24 * 7;
	public static final long ONE_DAY = 3600 * 1000 * 24;

}
