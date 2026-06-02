package org.open4goods.crawler.services.fetching;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.commons.config.yml.datasource.DataSourceProperties;
import org.open4goods.commons.model.crawlers.IndexationJobStat;
import org.open4goods.crawler.config.yml.FetcherProperties;
import org.open4goods.crawler.repository.IndexationRepository;
import org.open4goods.crawler.services.DataFragmentCompletionService;
import org.open4goods.crawler.services.IndexationService;
import org.open4goods.services.remotefilecaching.service.RemoteFileCachingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Health.Builder;
import org.springframework.boot.health.contributor.HealthIndicator;

import tools.jackson.dataformat.csv.CsvSchema;

import jakarta.annotation.PreDestroy;

/**
 * Service that handles the csv datasources fetching 
 * 
 * @author goulven 
 */

public class CsvDatasourceFetchingService extends DatasourceFetchingService implements HealthIndicator{

	
	private static final Logger logger = LoggerFactory.getLogger(CsvDatasourceFetchingService.class);
	private final CsvDialectDetector csvDialectDetector = new CsvDialectDetector();


	private static final Integer JOBS_QUEUE_CAPACITY = 1000;
	private BlockingQueue<DataSourceProperties> queue = new LinkedBlockingQueue<>(JOBS_QUEUE_CAPACITY);


	private final IndexationService indexationService;
	
	// The running job status
	private final Map<String, CsvIndexationWorker> runningJobs = new ConcurrentHashMap<>();

	private RemoteFileCachingService remoteFileCachingService;
	

	private AtomicLong feedNoUrls = new AtomicLong(0L);
	private Set<String> brokenCsvFiles = Collections.synchronizedSet(new HashSet<String>());
	
	/**
	 * Constructor
	 *
	 * @param indexationService
	 * @param fetcherProperties
	 * @param webFetchingService
	 */
	public CsvDatasourceFetchingService(final IndexationRepository csvIndexationRepository,   final DataFragmentCompletionService completionService,
			final IndexationService indexationService, final FetcherProperties fetcherProperties,
			final WebDatasourceFetchingService webFetchingService, IndexationRepository indexationRepository,
			DatasourceFetchingService fetchingService, RemoteFileCachingService remoteFileCachingService, final String logsFolder
			) {
		super(logsFolder, indexationRepository);
		this.indexationService = indexationService;
		this.remoteFileCachingService = remoteFileCachingService;
		// The CSV executor can have at most the fetcher max indexation tasks threads
		
//		executor = Executors.newFixedThreadPool(fetcherProperties.getConcurrentFetcherTask(), Thread.ofVirtual().factory());
//		executor = Executors.newFixedThreadPool(fetcherProperties.getConcurrentFetcherTask());
		
		for (int i = 0; i < fetcherProperties.getConcurrentFetcherTask(); i++) {			
			// TODO(conf,p3) : wait (4000) from config
			new Thread(new CsvIndexationWorker(this,completionService,indexationService, webFetchingService, csvIndexationRepository,  4000,logsFolder, remoteFileCachingService)).start();
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
	}

	@Override
	public Map<String, IndexationJobStat> stats() {
		return runningJobs.entrySet().stream()
				.collect(Collectors.toMap(e-> e.getKey(), e -> e.getValue().stats()));
	}


	/**
	 * Detects the CSV dialect (separator and quote character).
	 *
	 * @param file    the CSV file to inspect
	 * @param charset encoding used to read the file (e.g. {@code UTF-8} or {@code ISO-8859-1})
	 * @return a {@link CsvSchema} with {@code useHeader=true} and the detected separator/quote
	 * @throws IOException if the file cannot be read
	 */
	public CsvSchema detectSchema(File file, Charset charset) throws IOException
	{
	    logger.info("Autodetecting CSV schema for file {} (charset {})", file.getAbsolutePath(), charset);
	    CsvSchema schema = csvDialectDetector.detectSchema(file, charset);
	    logger.warn("Auto detected schema is quoteChar:{} separatorChar:{} escapeChar:none",
		        schema.getQuoteChar() == -1 ? "none" : Character.toString((char) schema.getQuoteChar()),
		        schema.getColumnSeparator() == -1 ? "none" : Character.toString((char) schema.getColumnSeparator()));
	    return schema;
	}

	
	
	

	/**
	 * Stopping jobs on application exit
	 */
	@PreDestroy
	private void destroy() {
		for (final String provider : runningJobs.keySet()) {
			stop(provider);
		}
	}

	
	public BlockingQueue<DataSourceProperties> getQueue() {
		return queue;
	}

	
	// To provide Data to the health check
	public synchronized void incrementFeedNoUrls() {
		feedNoUrls.incrementAndGet();
		
	}

	public synchronized void brokenCsv(String url) {
		brokenCsvFiles.add(url);
	}

	/**
	 * Custom healthcheck, simply goes to DOWN if critical exception occurs
	 */
	@Override
	public Health health() {
		
		Builder health = Health.up();
		
		//TODO(P1, fiability) : put back when v1 stabilized
		if (feedNoUrls.get() > 0L) {
//			health =  Health.down();
		} 
		
		if (brokenCsvFiles.size() > 0L) {
//			health =  Health.down();
		} 
		
		return health
				.withDetail("feed_without_urls", feedNoUrls.get() )
				.withDetail("invalid_csv_files", StringUtils.join(brokenCsvFiles ,"\n"))
				.build();
	}
	
	
}
