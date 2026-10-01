package org.open4goods.services.feedservice.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.Normalizer;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.commons.config.yml.datasource.CsvDataSourceProperties;
import org.open4goods.commons.config.yml.datasource.DataSourceProperties;
import org.open4goods.commons.helper.InStockParser;
import org.open4goods.commons.helper.ProductConditionParser;
import org.open4goods.commons.helper.ResourceHelper;
import org.open4goods.commons.helper.ShippingCostParser;
import org.open4goods.commons.helper.ShippingTimeParser;
import org.open4goods.commons.helper.StockQuantityParser;
import org.open4goods.model.attribute.ReferentielKey;
import org.open4goods.model.datafragment.DataFragment;
import org.open4goods.model.exceptions.ValidationException;
import org.open4goods.model.helper.IdHelper;
import org.open4goods.model.price.Price;
import org.open4goods.model.product.InStock;
import org.open4goods.model.product.ProductCondition;
import org.open4goods.model.rating.Rating;
import org.open4goods.model.resource.Resource;
import org.open4goods.services.feedservice.definition.ColumnResolution;
import org.open4goods.services.feedservice.definition.FeedColumnResolver;
import org.open4goods.services.feedservice.definition.FeedDefinition;
import org.open4goods.services.feedservice.definition.FeedDefinitionFactory;
import org.open4goods.services.feedservice.definition.MissingRequiredColumnsException;
import org.open4goods.services.feedservice.model.FeedIndexingJobStat;
import org.open4goods.services.remotefilecaching.service.RemoteFileCachingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import java.util.concurrent.ConcurrentHashMap;

import tools.jackson.databind.MappingIterator;
import tools.jackson.databind.ObjectReader;
import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.CsvReadFeature;
import tools.jackson.dataformat.csv.CsvSchema;


/**
 * Worker thread that continuously dequeues {@link DataSourceProperties} entries from
 * {@link FeedIndexingService#getQueue()}, downloads the remote CSV file, detects its
 * dialect, and indexes each row as a {@link org.open4goods.model.datafragment.DataFragment}.
 *
 * <p>One worker processes one feed at a time. Multiple workers run in parallel, one per
 * configured thread in {@link org.open4goods.services.feedservice.config.FeedIndexingProperties#getConcurrentFetcherTask()}.
 *
 * <p>The lenient {@link #CSV_MAPPER} tolerates trailing columns and missing columns so that a
 * single malformed row does not abort the whole feed. Dialect (separator, quote) is
 * auto-detected by {@link CsvDialectDetector} and may be overridden
 * per-datasource in YAML.
 *
 * @author goulven
 */
public class FeedIndexingWorker implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(FeedIndexingWorker.class);

	/**
	 * Lenient CSV mapper shared across all worker instances (CsvMapper is thread-safe after
	 * construction). IGNORE_TRAILING_UNMAPPABLE silently drops extra columns instead of throwing
	 * CsvReadException, recovering rows that the dialect detector might still mis-parse.
	 * INSERT_NULLS_FOR_MISSING_COLUMNS fills short rows rather than failing.
	 * SKIP_EMPTY_LINES avoids NPE on blank lines at end-of-file.
	 */
	private static final CsvMapper CSV_MAPPER = CsvMapper.builder()
	        .enable(CsvReadFeature.SKIP_EMPTY_LINES)
	        .enable(CsvReadFeature.IGNORE_TRAILING_UNMAPPABLE)
	        .enable(CsvReadFeature.INSERT_NULLS_FOR_MISSING_COLUMNS)
	        .enable(CsvReadFeature.TRIM_SPACES)
	        .build();

	private static final String CLASSPATH_PREFIX = "classpath:";

	/** The service used to "atomically" fetch and store / update DataFragments **/
	private final FeedIndexingService csvService;
	private final DataFragmentIndexer dataFragmentIndexer;

	private final DataFragmentCompletionService completionService;	
	private final RemoteFileCachingService remoteFileCachingService;

	/**
	 * The duration of the worker thread pause when nothing to get from the queue
	 **/
	private final int pauseDuration;

	private String logsFolder;

	/**
	 * State flag
	 */
