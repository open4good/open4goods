package org.open4goods.crawler.services.fetching;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.open4goods.commons.config.yml.datasource.DataSourceProperties;
import org.open4goods.commons.model.crawlers.IndexationJobStat;
import org.open4goods.crawler.config.yml.FetcherProperties;
import org.open4goods.crawler.repository.IndexationRepository;
import org.open4goods.crawler.services.DataFragmentCompletionService;
import org.open4goods.crawler.services.IndexationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.dataformat.csv.CsvSchema;

import jakarta.annotation.PreDestroy;

/**
 * Service that handles the csv datasources fetching 
 * 
 * @author goulven 
 */

public class CsvDatasourceFetchingService extends DatasourceFetchingService {

	
	private static final Logger logger = LoggerFactory.getLogger(CsvDatasourceFetchingService.class);


	private final IndexationService indexationService;
	

	private final DataFragmentCompletionService completionService;

	private final WebDatasourceFetchingService webFetchingService;

	private final DatasourceFetchingService fetchingService;
	
	

	// The running job status
	private final Map<String, CsvIndexationWorker> runningJobs = new ConcurrentHashMap<>();

	private final FetcherProperties fetcherProperties;

	private IndexationRepository csvIndexationRepository;

	private BlockingQueue<DataSourceProperties> queue = null;

	// The chars used in CSV after libreoffice sanitisation
//	private static final char SANITISED_COLUMN_SEPARATOR = ';';
//	private static final char SANITIZED_ESCAPE_CHAR = '"';
//	private static final char SANITIZED_QUOTE_CHAR = '"';
	
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
			DatasourceFetchingService fetchingService, final String logsFolder, boolean toConsole
			) {
		super(logsFolder, toConsole,indexationRepository);
		this.indexationService = indexationService;
		this.webFetchingService = webFetchingService;
		this.completionService = completionService;
		this.fetcherProperties = fetcherProperties;
		this.fetchingService = fetchingService;
		// The CSV executor can have at most the fetcher max indexation tasks threads
		
		this.queue = new LinkedBlockingQueue<>(fetcherProperties.getConcurrentFetcherTask());
//		executor = Executors.newFixedThreadPool(fetcherProperties.getConcurrentFetcherTask(), Thread.ofVirtual().factory());
//		executor = Executors.newFixedThreadPool(fetcherProperties.getConcurrentFetcherTask());
		this.csvIndexationRepository = csvIndexationRepository;
		
		for (int i = 0; i < fetcherProperties.getConcurrentFetcherTask(); i++) {			
			// TODO(conf,p3) : wait (4000) from config
			Thread.startVirtualThread(new CsvIndexationWorker(this,completionService,indexationService, webFetchingService, csvIndexationRepository,  4000,logsFolder));
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

	    Map<String, Long> escapeCharsCandidates = new HashMap<>();
	    escapeCharsCandidates.put("\\", 0L);

	    // Process the file, line by line (within limit)
	    try (Stream<String> lines = Files.lines(file.toPath())) {
	        lines.limit(MAX_LINES_TO_READ).forEach(line -> {
	            // Count occurrences of separators
	            for (String separator : columnSeparatorsCandidates.keySet()) {
	                if (line.contains(separator)) {
	                    columnSeparatorsCandidates.put(separator, columnSeparatorsCandidates.get(separator) + 1);
	                }
	            }

	            // Count occurrences of quote characters
	            for (String quote : quoteCharsCandidates.keySet()) {
	                if (line.contains(quote)) {
	                    quoteCharsCandidates.put(quote, quoteCharsCandidates.get(quote) + 1);
	                }
	            }

	            // Count occurrences of escape characters
	            for (String escape : escapeCharsCandidates.keySet()) {
	                if (line.contains(escape)) {
	                    escapeCharsCandidates.put(escape, escapeCharsCandidates.get(escape) + 1);
	                }
	            }
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

	    // Find the most frequent escape character (if needed)
	    String escapeChar = escapeCharsCandidates.entrySet().stream()
	        .max(Map.Entry.comparingByValue())
	        .map(Map.Entry::getKey)
	        .orElse(null);

	    // Set a minimum threshold for detected separator and quote character (e.g., 10 occurrences)
	    final long MIN_OCCURRENCES = 10;

	    if (columnSeparator != null && columnSeparatorsCandidates.get(columnSeparator) < MIN_OCCURRENCES) {
	        columnSeparator = null;
	    }

	    if (quoteChar != null && quoteCharsCandidates.get(quoteChar) < MIN_OCCURRENCES) {
	        quoteChar = null;
	    }

	    // If no quote character is found, we may not need an escape character
	    if (quoteChar == null) {
	        escapeChar = null;
	    }

	    // Build the CSV schema
	    CsvSchema.Builder builder = CsvSchema.builder();

	    if (columnSeparator != null) {
	        builder.setColumnSeparator(columnSeparator.charAt(0));
	    }

	    if (quoteChar != null) {
	        builder.setQuoteChar(quoteChar.charAt(0));
	    }

	    if (escapeChar != null && escapeCharsCandidates.get(escapeChar) > 0) {
	        builder.setEscapeChar(escapeChar.charAt(0));
	    }
	    
	    // Optionally handle escape character if needed (currently null)
	    // builder.setEscapeChar(...);

	    // Use header if available
	    builder.setUseHeader(true);

	    logger.info("Autodetected CSV schema for file {}: separator='{}', quote='{}', escape='{}'", file.getAbsolutePath(), columnSeparator, quoteChar, escapeChar);

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
	
	
}
