package org.open4goods.crawler.services.fetching;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.zip.GZIPInputStream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.open4goods.config.yml.datasource.CsvDataSourceProperties;
import org.open4goods.config.yml.datasource.DataSourceProperties;
import org.open4goods.crawler.config.yml.FetcherProperties;
import org.open4goods.crawler.repository.CsvIndexationRepository;
import org.open4goods.crawler.repository.IndexationRepository;
import org.open4goods.crawler.services.DataFragmentCompletionService;
import org.open4goods.crawler.services.IndexationService;
import org.open4goods.exceptions.ValidationException;
import org.open4goods.helper.InStockParser;
import org.open4goods.helper.ProductStateParser;
import org.open4goods.helper.ShippingCostParser;
import org.open4goods.helper.ShippingTimeParser;
import org.open4goods.model.constants.InStock;
import org.open4goods.model.constants.ReferentielKey;
import org.open4goods.model.constants.ResourceTagDictionary;
import org.open4goods.model.crawlers.FetchingJobStats;
import org.open4goods.model.data.DataFragment;
import org.open4goods.model.data.Price;
import org.open4goods.model.data.Rating;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.google.common.collect.Sets;

import edu.uci.ics.crawler4j.crawler.CrawlController;
import jakarta.annotation.PreDestroy;

/**
 * Service that handles the csv datasources fetching TODO(gof) : implement
 * productState
 * 
 * @author goulven TODO(gof) by datasource dedicated logging
 */

public class CsvDatasourceFetchingService extends DatasourceFetchingService {

	
	private static final Logger logger = LoggerFactory.getLogger(CsvDatasourceFetchingService.class);


	private final IndexationService indexationService;
	

	private final DataFragmentCompletionService completionService;

	private final WebDatasourceFetchingService webFetchingService;

	private final DatasourceFetchingService fetchingService;
	
	

	// The running job status
	private final Map<String, FetchingJobStats> running = new ConcurrentHashMap<>();

	private final FetcherProperties fetcherProperties;

	private CsvIndexationRepository csvIndexationRepository;

	private BlockingQueue<DataSourceProperties> queue = null;

	// The chars used in CSV after libreoffice sanitisation
	private static final char SANITISED_COLUMN_SEPARATOR = ';';
	private static final char SANITIZED_ESCAPE_CHAR = '"';
	private static final char SANITIZED_QUOTE_CHAR = '"';
	
	/**
	 * Constructor
	 *
	 * @param indexationService
	 * @param fetcherProperties
	 * @param webFetchingService
	 */
	public CsvDatasourceFetchingService(final CsvIndexationRepository csvIndexationRepository,   final DataFragmentCompletionService completionService,
			final IndexationService indexationService, final FetcherProperties fetcherProperties,
			final WebDatasourceFetchingService webFetchingService, IndexationRepository indexationRepository,
			DatasourceFetchingService fetchingService, final String logsFolder, boolean toConsole
			) {
		super(logsFolder, toConsole,indexationRepository);
		this.indexationService = indexationService;
		this.webFetchingService = webFetchingService;
		this.completionService = completionService;
		this.fetcherProperties = fetcherProperties;
		this.fetchingService = fetchingService;
		// The CSV executor can have at most the fetcher max indexation tasks threads
		
		// TODO : Limit from conf
		
		this.queue = new LinkedBlockingQueue<>(fetcherProperties.getConcurrentFetcherTask());
//		executor = Executors.newFixedThreadPool(fetcherProperties.getConcurrentFetcherTask(), Thread.ofVirtual().factory());
//		executor = Executors.newFixedThreadPool(fetcherProperties.getConcurrentFetcherTask());
		this.csvIndexationRepository = csvIndexationRepository;
		
		for (int i = 0; i < fetcherProperties.getConcurrentFetcherTask(); i++) {			
			// TODO : gof : wait 4secs from conf
			Thread.startVirtualThread(new CsvIndexationWorker(this,completionService,indexationService, webFetchingService, csvIndexationRepository,  4000,logsFolder,toConsole));
		
		}
	}

	/**
	 * Starting a crawl
	 */
	@Override
	public void start(final DataSourceProperties pConfig, final String datasourceConfName) {
	
		try {
			queue.put(pConfig);
		} catch (InterruptedException e) {
			logger.error("Error while putting csv fetching job in queue",e);
		}
		
	}

	@Override
	public void stop(final String providerName) {
		indexationService.clearIndexedCounter(providerName);
		running.get(providerName).setShuttingDown(true);
	}

	@Override
	public Map<String, FetchingJobStats> stats() {
		// Updating indexed counters
		for (final FetchingJobStats js : running.values()) {
			js.setNumberOfIndexedDatas(indexationService.getIndexed(js.getName()));
		}
		return running;
	}



	/**
	 * Stopping jobs on application exit
	 */
	@PreDestroy
	private void destroy() {
		for (final String provider : running.keySet()) {
			stop(provider);
		}
//		executor.shutdown();
	}


	
	
	public BlockingQueue<DataSourceProperties> getQueue() {
		return queue;
	}

	public Map<String, FetchingJobStats> getRunning() {
		return running;
	}
	
	
	
}