//	private volatile boolean stop;

	/**
	 * The externaly maintained stats
	 */
	private FeedIndexingJobStat stats;

	/**
	 * Per-worker cache that maps config column-name → actual CSV header key for the current URL.
	 * Cleared between URLs. Accessed only by the owning worker thread — plain HashMap is
	 * sufficient and avoids CAS overhead.
	 */
	private final Map<String, String> resolvedKeysCache = new java.util.HashMap<>();

	/**
	 * Global cache shared across all worker threads: raw CSV header → comparable (lower-case,
	 * accent-stripped, alphanumeric-only) form.
	 */
	private static final Map<String, String> COMPARABLE_HEADER_CACHE = new ConcurrentHashMap<>();

	/** Matches Unicode combining diacritics (used in NFD-based accent stripping). */
	private static final java.util.regex.Pattern DIACRITIC_PATTERN = java.util.regex.Pattern.compile("\\p{M}");

	/** Matches any character that is not a lower-case ASCII letter or digit. */
	private static final java.util.regex.Pattern NON_ALPHANUM_PATTERN = java.util.regex.Pattern.compile("[^a-z0-9]");

	/**
	 * Constructor
	 * 
	 * @param csvService
	 * @param toConsole
	 * @param dequeuePageSize
	 */
	public FeedIndexingWorker(final FeedIndexingService csvService, DataFragmentCompletionService completionService, DataFragmentIndexer dataFragmentIndexer, final int pauseDuration,
			String logsFolder, RemoteFileCachingService remoteFileCachingService) {
		this.csvService = csvService;
		this.pauseDuration = pauseDuration;
		this.completionService = completionService;
		this.dataFragmentIndexer = dataFragmentIndexer;
		this.logsFolder = logsFolder;
		this.remoteFileCachingService = remoteFileCachingService;
	}

	@Override
	public void run() {
		while (true) {
			try {
				if (!csvService.getQueue().isEmpty()) {
					// There is data to consume and queue consummation is enabled

					DataSourceProperties ds = csvService.getQueue().take();

					logger.info("will index {}", ds.getDatasourceConfigName());
					fetch(ds);
					logger.info("indexed {}", ds.getDatasourceConfigName());

				} else {
					try {
						logger.debug("No DataFragments to dequeue. Will sleep {}ms", pauseDuration);
						Thread.sleep(pauseDuration);
					} catch (final InterruptedException e) {
					}
				}
			} catch (final Exception e) {
				logger.error("Error while dequeing DataFragments", e);
			}
		}
	}

	public void fetch(DataSourceProperties dsProperties) {
		String safeName = IdHelper.azCharAndDigitsPointsDash(dsProperties.getName()).toLowerCase();
		Logger dedicatedLogger = csvService.createDatasourceLogger(safeName, dsProperties, logsFolder + "/feeds/");
		String dsConfName = dsProperties.getDatasourceConfigName();
		final CsvDataSourceProperties config = dsProperties.getCsvDatasource();
		Set<String> urls = config.getDatasourceUrls();
		dedicatedLogger.warn("STARTING FEED INDEXING OF {} - {} - urls={}", feedLogToken(dsProperties, urls), dsProperties, urls);

		if (urls.size() == 0) {
			// Triggering healthcheck down in the CsvService
			csvService.incrementFeedNoUrls();
			logger.error("No url's defined for datasource {}",dsProperties.getDatasourceConfigName());
			dedicatedLogger.error("No url's defined for datasource {}",dsProperties.getDatasourceConfigName());

		}

		FeedDefinition feedDefinition;
		try {
			feedDefinition = FeedDefinitionFactory.from(dsProperties);
		} catch (MissingRequiredColumnsException e) {
			// AC2 : no column is ever guessed by label. A feed whose config declares neither an
			// explicit url nor an explicit price column is not indexed, it is reported instead.
			csvService.incrementFeedMissingRequiredColumns();
			logger.error("Feed {} has no explicit url/price column mapping declared; skipping indexing: {}", dsConfName, e.getMessage());
			dedicatedLogger.error("Feed {} has no explicit url/price column mapping declared; skipping indexing: {}", dsConfName, e.getMessage());
			return;
		}

		for (String url : urls) {
			resolvedKeysCache.clear();
			dedicatedLogger.warn("STARTING FEED URL {} - {}", url, dsProperties);
			// Updating status with actual feed
			stats = new FeedIndexingJobStat(dsProperties.getDatasourceConfigName(), url, FeedIndexingJobStat.TYPE_CSV);
			// Resolved once from the first header row of this URL, reused for every data row (AC2/AC7).
			ColumnResolution columnResolution = null;

			int okItems = 0;
			int validationFailedItems = 0;
			int errorItems = 0;
			int excludedItems = 0;

			MappingIterator<Map<String, String>> mi = null;
			File destFile = null;
			try {
				try {
					destFile = remoteFileCachingService.downloadToTmpFile(url, safeName);
					if (destFile ==null || !destFile.exists() || destFile.length() == 0) {
						dedicatedLogger.error("Non existing or empty downloaded file : {}",url);
						continue;
					}
				} catch (Exception e) {
					dedicatedLogger.error("Exception while downloading feed  {}",url, e);
					continue;
				}

				if (config.getGzip()) {
					destFile = remoteFileCachingService.decompressGzipAndDeleteSource(destFile);
				} else if (config.getZiped()) {
					destFile = remoteFileCachingService.unzipFileAndDeleteSource(destFile);
				}

				CsvSchema schema = configureCsvSchema(config, destFile, dedicatedLogger);
				Charset charset = Charset.forName(config.getCsvEncoding() != null ? config.getCsvEncoding() : "UTF-8");
				ObjectReader oReader = CSV_MAPPER.readerFor(Map.class).with(schema);

				mi = oReader.readValues(new InputStreamReader(new FileInputStream(destFile), charset));

				while (true) {
					Map<String, String> line = null;
					try {

						// NOTE : can raise exception if further line is invalid
						boolean hasNext = mi.hasNext();
						if (!hasNext) {
							break;
						}

						line = mi.next();
						// Count only after a successful read so stats match actual rows attempted
						stats.incrementLines();
						if (line == null) {
							throw new ValidationException("Null line encountered");
						}

						// Normalise cell values; guard against null values (sparse rows from INSERT_NULLS_FOR_MISSING_COLUMNS)
						 line = line.entrySet().stream()
							    .collect(Collectors.toMap(
							        e -> e.getKey(),
							        e -> e.getValue() == null ? "" : normalizeCsvValue(e.getValue())
							    ));

						if (columnResolution == null) {
							// Resolved once per header row, not per data row (an Awin feed has hundreds of
							// thousands of lines) : AC2 reports unknown columns, it never guesses from them.
							columnResolution = FeedColumnResolver.resolve(feedDefinition, line.keySet());
							if (!columnResolution.unknownColumns().isEmpty()) {
								dedicatedLogger.warn("Unknown columns reported for {} ({}): {}", dsConfName, url, columnResolution.unknownColumns());
								logger.warn("Unknown columns reported for {} ({}): {}", dsConfName, url, columnResolution.unknownColumns());
								stats.addUnknownColumns(columnResolution.unknownColumns());
							}
							if (!columnResolution.missingUnitColumns().isEmpty()) {
								dedicatedLogger.warn("Missing unit columns for {} ({}): {}", dsConfName, url, columnResolution.missingUnitColumns());
							}
						}

						DataFragment df = parseCsvLine(dsProperties, line, dsConfName, dedicatedLogger, url);

						// Store the feedUrl as an attribute (for debug)
						// TODO(p3,conf) : from conf
						df.addAttribute("feed_url", url, "fr", null);

						dataFragmentIndexer.index(df, dsConfName);
						stats.incrementIndexed();
						okItems++;
						
					} catch (ValidationException e) {
						stats.incrementValidationFail();
						validationFailedItems++;
						dedicatedLogger.info("Validation exception ({}) while parsing {} (url={})", e.getMessage(), dataFragment(line), url);
					} catch (Exception e) {
						stats.incrementErrors();
						errorItems++;
						dedicatedLogger.warn("Error in {}, while parsing {} ({} cols)", dsConfName, url, line == null ? 0 : line.size(), e);
					}
				}

				dedicatedLogger.info("Removing fetched CSV file at {}", destFile);

			} catch (Exception e) {
				// Triggering healthcheck down in the CsvService
				csvService.brokenCsv(url);
				logger.error("Critical exception while parsing CSV file: {} : {}", dsConfName, url, e);
				dedicatedLogger.error("Critical exception while parsing CSV file:  : {} : {}", dsConfName, url, e);
				stats.setFail(true);
				
			} finally {
				// Saving the feed state specifically
				stats.terminate();
				csvService.finished(stats, dsProperties);
				closeIterator(mi, dedicatedLogger);
				deleteTemporaryFile(url, destFile, dedicatedLogger);
			}

			dedicatedLogger.info("Done: {} (imported: {}, errors: {}, not_validable: {}, excluded: {}) - {}", dsConfName, okItems, errorItems, validationFailedItems, excludedItems, url);
		}
		dedicatedLogger.info("End CSV feed indexing for {}", dsConfName);
	}

	

	/**
	 * Builds a {@link CsvSchema} for {@code destFile} by running dialect auto-detection and then
	 * applying any explicit overrides from the datasource YAML configuration.
	 * <p>
	 * Explicit YAML overrides always win over auto-detection, which is useful for feeds whose
	 * content would confuse the heuristics (e.g. very short files or unusual encodings).
	 * </p>
	 *
	 * @param config       datasource CSV configuration (may contain explicit separator/quote)
	 * @param destFile     the local CSV file to analyse
	 * @param dedicatedLogger feed-specific logger
	 * @return configured {@link CsvSchema}
	 * @throws IOException if the file cannot be read during detection
	 */
	private CsvSchema configureCsvSchema(CsvDataSourceProperties config, File destFile, Logger dedicatedLogger) throws IOException
	{
	    Charset charset = Charset.forName(config.getCsvEncoding() != null ? config.getCsvEncoding() : "UTF-8");
	    dedicatedLogger.info("Detecting schema for {} (charset {})", destFile.getAbsolutePath(), charset);

	    CsvSchema schema = csvService.detectSchema(destFile, charset);

	    schema = applyCsvSchemaOverrides(schema, config);

	    dedicatedLogger.warn("Final schema: quoteChar:{} separatorChar:{} escapeChar:{}",
	        schema.getQuoteChar() == -1 ? "none" : Character.toString((char) schema.getQuoteChar()),
	        schema.getColumnSeparator() == -1 ? "none" : Character.toString((char) schema.getColumnSeparator()),
	        schema.getEscapeChar() == -1 ? "none" : Character.toString((char) schema.getEscapeChar()));

	    return schema;
	}

	static CsvSchema applyCsvSchemaOverrides(CsvSchema schema, CsvDataSourceProperties config)
	{
	    if (config.getCsvQuoteChar() != null)
	    {
	        schema = schema.withQuoteChar(config.getCsvQuoteChar());
	    }
	    if (config.getCsvEscapeChar() != null)
	    {
	        schema = schema.withEscapeChar(config.getCsvEscapeChar());
	    }
	    if (config.getCsvSeparator() != null)
	    {
	        schema = schema.withColumnSeparator(config.getCsvSeparator());
	    }
	    return schema;
	}

	private String normalizeCsvValue(String value)
	{
	    String normalized = IdHelper.sanitizeAndNormalize(value);
	    if (normalized.length() >= 2 && normalized.charAt(0) == '"' && normalized.charAt(normalized.length() - 1) == '"')
	    {
	        return normalized.substring(1, normalized.length() - 1);
	    }
	    return normalized;
	}

	private void closeIterator(MappingIterator<Map<String, String>> mi, Logger logger) {
		if (mi != null) {
			mi.close();
		}
	}

	private void deleteTemporaryFile(String url, File destFile, Logger logger) {
		// We delete only downloaded files, to preserve the local files (used for debug)
		if (url.startsWith("http") && destFile != null && destFile.exists()) {
			try {
				Files.delete(destFile.toPath());
			} catch (IOException e) {
				logger.error("Error while deleting temporary file", e);
			}
		}
	}
	
	/**
	 * Parses a CSV line into a DataFragment object.
	 *
	 * @param config DataSourceProperties for configuration settings
	 * @param item Map representing a CSV line
	 * @param datasourceConfigName Name of the data source configuration
	 * @param dedicatedLogger Logger for logging information
	 * @param datasetUrl URL of the dataset being parsed
	 * @return DataFragment object containing parsed data
	 * @throws ValidationException if validation fails
	 */
	private DataFragment parseCsvLine(final DataSourceProperties config, final Map<String, String> item, final String datasourceConfigName, final Logger dedicatedLogger, String datasetUrl) throws ValidationException {
	    dedicatedLogger.debug("Parsing line : {}", item);
	    
	    DataFragment dataFragment = new DataFragment();
	    dataFragment.setFragmentHashCode(item.hashCode());
	    
	    setAffiliatedUrl(dataFragment, item, config, dedicatedLogger);
	    setUrl(dataFragment, item, config, dedicatedLogger, datasetUrl);
	    trimUrlParameters(dataFragment, config);
	    setPrice(dataFragment, item, config, dedicatedLogger);
	    setNameAndTags(dataFragment, item, config);
	    setAttributes(dataFragment, item, config, dedicatedLogger);
	    setRating(dataFragment, item, config, dedicatedLogger);
	    setDescription(dataFragment, item, config);
	    addResources(dataFragment, item, config, dedicatedLogger);
	    boolean explicitStockAvailability = setInStock(dataFragment, item, config, dedicatedLogger);
	    setShippingDetails(dataFragment, item, config, dedicatedLogger, explicitStockAvailability);
	    setProductState(dataFragment, item, config, dedicatedLogger);
	    addReferentielAttributes(dataFragment, item, config, dedicatedLogger);
	    addExternalIds(dataFragment, item, config);
	    
	    importAllAttributes(dataFragment, item, config);
	    completionService.complete(dataFragment, datasourceConfigName, config, dedicatedLogger);
	    enforceAffiliatedUrl(dataFragment);
	    dataFragment.validate(config.getValidationFields());
	    
	    return dataFragment;
	}

	/**
	 * Sets the URL of the DataFragment.
	 *
	 * @param dataFragment DataFragment to set the URL
	 * @param item Map representing a CSV line
	 * @param config DataSourceProperties for configuration settings
	 * @param logger Logger for logging information
	 * @param datasetUrl URL of the dataset being parsed
	 */
	private void setUrl(DataFragment dataFragment, Map<String, String> item, DataSourceProperties config, Logger logger, String datasetUrl) {
	    try {
	        CsvDataSourceProperties csvProperties = config.getCsvDatasource();
	        String url = !StringUtils.isEmpty(csvProperties.getExtractUrlFromParam()) ? extractUrlFromParam(item, csvProperties) : null;
	        if (StringUtils.isEmpty(url)) {
	            url = getFromCsvRow(item, csvProperties.getUrl());
	        }
	        dataFragment.setUrl(url);
	        // revove from the source to prevent further integration
	        removeFromSource(item, csvProperties.getUrl());
	        
	    } catch (Exception e) {
	        logger.info("Error while extracting url in dataset {} ({} columns)", datasetUrl, item.size());
	    }
	}


	/**
	 * Extracts a URL from a query-string parameter embedded in the value of {@code csvProperties.url}.
	 * <p>
	 * Example: if the CSV cell contains {@code https://tracker.example.com?target=https%3A%2F%2Fshop.com%2Fproduct}
	 * and {@code extractUrlFromParam = "target"}, returns the decoded {@code https://shop.com/product}.
	 * </p>
	 *
	 * @param item          CSV row
	 * @param csvProperties datasource configuration carrying the column name and param key
	 * @return decoded URL, or {@code null} if the param is absent from the cell value
	 */
	private String extractUrlFromParam(Map<String, String> item, CsvDataSourceProperties csvProperties)
	{
	    String raw = getFromCsvRow(item, csvProperties.getUrl());
	    if (StringUtils.isEmpty(raw))
	    {
	        return null;
	    }
	    UriComponents parsedUrl = UriComponentsBuilder.fromUriString(raw).build();
	    String paramValue = parsedUrl.getQueryParams().getFirst(csvProperties.getExtractUrlFromParam());
	    if (paramValue == null)
	    {
	        logger.debug("Query param '{}' not found in URL cell '{}'", csvProperties.getExtractUrlFromParam(), raw);
	        return null;
	    }
	    return URLDecoder.decode(paramValue, StandardCharsets.UTF_8);
	}

	/**
	 * Sets the affiliated URL of the DataFragment.
	 *
	 * @param dataFragment DataFragment to set the affiliated URL
	 * @param item Map representing a CSV line
	 * @param config DataSourceProperties for configuration settings
	 * @param logger Logger for logging information
	 */
	private void setAffiliatedUrl(DataFragment dataFragment, Map<String, String> item, DataSourceProperties config, Logger logger) {
	    CsvDataSourceProperties csvProperties = config.getCsvDatasource();
	    if (!StringUtils.isEmpty(csvProperties.getAffiliatedUrl())) {
	        String url = getFromCsvRow(item, csvProperties.getAffiliatedUrl());
	        if (url != null && csvProperties.getAffiliatedUrlReplacementTokens() != null) {
	            for (Map.Entry<String, String> token : csvProperties.getAffiliatedUrlReplacementTokens().entrySet()) {
	                url = url.replace(token.getKey(), token.getValue());
	            }
	        }
	        dataFragment.setAffiliatedUrl(url);
	    }
	}

	/**
	 * Trims URL parameters from the DataFragment's URL.
	 *
	 * @param dataFragment DataFragment to trim the URL
	 * @param config DataSourceProperties for configuration settings
	 */
	private void trimUrlParameters(DataFragment dataFragment, DataSourceProperties config) {
	    CsvDataSourceProperties csvProperties = config.getCsvDatasource();
	    if (csvProperties.getTrimUrlParameters()) {
	        int pos = dataFragment.getUrl().indexOf('?');
	        if (pos != -1) {
	            dataFragment.setUrl(dataFragment.getUrl().substring(0, pos));
	        }
	    }
	}

	/**
	 * Sets the price of the DataFragment.
	 *
	 * @param dataFragment DataFragment to set the price
	 * @param item Map representing a CSV line
	 * @param config DataSourceProperties for configuration settings
	 * @param logger Logger for logging information
	 */
	private void setPrice(DataFragment dataFragment, Map<String, String> item, DataSourceProperties config, Logger logger) {
	    CsvDataSourceProperties csvProperties = config.getCsvDatasource();
	    Set<String> configuredPriceColumns = csvProperties.getPrice();
	    if (configuredPriceColumns != null) {
	        trySetPriceFromColumns(dataFragment, item, configuredPriceColumns, config, logger);
	    }
	    if (null == dataFragment.getPrice() || null == dataFragment.getPrice().getPrice()) {
	        logger.warn("No price extracted for row with {} columns; configured price columns {}; available row values {}",
	                item.size(),
	                csvProperties.getPrice(),
	                item);
	    }
	}

	/**
	 * Tries to set the DataFragment price from the first non-empty matching CSV column.
	 *
	 * @param dataFragment DataFragment to set the price
	 * @param item Map representing a CSV line
	 * @param priceColumns candidate column names containing the price
	 * @param config DataSourceProperties for configuration settings
	 * @param logger Logger for logging information
	 */
	private void trySetPriceFromColumns(DataFragment dataFragment, Map<String, String> item, Iterable<String> priceColumns, DataSourceProperties config, Logger logger) {
	    CsvDataSourceProperties csvProperties = config.getCsvDatasource();
	    for (String priceColumn : priceColumns) {
	        try {
	            Price price = new Price();
	            String value = getFromCsvRow(item, priceColumn);
	            if (!StringUtils.isEmpty(value)) {
	                price.setPriceValue(value, Locale.forLanguageTag(config.getLanguage().toUpperCase()));
	                price.setCurrency(csvProperties.getCurrency());
	                dataFragment.setPrice(price);
	                // Delete from source
	                removeFromSource(item, priceColumn);
	                break;
	            }
	        } catch (Exception e) {
	            handlePriceFallback(dataFragment, item, priceColumn, config, logger);
	        }
	        if (null != dataFragment.getPrice() && null != dataFragment.getPrice().getPrice()) {
	            break;
	        }
	    }
	}

	/**
	 * Handles fallback mechanism for setting the price if the initial attempt fails.
	 *
	 * @param dataFragment DataFragment to set the price
	 * @param item Map representing a CSV line
	 * @param priceColumn Column name containing the price
	 * @param config DataSourceProperties for configuration settings
	 * @param logger Logger for logging information
	 */
	private void handlePriceFallback(DataFragment dataFragment, Map<String, String> item, String priceColumn, DataSourceProperties config, Logger logger) {
	    try {
	        dataFragment.setPriceAndCurrency(getFromCsvRow(item, priceColumn), Locale.forLanguageTag(config.getLanguage().toUpperCase()));
	    } catch (Exception e) {
	        logger.warn("Error setting fallback price with setPriceAndCurrency() from column '{}' at {}", priceColumn, dataFragment.getUrl());
	    }
	}

	/**
	 * Sets the name and tags of the DataFragment.
	 *
	 * @param dataFragment DataFragment to set the name and tags
	 * @param item Map representing a CSV line
	 * @param config DataSourceProperties for configuration settings
	 */
	private void setNameAndTags(DataFragment dataFragment, Map<String, String> item, DataSourceProperties config) {
	    CsvDataSourceProperties csvProperties = config.getCsvDatasource();
	    String nameColumn = csvProperties.getName();
	    dataFragment.addName(getFromCsvRow(item, nameColumn));

        // Delete from source
        removeFromSource(item, nameColumn);
        
	    dataFragment.addProductTags(getCategoryFromCsvRows(item));
	}

	/**
	 * Sets the attributes of the DataFragment.
	 *
	 * @param dataFragment DataFragment to set the attributes
	 * @param item Map representing a CSV line
	 * @param config DataSourceProperties for configuration settings
	 * @param logger Logger for logging information
	 */
	private void setAttributes(DataFragment dataFragment, Map<String, String> item, DataSourceProperties config, Logger logger) {
	    CsvDataSourceProperties csvProperties = config.getCsvDatasource();
	    if (!StringUtils.isEmpty(csvProperties.getAttrs())) {
	        handleAttributes(dataFragment, config, getFromCsvRow(item, csvProperties.getAttrs()), logger);
            // Delete from source
            removeFromSource(item, csvProperties.getAttrs());
	    }
	}

	/**
	 * Sets the rating of the DataFragment.
	 *
	 * @param dataFragment DataFragment to set the rating
	 * @param item Map representing a CSV line
	 * @param config DataSourceProperties for configuration settings
	 * @param logger Logger for logging information
	 */
	private void setRating(DataFragment dataFragment, Map<String, String> item, DataSourceProperties config, Logger logger) {
	    CsvDataSourceProperties csvProperties = config.getCsvDatasource();
	    if (csvProperties.getRating() != null) {
	        try {
	            Rating rating = new Rating();
	            rating.setMin(csvProperties.getRating().getMinValue());
	            rating.setMax(csvProperties.getRating().getMaxValue());
	            rating.addTag(csvProperties.getRating().getType());
	            rating.setValue(Double.valueOf(getFromCsvRow(item, csvProperties.getRating().getValue())));
	            dataFragment.addRating(rating);
	        } catch (Exception e) {
	            logger.warn("Error while adding rating: {}", e.getMessage());
	        }
	    }
	}

	/**
	 * Sets the description of the DataFragment.
	 *
	 * @param dataFragment DataFragment to set the description
	 * @param item Map representing a CSV line
	 * @param config DataSourceProperties for configuration settings
	 */
	private void setDescription(DataFragment dataFragment, Map<String, String> item, DataSourceProperties config) {
	    CsvDataSourceProperties csvProperties = config.getCsvDatasource();
	    for (String descColumn : configuredColumns(csvProperties.getDescription())) {
	        String description = getFromCsvRow(item, descColumn);
	        if (!StringUtils.isEmpty(description) && config.getDescriptionRemoveToken() != null) {
	            for (String token : config.getDescriptionRemoveToken()) {
	                description = description.replace(token, "");
	            }
	        }
	        dataFragment.addDescription(dataFragment.getDatasourceName(), description);
            // Delete from source
            removeFromSource(item, descColumn);
	    }
	}

	/**
	 * Adds resources (e.g., images) to the DataFragment.
	 *
	 * @param dataFragment DataFragment to add resources
	 * @param item Map representing a CSV line
	 * @param config DataSourceProperties for configuration settings
	 * @param logger Logger for logging information
	 */
	private void addResources(DataFragment dataFragment, Map<String, String> item, DataSourceProperties config, Logger logger) {
	    try {
	        CsvDataSourceProperties csvProperties = config.getCsvDatasource();
	        for (String imgCell : configuredColumns(csvProperties.getImage())) {
	            String resource = getFromCsvRow(item, imgCell);
	            if (!StringUtils.isEmpty(resource) && shouldIncludeResource(resource, csvProperties)) {
	                dataFragment.addResource(new Resource(resource));
	            }
	            
	            // Delete from source
                removeFromSource(item, imgCell);
                
	        }
	        
	        // Adding all resources patterns
	        Set<String> toRemove = new HashSet<String>();
	        for (Entry<String, String> attr : item.entrySet()) {
	        	
				if (ResourceHelper.isResource(attr.getValue())) {
					Resource r = new Resource(attr.getValue());
					// TODO (p2, feature) : handle COVERS
					dataFragment.addResource(r);
					toRemove.add( attr.getKey());
				}
	        }
	        toRemove.forEach(e -> {
	        	removeFromSource(item,e);
	        	
	        });
	        
	    } catch (ValidationException e) {
	        logger.warn("Problem while adding resource for row with {} columns", item.size());
	    }
	}

	/**
	 * Determines if a resource should be included based on exclusions.
	 *
	 * @param resource Resource string to check
	 * @param csvProperties CsvDataSourceProperties for configuration settings
	 * @return True if the resource should be included, false otherwise
	 */
	private boolean shouldIncludeResource(String resource, CsvDataSourceProperties csvProperties) {
	    if (csvProperties.getImageTokenExclusions() != null) {
	        for (String exclusion : csvProperties.getImageTokenExclusions()) {
	            if (resource.contains(exclusion)) {
	                return false;
	            }
	        }
	    }
	    return true;
	}

	/**
	 * Sets the stock availability of the DataFragment.
	 *
	 * @param dataFragment DataFragment to set stock availability
	 * @param item Map representing a CSV line
	 * @param config DataSourceProperties for configuration settings
	 * @param logger Logger for logging information
	 */
	private boolean setInStock(DataFragment dataFragment, Map<String, String> item, DataSourceProperties config, Logger logger) {
	    CsvDataSourceProperties csvProperties = config.getCsvDatasource();
	    dataFragment.setInStock(InStock.INSTOCK);
	    for (String inStockColumn : configuredColumns(csvProperties.getInStock())) {
	        String value = getFromCsvRow(item, inStockColumn);
	        if (StringUtils.isEmpty(value)) {
	            continue;
	        }
	        try {
				InStock inStock = InStockParser.parse(value);
				if (inStock != null) {
					dataFragment.setInStock(inStock);
					removeFromSource(item, inStockColumn);
					return true;
				}
	        } catch (Exception e) {
	            logger.info("Cannot parse InStock : {}", e.getMessage());
	        }
	    }
	    return false;
	}

	/**
	 * Sets the shipping details of the DataFragment.
	 *
	 * @param dataFragment DataFragment to set shipping details
	 * @param item Map representing a CSV line
	 * @param config DataSourceProperties for configuration settings
	 * @param logger Logger for logging information
	 */
	private void setShippingDetails(DataFragment dataFragment, Map<String, String> item, DataSourceProperties config, Logger logger, boolean explicitStockAvailability) {
	    CsvDataSourceProperties csvProperties = config.getCsvDatasource();
	    setShippingTime(dataFragment, item, csvProperties, logger);
	    setShippingCost(dataFragment, item, csvProperties, logger);
	    setQuantityInStock(dataFragment, item, csvProperties, logger, explicitStockAvailability);
	    setWarranty(dataFragment, item, csvProperties, logger);
	}

	/**
	 * Sets the shipping time of the DataFragment.
	 *
	 * @param dataFragment DataFragment to set shipping time
	 * @param item Map representing a CSV line
	 * @param csvProperties CsvDataSourceProperties for configuration settings
	 * @param logger Logger for logging information
	 */
	private void setShippingTime(DataFragment dataFragment, Map<String, String> item, CsvDataSourceProperties csvProperties, Logger logger) {
	    String shippingTimeColumn = csvProperties.getShippingTime();
	    if (StringUtils.isEmpty(shippingTimeColumn)) {
	        return;
	    }
	    String shippingTimeStr = getFromCsvRow(item, shippingTimeColumn);
	    if (StringUtils.isEmpty(shippingTimeStr)) {
	        return;
	    }
	    try {
	        Integer shippingTime = ShippingTimeParser.parse(shippingTimeStr);
	        if (shippingTime != null) {
	            dataFragment.setShippingTime(shippingTime);
	            removeFromSource(item, shippingTimeColumn);
	        }
	    } catch (Exception e) {
	        logger.info("Cannot parse shippingTime : {}", e.getMessage());
	    }
	}

	/**
	 * Sets the quantity in stock of the DataFragment.
	 *
	 * @param dataFragment DataFragment to set quantity in stock
	 * @param item Map representing a CSV line
	 * @param csvProperties CsvDataSourceProperties for configuration settings
	 * @param logger Logger for logging information
	 */
	private void setQuantityInStock(DataFragment dataFragment, Map<String, String> item, CsvDataSourceProperties csvProperties, Logger logger, boolean explicitStockAvailability) {
	    String quantityColumn = csvProperties.getQuantityInStock();
	    if (StringUtils.isEmpty(quantityColumn)) {
	        return;
	    }
	    String quantityStr = getFromCsvRow(item, quantityColumn);
	    if (StringUtils.isEmpty(quantityStr)) {
	        return;
	    }
	    try {
	        Integer quantity = StockQuantityParser.parse(quantityStr);
	        dataFragment.setQuantityInStock(quantity);
	        if (!explicitStockAvailability && quantity == 0) {
	            dataFragment.setInStock(InStock.OUTOFSTOCK);
	        }
	        // Delete from source
            removeFromSource(item, quantityColumn);
	    } catch (Exception e) {
	        logger.info("Cannot parse QuantityInStock : {}", e.getMessage());
	    }
	}

	/**
	 * Sets the shipping cost of the DataFragment.
	 *
	 * @param dataFragment DataFragment to set shipping cost
	 * @param item Map representing a CSV line
	 * @param csvProperties CsvDataSourceProperties for configuration settings
	 * @param logger Logger for logging information
	 */
	private void setShippingCost(DataFragment dataFragment, Map<String, String> item, CsvDataSourceProperties csvProperties, Logger logger) {
	    String shippingCostColumn = csvProperties.getShippingCost();
	    if (StringUtils.isEmpty(shippingCostColumn)) {
	        return;
	    }
	    String costStr = getFromCsvRow(item, shippingCostColumn);
	    if (StringUtils.isEmpty(costStr)) {
	        return;
	    }
	    try {
	        dataFragment.setShippingCost(ShippingCostParser.parse(costStr));
	        // Delete from source
            removeFromSource(item, shippingCostColumn);
	    } catch (Exception e) {
	        logger.info("Cannot parse ShippingCost : {}", e.getMessage());
	    }
	}

	/**
	 * Sets the warranty of the DataFragment.
	 *
	 * @param dataFragment DataFragment to set warranty
	 * @param item Map representing a CSV line
	 * @param csvProperties CsvDataSourceProperties for configuration settings
	 * @param logger Logger for logging information
	 */
	private void setWarranty(DataFragment dataFragment, Map<String, String> item, CsvDataSourceProperties csvProperties, Logger logger) {
	    if (!StringUtils.isEmpty(csvProperties.getWarranty())) {
	        String warrantyStr = getFromCsvRow(item, csvProperties.getWarranty());
	        try {
	            dataFragment.setWarranty(Integer.valueOf(warrantyStr));
                // Delete from source
                removeFromSource(item, csvProperties.getWarranty());
	        } catch (Exception e) {
	            logger.info("Cannot parse Warranty : {}", e.getMessage());
	        }
	    }
	}

	/**
	 * Sets the product state of the DataFragment.
	 *
	 * @param dataFragment DataFragment to set product state
	 * @param item Map representing a CSV line
	 * @param config DataSourceProperties for configuration settings
	 * @param logger Logger for logging information
	 */
	private void setProductState(DataFragment dataFragment, Map<String, String> item, DataSourceProperties config, Logger logger) {
	    CsvDataSourceProperties csvProperties = config.getCsvDatasource();
	    dataFragment.setProductState(config.getDefaultItemCondition());
	    for (String productStateColumn : configuredColumns(csvProperties.getProductState())) {
	        String value = getFromCsvRow(item, productStateColumn);
	        if (StringUtils.isEmpty(value)) {
	            continue;
	        }
	        try {
	            ProductCondition productState = ProductConditionParser.parse(value);
	            if (productState != null) {
	                dataFragment.setProductState(productState);
	                // Delete from source
	                removeFromSource(item, productStateColumn);
	                return;
	            }
	        } catch (Exception e) {
	            logger.info("Cannot parse product state : {}", e.getMessage());
	        }
	    }
	}

	/**
	 * Adds referentiel attributes to the DataFragment.
	 *
	 * @param dataFragment DataFragment to add referentiel attributes
	 * @param item Map representing a CSV line
	 * @param config DataSourceProperties for configuration settings
	 * @param logger Logger for logging information
	 */
	private void addReferentielAttributes(DataFragment dataFragment, Map<String, String> item, DataSourceProperties config, Logger logger) {
	    CsvDataSourceProperties csvProperties = config.getCsvDatasource();
	    for (Map.Entry<ReferentielKey, Set<String>> entry : csvProperties.getReferentiel().entrySet()) {
	        for (String key : entry.getValue()) {
	            String value = getFromCsvRow(item, key);	            
	            if (!StringUtils.isEmpty(value)) {
	                dataFragment.addReferentielAttribute(entry.getKey(), value);
	                // Delete from source
	                removeFromSource(item, key);
	                continue;
	            }
	        }
	    }
	}

	/**
	 * Adds structured external identifiers to the DataFragment.
	 *
	 * @param dataFragment DataFragment to add external ids
	 * @param item Map representing a CSV line
	 * @param config DataSourceProperties for configuration settings
	 */
	private void addExternalIds(DataFragment dataFragment, Map<String, String> item, DataSourceProperties config) {
	    CsvDataSourceProperties csvProperties = config.getCsvDatasource();
	    for (String key : configuredColumns(csvProperties.getMpn())) {
	        String value = getFromCsvRow(item, key);
	        if (!StringUtils.isEmpty(value)) {
	            dataFragment.getExternalIds().getMpn().add(value);
	            removeFromSource(item, key);
	        }
	    }
	    for (String key : configuredColumns(csvProperties.getSku())) {
	        String value = getFromCsvRow(item, key);
	        if (!StringUtils.isEmpty(value)) {
	            dataFragment.getExternalIds().getSku().add(value);
	            removeFromSource(item, key);
	        }
	    }
	}

	/**
	 * Enforces the use of the affiliated URL if it is set.
	 *
	 * @param dataFragment DataFragment to enforce affiliated URL
	 */
	private void enforceAffiliatedUrl(DataFragment dataFragment) {
	    if (!StringUtils.isEmpty(dataFragment.getAffiliatedUrl())) {
	        dataFragment.setUrl(dataFragment.getAffiliatedUrl());
	    }
	    
	    if (StringUtils.isEmpty(dataFragment.getAffiliatedUrl())) {
	    	dataFragment.setAffiliatedUrl(dataFragment.getUrl());
	    }
	}

	/**
	 * Imports all attributes from the CSV row into the DataFragment.
	 *
	 * @param dataFragment DataFragment to import attributes
	 * @param item Map representing a CSV line
	 * @param config DataSourceProperties for configuration settings
	 */
	private void importAllAttributes(DataFragment dataFragment, Map<String, String> item, DataSourceProperties config) {
	    CsvDataSourceProperties csvProperties = config.getCsvDatasource();
	    if (csvProperties.getImportAllAttributes()) {
	        for (Map.Entry<String, String> entry : item.entrySet()) {
	            dataFragment.addAttribute(entry.getKey(), entry.getValue(), config.getLanguage(), null);
	        }
	    }
	}
	/**
	 * Returns a safe log token for a CSV row: column count and key names only, never values.
	 * Avoids leaking affiliate API keys or PII that may appear in cell values.
	 */
	private static String dataFragment(Map<String, String> row)
	{
	    return row == null ? "null" : row.size() + " cols: " + row.keySet();
	}

	/**
	 * Remove an attribute from the source jackson csv map,
	 * @param item
	 * @param url
	 */
	private void removeFromSource(Map<String, String> item, String key) {
		if (item == null || key == null) {
			return;
		}
		String actualKey = actualCsvKey(item, key);
		if (actualKey != null) {
			item.remove(actualKey);
		}
		
	}

	private void removeFromSource(Map<String, String> item, Iterable<String> keys) {
		if (keys == null) {
			return;
		}
		for (String key : keys) {
			removeFromSource(item, key);
		}
	}
	
	
	
	
	
	
	
	



	private void handleAttributes(final DataFragment pd, final DataSourceProperties config, final String attrRaw, Logger dedicatedLogger) {

		if (StringUtils.isEmpty(attrRaw)) {
			return;
		}

		final String[] lines = attrRaw.split(config.getCsvDatasource().getAttributesSplitChar());

		for (final String line : lines) {

			// Limit to 2 so that values containing the separator (e.g. URLs with "://") are preserved intact.
			final String[] frags = line.split(config.getCsvDatasource().getAttributesKeyValSplitChar(), 2);
			if (frags.length != 2) {
				dedicatedLogger.info("Was expecting two fragments, got {} : {} at {}", frags.length, line, config.getName());
			} else {
				String key = frags[0];
				String value = frags[1];
				if (null != config.getCsvDatasource().getAttributesKeyKeepAfter()) {

					int pos = key.indexOf(config.getCsvDatasource().getAttributesKeyKeepAfter());
					if (-1 != pos) {
						key = key.substring(pos + 1).trim();
					}
				}

				// Splitters from conf
				pd.addAttribute(key, value, config.getLanguage(), null);

				// If the value is a comma-separated list of URLs (e.g. imageURL_large), add each as a resource.
				if (value.startsWith("http://") || value.startsWith("https://")) {
					for (String url : value.split(",")) {
						String trimmed = url.trim();
						if (!trimmed.isEmpty()) {
							try {
								pd.addResource(trimmed);
							} catch (Exception ignored) {
								dedicatedLogger.debug("Skipping invalid resource URL from attrs: {}", trimmed);
							}
						}
					}
				}
			}
		}

	}

	/**
	 * Get a value from a named csv row
	 *
	 * @param item
	 * @param colName
	 * @return
	 */
	/**
	 * Returns the columns explicitly configured for a field, never a guessed default (AC2).
	 */
	private static List<String> configuredColumns(Set<String> configured) {
	    return configured == null ? List.of() : List.copyOf(configured);
	}

	private String getFromCsvRow(final Map<String, String> item, final String colName) {
		String actualKey = actualCsvKey(item, colName);
		return actualKey == null ? null : item.get(actualKey);
		
	}

	private String getFromCsvRow(final Map<String, String> item, final Iterable<String> colNames) {
		if (colNames == null) {
			return null;
		}
		for (String colName : colNames) {
			String value = getFromCsvRow(item, colName);
			if (!StringUtils.isEmpty(value)) {
				return value;
			}
		}
		return null;
	}

	private String actualCsvKey(final Map<String, String> item, final String colName)
	{
		if (item == null || colName == null)
		{
			return null;
		}
		if (item.containsKey(colName))
		{
			return colName;
		}
		if (resolvedKeysCache.containsKey(colName))
		{
			final String cached = resolvedKeysCache.get(colName);
			return "".equals(cached) ? null : cached;
		}

		final String normalized = colName.trim();
		final String comparable = comparableCsvHeader(colName);
		final String trimmedMatch = item.keySet().stream()
				.filter(key -> key != null && key.trim().equalsIgnoreCase(normalized))
				.findFirst()
				.orElse(null);
		if (trimmedMatch != null)
		{
			resolvedKeysCache.put(colName, trimmedMatch);
			return trimmedMatch;
		}
		final String comparableMatch = item.keySet().stream()
				.filter(key -> comparableCsvHeader(key).equals(comparable))
				.findFirst()
				.orElse(null);

		resolvedKeysCache.put(colName, comparableMatch != null ? comparableMatch : "");
		return comparableMatch;
	}

	static String comparableCsvHeader(String value)
	{
		if (value == null)
		{
			return "";
		}
		return COMPARABLE_HEADER_CACHE.computeIfAbsent(value, val -> {
			String normalized = IdHelper.sanitizeAndNormalize(val);
			if (!normalized.isEmpty() && normalized.charAt(0) == '\ufeff')
			{
				normalized = normalized.substring(1);
			}
			normalized = stripWrappingHeaderQuotes(normalized);
			// Use pre-compiled patterns to avoid Pattern.compile on every cache-miss invocation.
			normalized = DIACRITIC_PATTERN.matcher(
					Normalizer.normalize(normalized, Normalizer.Form.NFD))
					.replaceAll("")
					.toLowerCase(Locale.ROOT);
			return NON_ALPHANUM_PATTERN.matcher(normalized).replaceAll("");
		});
	}

	private static String stripWrappingHeaderQuotes(String value) {
		String trimmed = value.trim();
		while (trimmed.length() >= 2
				&& ((trimmed.charAt(0) == '"' && trimmed.charAt(trimmed.length() - 1) == '"')
						|| (trimmed.charAt(0) == '\'' && trimmed.charAt(trimmed.length() - 1) == '\''))) {
			trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
		}
		return trimmed;
	}

	private String feedLogToken(DataSourceProperties dsProperties, Set<String> urls) {
		if (!StringUtils.isEmpty(dsProperties.getFeedKey())) {
			return dsProperties.getFeedKey();
		}
		if (urls != null && urls.size() == 1) {
			return urls.iterator().next();
		}
		return dsProperties.getDatasourceConfigName();
	}

	private List<String> getCategoryFromCsvRows(Map<String, String> item) {
		Set<String> catsColumns = new HashSet<String>(item.keySet());
		
		List<String> catCols = catsColumns.stream()
							.filter(e -> e.toLowerCase().contains("categor"))
							.toList();
		
		List<String> catValues = catCols.stream().sorted()
							.map(e -> item.get(e) )
							.filter(e -> !StringUtils.isEmpty(e))
							.toList();

		catCols.forEach(e -> {
            // Delete from source
            removeFromSource(item, e);
		});
		
		
		return catValues;
	}

	public synchronized FeedIndexingJobStat stats() {
		return stats;
	}

}
