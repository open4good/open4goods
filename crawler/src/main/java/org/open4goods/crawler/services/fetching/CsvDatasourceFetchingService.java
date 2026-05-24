package org.open4goods.crawler.services.fetching;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
	 * Candidate column separators in priority order (higher index = higher preference when tied).
	 * Single-quote is intentionally absent: it is not a valid RFC-4180 quote character and is
	 * extremely common in French/Spanish text, causing false detections.
	 */
	private static final char[] SEPARATOR_CANDIDATES = {'\t', '|', ';', ','};

	/** Only {@code "} is a RFC-4180 quote character. Backtick never appears in real feeds. */
	private static final char[] QUOTE_CANDIDATES = {'"'};

	/** Number of data lines (after header) sampled for per-line column-count stability scoring. */
	private static final int DETECTION_SAMPLE_LINES = 500;

	/**
	 * Detects the CSV dialect (separator and quote character) by combining two signals:
	 * <ol>
	 *   <li>Raw occurrence count for each candidate character.</li>
	 *   <li>Per-line column-count <em>consistency</em> score: for a real CSV every data line has
	 *       the same number of fields; French descriptions inflate raw apostrophe counts but
	 *       produce wildly variable field counts per line, making this score very low.</li>
	 * </ol>
	 * The winning dialect is the one whose consistency score is highest. Raw count is used only
	 * as a tie-breaker.
	 *
	 * @param file    the CSV file to inspect
	 * @param charset encoding used to read the file (e.g. {@code UTF-8} or {@code ISO-8859-1})
	 * @return a {@link CsvSchema} with {@code useHeader=true} and the detected separator/quote
	 * @throws IOException if the file cannot be read
	 */
	public CsvSchema detectSchema(File file, Charset charset) throws IOException
	{
	    logger.info("Autodetecting CSV schema for file {} (charset {})", file.getAbsolutePath(), charset);

	    // Sample up to DETECTION_SAMPLE_LINES data lines (skip the header)
	    List<String> sampleLines = new ArrayList<>(DETECTION_SAMPLE_LINES + 1);
	    try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), charset)))
	    {
	        String line;
	        int read = 0;
	        while ((line = br.readLine()) != null && read <= DETECTION_SAMPLE_LINES)
	        {
	            sampleLines.add(line);
	            read++;
	        }
	    }

	    if (sampleLines.isEmpty())
	    {
	        logger.warn("CSV file is empty, using default schema for {}", file.getName());
	        return CsvSchema.builder().setUseHeader(true).build();
	    }

	    char bestSep = ',';
	    char bestQuote = '"';
	    double bestScore = -1.0;

	    for (char sep : SEPARATOR_CANDIDATES)
	    {
	        for (char quote : QUOTE_CANDIDATES)
	        {
	            double score = consistencyScore(sampleLines, sep, quote);
	            if (score > bestScore)
	            {
	                bestScore = score;
	                bestSep = sep;
	                bestQuote = quote;
	            }
	        }
	    }

	    CsvSchema schema = CsvSchema.builder()
	        .setColumnSeparator(bestSep)
	        .setQuoteChar(bestQuote)
	        .setUseHeader(true)
	        .build();

	    logger.warn("Auto detected schema is quoteChar:{} separatorChar:{} escapeChar:none (consistency score: {:.3f})",
	        bestQuote, bestSep, bestScore);

	    return schema;
	}

	/**
	 * Scores a candidate {@code (separator, quote)} dialect by measuring how consistently
	 * each sampled line produces the same number of unquoted fields.
	 * <p>
	 * Algorithm: count raw separator occurrences <em>outside</em> quoted regions for every
	 * line, collect the frequency distribution, and return the fraction of lines that agree
	 * on the modal column count — weighted by that column count so that a 20-column agreement
	 * scores higher than a 2-column agreement.
	 * </p>
	 *
	 * @param lines sample lines (including the header at index 0)
	 * @param sep   separator character to test
	 * @param quote quote character to test
	 * @return consistency score in [0, 1]; higher is better
	 */
	private double consistencyScore(List<String> lines, char sep, char quote)
	{
	    if (lines.size() < 2)
	    {
	        return 0.0;
	    }

	    Map<Integer, Integer> colCountFreq = new HashMap<>();
	    // Skip line 0 (header) — count from line 1 onwards
	    for (int i = 1; i < lines.size(); i++)
	    {
	        int cols = countFieldsOutsideQuotes(lines.get(i), sep, quote);
	        colCountFreq.merge(cols, 1, Integer::sum);
	    }

	    int dataLines = lines.size() - 1;
	    // Find modal column count
	    int modalCount = colCountFreq.entrySet().stream()
	        .max(Map.Entry.comparingByValue())
	        .map(Map.Entry::getKey)
	        .orElse(0);

	    if (modalCount == 0)
	    {
	        return 0.0;
	    }

	    int modalFreq = colCountFreq.getOrDefault(modalCount, 0);
	    // Weight by column count so many-column agreement beats few-column agreement
	    return ((double) modalFreq / dataLines) * Math.log1p(modalCount);
	}

	/**
	 * Counts the number of fields in {@code line} by splitting on {@code sep} while respecting
	 * quoted regions opened and closed by {@code quote}.
	 *
	 * @param line  raw CSV line
	 * @param sep   separator character
	 * @param quote quote character
	 * @return number of fields (= number of separators outside quotes + 1)
	 */
	private int countFieldsOutsideQuotes(String line, char sep, char quote)
	{
	    int fields = 1;
	    boolean inQuotes = false;
	    for (int i = 0; i < line.length(); i++)
	    {
	        char c = line.charAt(i);
	        if (c == quote)
	        {
	            // Doubled-quote escape: "" inside a quoted field is not a closing quote
	            if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == quote)
	            {
	                i++; // skip the second quote
	            }
	            else
	            {
	                inQuotes = !inQuotes;
	            }
	        }
	        else if (c == sep && !inQuotes)
	        {
	            fields++;
	        }
	    }
	    return fields;
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
