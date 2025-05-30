
package org.open4goods.crawler.services;

import java.util.HashMap;
import java.util.Map;

import org.open4goods.commons.config.yml.datasource.DataSourceProperties;
import org.open4goods.commons.model.crawlers.FetcherGlobalStats;
import org.open4goods.commons.model.crawlers.IndexationJobStat;
import org.open4goods.commons.model.crawlers.IndexationJobStat;
import org.open4goods.crawler.config.yml.FetcherProperties;
import org.open4goods.crawler.services.fetching.CsvDatasourceFetchingService;
import org.open4goods.crawler.services.fetching.WebDatasourceFetchingService;
import org.open4goods.model.exceptions.TechnicalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.uci.ics.crawler4j.crawler.WebCrawler;

/**
 * The FetchersService is in charge of providing orchestration and stats functions over the various
 *  data retrieving jobs (WebDataSource, CsvDataSource)
 *
 * @author Goulven.Furet
 *  TODO(design) : Should be a unique map of DatasourceFetchingService maintained here, and no multiple *FetchingService....
 */
public class FetchersService{

	protected static final Logger logger = LoggerFactory.getLogger(WebCrawler.class);

	private final FetcherProperties config;

	private final CsvDatasourceFetchingService csvDatasourceFetchingService;

	private final WebDatasourceFetchingService webDatasourceFetchingService;


	/**
	 * Constructor
	 * @param config
	 * @param csvDatasourceFetchingService
	 * @param indexationService
	 */
	public FetchersService(final FetcherProperties config, final WebDatasourceFetchingService webDatasourceFetchingService, final CsvDatasourceFetchingService csvDatasourceFetchingService) {
		super();
		this.config = config;
		this.csvDatasourceFetchingService = csvDatasourceFetchingService;
		this.webDatasourceFetchingService = webDatasourceFetchingService;
	}

	/**
	 * Async start a fetcher job
	 * @param provider
	 * @throws TechnicalException 
	 */
	public void start(final DataSourceProperties provider, final String datasourceConfName) throws TechnicalException {

		logger.info("Will start fetcher job : {}", datasourceConfName);
		if (null != provider.getCsvDatasource()) {
			logger.info("Starting csv fetching of {}",datasourceConfName);
			csvDatasourceFetchingService.start(provider,datasourceConfName);
		}else if (null != provider.getWebDatasource()) {
			logger.info("Starting web fetching of {}",datasourceConfName);
			webDatasourceFetchingService.start(provider,datasourceConfName);
		}

		else {
			logger.warn("No fetching mode defined for {}",datasourceConfName);
		}
	}


	public void stop(final DataSourceProperties provider, final String datasourceConfName) {
		if (null != provider.getCsvDatasource()) {
			logger.info("Stoping csv fetching of {}",datasourceConfName);
			csvDatasourceFetchingService.stop(datasourceConfName);
		}else if (null != provider.getWebDatasource()) {
			logger.info("Stoping web fetching of {}",datasourceConfName);
			webDatasourceFetchingService.stop(datasourceConfName);
		} 



		else {
			logger.warn("No fetching mode defined for {}",datasourceConfName);
		}
	}


	/**
	 * Computing the global stats for this Fetcher
	 */
	public FetcherGlobalStats stats() {

		final Map<String, IndexationJobStat> ret = new HashMap<>();

		ret.putAll( webDatasourceFetchingService.stats());
		ret.putAll( csvDatasourceFetchingService.stats());


		long totQueue = 0L, totProcessed = 0L, totIndexed = 0L;
		// Computing the global stats from each crawlers one
		for (final IndexationJobStat s : ret. values()) {
			totIndexed += s.getIndexed();
			totProcessed += s.getProcessed();
		}

		final FetcherGlobalStats gs = new FetcherGlobalStats();
		gs.setCrawlerStats(ret);
		gs.setNodeConfig(config.getApiSynchConfig());
		gs.setQueueLength(totQueue);
		gs.setTotalProcessedDatas(totProcessed);
		gs.setTotalIndexedDatas(totIndexed);

		return gs;

	}

	public CsvDatasourceFetchingService getCsvDatasourceFetchingService() {
		return csvDatasourceFetchingService;
	}

	public WebDatasourceFetchingService getWebDatasourceFetchingService() {
		return webDatasourceFetchingService;
	}

}
