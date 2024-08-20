package org.open4goods.crawler.services.fetching;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

import org.open4goods.config.yml.datasource.DataSourceProperties;
import org.open4goods.crawler.config.yml.FetcherProperties;
import org.open4goods.crawler.repository.CsvIndexationRepository;
import org.open4goods.crawler.repository.IndexationRepository;
import org.open4goods.crawler.services.DataFragmentCompletionService;
import org.open4goods.crawler.services.IndexationService;
import org.open4goods.model.crawlers.WebIndexationStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.dataformat.csv.CsvSchema;

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
	private final Map<String, WebIndexationStats> running = new ConcurrentHashMap<>();

	private final FetcherProperties fetcherProperties;

	private CsvIndexationRepository csvIndexationRepository;

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
		running.get(providerName).setShuttingDown(true);
	}

	@Override
	public Map<String, WebIndexationStats> stats() {
		// Updating indexed counters
		for (final WebIndexationStats js : running.values()) {
			js.setNumberOfIndexedDatas(indexationService.getIndexed(js.getName()));
		}
		return running;
	}


	/**
	 * CSV Schema autodetection
	 * @param f
	 * @return
	 * @throws IOException 
	 */
	public CsvSchema detectSchema (File f) throws IOException {
		
		logger.info("Autodetecting CSV schema for file {}",f.getAbsolutePath());
		// TODO : Numer of lines from conf
		 List<String> lines = Files.lines(f.toPath()).limit(1000).collect(Collectors.toList());
	

		 // The potential columns separators
		 Map<String,Long> columnsSeparatorsCandidates = new HashMap<>();
		 // TODO from conf
		 columnsSeparatorsCandidates.put(";", 0L);
		 columnsSeparatorsCandidates.put(",", 0L);
		 columnsSeparatorsCandidates.put("|", 0L);
		 columnsSeparatorsCandidates.put("\t", 0L);
		 

		 Map<String,Long> quoteCharsCandidates = new HashMap<>();
		 // TODO from conf
		 quoteCharsCandidates.put("\"", 0L);
		 quoteCharsCandidates.put("'", 0L);
		 quoteCharsCandidates.put("`", 0L);
		 
		 
		 Map<String,Long> escapeCharsCandidates = new HashMap<>();
		 // TODO from conf		 
		 escapeCharsCandidates.put("\\", 0L);
		 
		 
		 // Counting the number of lines containing the separator
		for (String line : lines) {
			for (String separator : columnsSeparatorsCandidates.keySet()) {
				if (line.contains(separator)) {
					columnsSeparatorsCandidates.put(separator, columnsSeparatorsCandidates.get(separator) + 1);
				}
			}

			for (String quote : quoteCharsCandidates.keySet()) {
				if (line.contains(quote)) {
					quoteCharsCandidates.put(quote, quoteCharsCandidates.get(quote) + 1);
				}
			}

			for (String escape : escapeCharsCandidates.keySet()) {
				if (line.contains(escape)) {
					escapeCharsCandidates.put(escape, escapeCharsCandidates.get(escape) + 1);
				}
			}
		}
		
		
		// Electing the most frequent separator
		
		String columnSeparator = columnsSeparatorsCandidates.entrySet().stream().max((e1,e2) -> e1.getValue().compareTo(e2.getValue())).get().getKey();
		String quoteChar = quoteCharsCandidates.entrySet().stream().max((e1,e2) -> e1.getValue().compareTo(e2.getValue())).get().getKey();
		String escapeChar = null ; // escapeCharsCandidates.entrySet().stream().max((e1,e2) -> e1.getValue().compareTo(e2.getValue())).get().getKey();
		
		
		// Evicting to unset the use of separator if not a minimum of occurences
		
		if (columnsSeparatorsCandidates.get(columnSeparator) < 10) {
			columnSeparator = null;
		}
		
		if (quoteCharsCandidates.get(quoteChar) < 10) {
			quoteChar = null;
		}
		
		
		// Special handling escape char if no quote separator
		if (quoteChar == null && null != escapeChar) {
			escapeChar = null;
			// TODO : test
		}
		
		CsvSchema.Builder builder = CsvSchema.builder();
		
		if (columnSeparator != null) {
			builder.setColumnSeparator(columnSeparator.charAt(0));
		}
	
		if (quoteChar != null) {
			builder.setQuoteChar(quoteChar.charAt(0));
		}
		
		
		logger.warn("Autodetected CSV schema for file {} : separator {}, quote {}, escape {}",f.getAbsolutePath(),columnSeparator,quoteChar,escapeChar);
		// All catalogs use header
		builder.setUseHeader(true);
		
		return builder.build();
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

	public Map<String, WebIndexationStats> getRunning() {
		return running;
	}
	
	
	
}
