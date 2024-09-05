package org.open4goods.crawler.services.fetching;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.open4goods.commons.config.yml.datasource.DataSourceProperties;
import org.open4goods.commons.model.crawlers.IndexationJobStat;
import org.open4goods.commons.services.RemoteFileCachingService;
import org.open4goods.crawler.config.yml.FetcherProperties;
import org.open4goods.crawler.repository.IndexationRepository;
import org.open4goods.crawler.services.DataFragmentCompletionService;
import org.open4goods.crawler.services.IndexationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Health.Builder;
import org.springframework.boot.actuate.health.HealthIndicator;

import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.mchange.lang.StringUtils;

import jakarta.annotation.PreDestroy;

/**
 * Service that handles the csv datasources fetching 
 * 
 * @author goulven 
 */

public class CsvDatasourceFetchingService extends DatasourceFetchingService implements HealthIndicator{

	
	private static final Logger logger = LoggerFactory.getLogger(CsvDatasourceFetchingService.class);


	private static final Integer JOBS_QUEUE_CAPACITY = 1000;
	private BlockingQueue<DataSourceProperties> queue = new LinkedBlockingQueue<>(JOBS_QUEUE_CAPACITY);


	private final IndexationService indexationService;
	
	// The running job status
	private final Map<String, CsvIndexationWorker> runningJobs = new ConcurrentHashMap<>();

	private RemoteFileCachingService remoteFileCachingService;
	

	private AtomicLong feedNoUrls = new AtomicLong(0L);
	private AtomicLong brokenCsvFiles = new AtomicLong(0L);
	
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
			Thread.startVirtualThread(new CsvIndexationWorker(this,completionService,indexationService, webFetchingService, csvIndexationRepository,  4000,logsFolder, remoteFileCachingService));
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
		runningJobs.get(providerName).stop();
	}

	@Override
	public Map<String, IndexationJobStat> stats() {
		return runningJobs.entrySet().stream()
				.collect(Collectors.toMap(e-> e.getKey(), e -> e.getValue().stats()));
	}


	/**
	 * CSV Schema autodetection
	 * @param file the CSV file
	 * @return the detected CsvSchema
	 * @throws IOException if an I/O error occurs
	 */
	public CsvSchema detectSchema(File file) throws IOException {
	    logger.info("Autodetecting CSV schema for file {}", file.getAbsolutePath());
	    
	    // TODO: Get number of lines to read from config (limit 10000 in this case)
	    // NOTE : When having headers, this is enough !
	    final int MAX_LINES_TO_READ = 10000;

	    // Potential column separators, quote characters, and escape characters from config (can be externalized)
	    Map<String, Long> columnSeparatorsCandidates = new HashMap<>();
	    columnSeparatorsCandidates.put(";", 0L);
	    columnSeparatorsCandidates.put(",", 0L);
	    columnSeparatorsCandidates.put("|", 0L);
	    columnSeparatorsCandidates.put("\t", 0L);

	    Map<String, Long> quoteCharsCandidates = new HashMap<>();
	    quoteCharsCandidates.put("\"", 0L);
	    quoteCharsCandidates.put("'", 0L);
	    quoteCharsCandidates.put("`", 0L);

//	    Map<String, Long> escapeCharsCandidates = new HashMap<>();
//	    escapeCharsCandidates.put("\\", 0L);

	    // Process the file, line by line (within limit)
	    try (Stream<String> lines = Files.lines(file.toPath())) {
	        lines.limit(MAX_LINES_TO_READ).forEach(line -> {

	        	
	        	// Count occurrences of separators
	            for (String separator : columnSeparatorsCandidates.keySet()) {
	                    columnSeparatorsCandidates.put(separator, columnSeparatorsCandidates.get(separator) + org.apache.commons.lang3.StringUtils.countMatches(line, separator) );
	            }

	            // Count occurrences of quote characters
	            for (String quote : quoteCharsCandidates.keySet()) {
	                if (line.contains(quote)) {
	                    quoteCharsCandidates.put(quote, quoteCharsCandidates.get(quote) +  org.apache.commons.lang3.StringUtils.countMatches(line, quote));
	                }
	            }

//	            // Count occurrences of escape characters
//	            for (String escape : escapeCharsCandidates.keySet()) {
//	                if (line.contains(escape)) {
//	                    escapeCharsCandidates.put(escape, escapeCharsCandidates.get(escape) + 1);
//	                }
//	            }
	        });
	    }

	    // Find the most frequent column separator
	    String columnSeparator = columnSeparatorsCandidates.entrySet().stream()
	        .max(Map.Entry.comparingByValue())
	        .map(Map.Entry::getKey)
	        .orElse(null);

	    // Find the most frequent quote character
	    String quoteChar = quoteCharsCandidates.entrySet().stream()
	        .max(Map.Entry.comparingByValue())
	        .map(Map.Entry::getKey)
	        .orElse(null);

//	    // Find the most frequent escape character (if needed)
//	    String escapeChar = escapeCharsCandidates.entrySet().stream()
//	        .max(Map.Entry.comparingByValue())
//	        .map(Map.Entry::getKey)
//	        .orElse(null);

	    // Set a minimum threshold for detected separator and quote character (e.g., 10 occurrences)
	    final long MIN_OCCURRENCES = 10;

	    if (columnSeparator != null && columnSeparatorsCandidates.get(columnSeparator) < MIN_OCCURRENCES) {
	        columnSeparator = null;
	    }

	    if (quoteChar != null && quoteCharsCandidates.get(quoteChar) < MIN_OCCURRENCES) {
	        quoteChar = null;
	    }


	    // Build the CSV schema
	    CsvSchema.Builder builder = CsvSchema.builder();

	    if (columnSeparator != null) {
	        builder.setColumnSeparator(columnSeparator.charAt(0));
	    }

	    if (quoteChar != null) {
	        builder.setQuoteChar(quoteChar.charAt(0));
	    }


	    // TODO(design,p2) : not handling escape chars dynamically : detected \ seems not to be really escapechars...  
//	    if (escapeChar != null && escapeCharsCandidates.get(escapeChar) > 0) {
//	        builder.setEscapeChar(escapeChar.charAt(0));
//	    }
//	    
	    
	    // Use header if available
	    builder.setUseHeader(true);

	    logger.info("Autodetected CSV schema for file {}: separator='{}', quote='{}'", file.getAbsolutePath(), columnSeparator, quoteChar);

	    return builder.build();
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

	public synchronized void brokenCsv() {
		brokenCsvFiles.incrementAndGet();
	}

	/**
	 * Custom healthcheck, simply goes to DOWN if critical exception occurs
	 */
	@Override
	public Health health() {
		
		Builder health = Health.up();
		
		
		if (feedNoUrls.get() > 0L) {
			health =  Health.down();
		} 
		
		if (brokenCsvFiles.get() > 0L) {
			health =  Health.down();
		} 
		
		return health
				.withDetail("feed_without_urls", feedNoUrls.get() )
				.withDetail("invalid_csv_files", brokenCsvFiles.get())
				.build();
	}
	
	
}
