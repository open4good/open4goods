package org.open4goods.crawler.services.fetching;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.commons.config.yml.datasource.CsvDataSourceProperties;
import org.open4goods.commons.config.yml.datasource.DataSourceProperties;
import org.open4goods.commons.config.yml.datasource.HtmlDataSourceProperties;
import org.open4goods.commons.exceptions.ValidationException;
import org.open4goods.commons.helper.IdHelper;
import org.open4goods.commons.helper.InStockParser;
import org.open4goods.commons.helper.ProductConditionParser;
import org.open4goods.commons.helper.ShippingCostParser;
import org.open4goods.commons.helper.ShippingTimeParser;
import org.open4goods.commons.model.constants.InStock;
import org.open4goods.commons.model.constants.ProductCondition;
import org.open4goods.commons.model.constants.ReferentielKey;
import org.open4goods.commons.model.crawlers.IndexationJobStat;
import org.open4goods.commons.model.data.DataFragment;
import org.open4goods.commons.model.data.Price;
import org.open4goods.commons.model.data.Rating;
import org.open4goods.commons.services.RemoteFileCachingService;
import org.open4goods.crawler.repository.IndexationRepository;
import org.open4goods.crawler.services.DataFragmentCompletionService;
import org.open4goods.crawler.services.IndexationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.google.common.collect.Sets;

import edu.uci.ics.crawler4j.crawler.CrawlController;

/**
 * Worker thread that asynchronously dequeue the DataFragments from the file
 * queue. It
 * 
 * @author goulven
 *
 */
public class CsvIndexationWorker implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(CsvIndexationWorker.class);

//	private final static ObjectMapper csvMapper = new CsvMapper().enable((CsvParser.Feature.IGNORE_TRAILING_UNMAPPABLE));

	private final static ObjectMapper csvMapper = new CsvMapper();

	private static final String CLASSPATH_PREFIX = "classpath:";

	/** The service used to "atomically" fetch and store / update DataFragments **/
	private final CsvDatasourceFetchingService csvService;

	private WebDatasourceFetchingService webFetchingService;

	private final IndexationService indexationService;

	private final DataFragmentCompletionService completionService;

	private final IndexationRepository csvIndexationRepository;
	
	private final RemoteFileCachingService remoteFileCachingService;

	/**
	 * The duration of the worker thread pause when nothing to get from the queue
	 **/
	private final int pauseDuration;

	private String logsFolder;

	/**
	 * State flag
	 */
	private volatile boolean stop;

	/**
	 * The externaly maintained stats
	 */
	private IndexationJobStat stats;

	/**
	 * Constructor
	 * 
	 * @param csvService
	 * @param toConsole
	 * @param dequeuePageSize
	 */
	public CsvIndexationWorker(final CsvDatasourceFetchingService csvService, DataFragmentCompletionService completionService, IndexationService indexationService, WebDatasourceFetchingService webFetchingService, IndexationRepository csvIndexationRepository, final int pauseDuration, 
			String logsFolder, RemoteFileCachingService remoteFileCachingService) {
		this.csvService = csvService;
		this.pauseDuration = pauseDuration;
		this.completionService = completionService;
		this.indexationService = indexationService;
		this.webFetchingService = webFetchingService;
		this.csvIndexationRepository = csvIndexationRepository;
		this.logsFolder = logsFolder;
		this.remoteFileCachingService = remoteFileCachingService;
	}

	@Override
	public void run() {
		while (!stop) {
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

		dedicatedLogger.info("STARTING CRAWL OF {}", dsProperties);

		final HtmlDataSourceProperties crawlConfig = dsProperties.getCsvDatasource().getWebDatasource();
		DataFragmentWebCrawler crawler = null;
		CrawlController controller = null;

		String dsConfName = dsProperties.getDatasourceConfigName();
		if (crawlConfig != null) {
			try {
				dedicatedLogger.info("Configuring direct web crawler for CSV datasource {}", dsConfName);
				controller = webFetchingService.createCrawlController("csv-" + dsConfName, crawlConfig.getCrawlConfig());
				crawler = webFetchingService.createWebCrawler(dsConfName, dsProperties, crawlConfig);
				crawler.setShouldFollowLinks(false);
			} catch (Exception e) {
				dedicatedLogger.error("Error while starting the CSV-driven web crawler", e);
			}
		}

		final CsvDataSourceProperties config = dsProperties.getCsvDatasource();
		Set<String> urls = config.getDatasourceUrls();

		if (urls.size() == 0) {
			// Triggering healthcheck down in the CsvService
			csvService.incrementFeedNoUrls();
			logger.error("No url's defined for datasource {}",dsProperties.getDatasourceConfigName());
			dedicatedLogger.error("No url's defined for datasource {}",dsProperties.getDatasourceConfigName());
			
		}
		
		for (String url : urls) {
			// Updating status with actual feed
			stats = new IndexationJobStat(dsProperties.getDatasourceConfigName(), url, IndexationJobStat.TYPE_CSV);

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
				ObjectReader oReader = csvMapper.readerFor(Map.class).with(schema);

				mi = oReader.readValues(destFile);

				while (true) {
					stats.incrementLines();
					Map<String, String> line = null;
					try {
						
						// NOTE : can raise exception if further line is invalid
						boolean hasNext = mi.hasNext();
						if (!hasNext) {
							break;
						}
						
						line = mi.next();
						if (line == null) {
							throw new ValidationException("Null line encountered");
						}

						// Normalisation 
						 line = line.entrySet().stream()
							    .collect(Collectors.toMap(
							        e -> e.getKey(), 
							        e -> (String) IdHelper.sanitizeAndNormalize(e.getValue().toString())  
							    ));
						DataFragment df = parseCsvLine(crawler, controller, dsProperties, line, dsConfName, dedicatedLogger, url);

						// Store the feedUrl as an attribute (for debug)
						// TODO(p3,conf) : from conf
						df.addAttribute("feed_url", url, "fr", true, null);

						indexationService.index(df, dsConfName);
						stats.incrementIndexed();
						okItems++;
						
					} catch (ValidationException e) {
						stats.incrementValidationFail();
						validationFailedItems++;
						dedicatedLogger.info("Validation exception ({}) while parsing {}: {}", e.getMessage(), url, line);
					} catch (Exception e) {
						stats.incrementErrors();
						errorItems++;
						dedicatedLogger.warn("Error in {}, while parsing {}", dsConfName, url, e);
					}
				}

				dedicatedLogger.info("Removing fetched CSV file at {}", destFile);

			} catch (Exception e) {
				// Triggering healthcheck down in the CsvService
				csvService.brokenCsv();
				logger.error("Critical exception while parsing CSV file: {} : {}", dsConfName, url, e);
				dedicatedLogger.error("Critical exception while parsing CSV file:  : {} : {}", dsConfName, url, e);
				stats.setFail(true);
			} finally {
				// Saving the feed state specifically
				stats.terminate();
				csvIndexationRepository.save(stats);
				closeIterator(mi, dedicatedLogger);
				deleteTemporaryFile(url, destFile, dedicatedLogger);
			}

			dedicatedLogger.info("Done: {} (imported: {}, errors: {}, not_validable: {}, excluded: {}) - {}", dsConfName, okItems, errorItems, validationFailedItems, excludedItems, url);
		}

		finalizeCrawl(controller, crawler, dsConfName, dsProperties, dedicatedLogger);
	}

	

	/**
	 * Configure the schema for the specified file, and overrides with DatasourceConfig props if any
	 * @param config
	 * @param destFile
	 * @param dedicatedLogger
	 * @return
	 * @throws IOException
	 */
	private CsvSchema configureCsvSchema(CsvDataSourceProperties config, File destFile, Logger dedicatedLogger) throws IOException {
		dedicatedLogger.info("Detecting schema for {}", destFile.getAbsolutePath());
		CsvSchema schema = csvService.detectSchema(destFile);

		// Specific logging in dedicated logger
		dedicatedLogger.warn("Auto detected schema is quoteChar:{} separatorChar:{} escapeChar:{}",  schema.getQuoteChar() == -1 ? "none" : Character.toString(schema.getQuoteChar()), schema.getColumnSeparator() == -1 ? "none" : Character.toString(schema.getColumnSeparator()), schema.getEscapeChar() == -1 ? "none" : Character.toString(schema.getEscapeChar())  );
		
		// Overriding with datasource specific config if defined
		if (config.getCsvQuoteChar() != null) {
			schema = schema.withQuoteChar(config.getCsvQuoteChar().charValue());
		}
		
		if (config.getCsvEscapeChar() != null) {
			schema = schema.withEscapeChar(config.getCsvEscapeChar().charValue());
		}
		
		if (config.getCsvSeparator() != null) {
			schema = schema.withColumnSeparator(config.getCsvSeparator().charValue());
		}
				
		
		return schema;
	}

	private void closeIterator(MappingIterator<Map<String, String>> mi, Logger logger) {
		if (mi != null) {
			try {
				mi.close();
			} catch (IOException e) {
				logger.error("Error while closing CSV iterator", e);
			}
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

	private void finalizeCrawl(CrawlController controller, DataFragmentWebCrawler crawler, String dsConfName, DataSourceProperties dsProperties, Logger dedicatedLogger) {
		IndexationJobStat sObject = csvService.stats().get(dsConfName);
		if (sObject != null) {
			csvService.finished(sObject, dsProperties);
		}

		if (crawler != null && controller != null) {
			dedicatedLogger.info("Terminating the CSV direct crawl controller for {}", dsConfName);
			controller.shutdown();
		}

		dedicatedLogger.info("End CSV direct fetching for {}", dsConfName);
	}

	private DataFragment parseCsvLine(final DataFragmentWebCrawler crawler, final CrawlController controler, final DataSourceProperties config, final Map<String, String> item, final String datasourceConfigName, final Logger dedicatedLogger, String datasetUrl) throws ValidationException {

		final CsvDataSourceProperties csvProperties = config.getCsvDatasource();

		dedicatedLogger.info("Parsing line : {}", item);

		/////////////////////////////////
		// DataFragments mapping
		//////////////////////////////////

		final DataFragment p = new DataFragment();

		// Url extraction from param
		try {
			if (!StringUtils.isEmpty(csvProperties.getExtractUrlFromParam())) {
				String u = getFromCsvRow(item, csvProperties.getUrl());
				final UriComponents parsedUrl = UriComponentsBuilder.fromUriString(u).build();
				u = URLDecoder.decode(parsedUrl.getQueryParams().getFirst(csvProperties.getExtractUrlFromParam()), StandardCharsets.UTF_8);
				p.setUrl(u);
			} else {
				p.setUrl(getFromCsvRow(item, csvProperties.getUrl()));
			}
		} catch (final Exception e2) {
			dedicatedLogger.info("Error while extracting url in dataset {} :  {}", datasetUrl, item);
		}

		if (!StringUtils.isEmpty(csvProperties.getAffiliatedUrl())) {
			String u = getFromCsvRow(item, csvProperties.getAffiliatedUrl());
			if (null == u) {
				dedicatedLogger.info("Null affiliated url in {}", item);
			} else {
				if (null != csvProperties.getAffiliatedUrlReplacementTokens()) {
					for (Entry<String, String> a : csvProperties.getAffiliatedUrlReplacementTokens().entrySet()) {
						u = u.replace(a.getKey(), a.getValue());
					}
				}
				p.setAffiliatedUrl(u);
			}
		}

		if (csvProperties.getTrimUrlParameters()) {

			final int pos = p.getUrl().indexOf('?');
			if (-1 != pos) {
				p.setUrl(p.getUrl().substring(0, pos));
			}
		}

		if (null != csvProperties.getPrice()) {

			for (String pc : csvProperties.getPrice()) {
				try {

					final Price price = new Price();
					String val = getFromCsvRow(item, pc);

					if (StringUtils.isEmpty(val)) {
						continue;
					}

					price.setPriceValue(val, Locale.forLanguageTag(config.getLanguage().toUpperCase()));
					price.setCurrency(csvProperties.getCurrency());

					p.setPrice(price);
					break;
				} catch (final Exception e) {
					dedicatedLogger.info("Error setting price, trying setPriceAndCurrency : {}", p.getUrl());
					try {
						p.setPriceAndCurrency(getFromCsvRow(item, pc), Locale.forLanguageTag(config.getLanguage().toUpperCase()));
					} catch (final Exception e1) {
						dedicatedLogger.warn("Error setting fallback price with setPriceAndCurrency(): {}", p.getUrl());
					}
				}
			}
		}

		p.addName(getFromCsvRow(item, csvProperties.getName()));
		p.addProductTags(getCategoryFromCsvRows(item, csvProperties.getProductTags()));

		if (!StringUtils.isEmpty(csvProperties.getAttrs())) {
			handleAttributes(p, config, getFromCsvRow(item, csvProperties.getAttrs()), dedicatedLogger);
		}

		/////////////////////////////////////
		// Adding all columns as attributes
		/////////////////////////////////////
		if (csvProperties.getImportAllAttributes()) {
			for (Entry<String, String> kv : item.entrySet()) {
				String key = kv.getKey();
				String val =(kv.getValue());

				if (!StringUtils.isEmpty(val)) {
					p.addAttribute(key, val, config.getLanguage(), csvProperties.getAttributesIgnoreCariageReturns(), csvProperties.getAttributesSplitSeparators());
				}
			}
		}

		if (null != csvProperties.getRating()) {
			try {
				final Rating r = new Rating();
//				r.setProviderName(config.getName());
//				r.setTimeStamp(System.currentTimeMillis());
				r.setMin(csvProperties.getRating().getMinValue());
				r.setMax(csvProperties.getRating().getMaxValue());
				r.addTag(csvProperties.getRating().getType());
				r.setValue(Double.valueOf(getFromCsvRow(item, csvProperties.getRating().getValue())));
//				r.setUrl(p.getUrl());
				p.addRating(r);
			} catch (final Exception e) {
				dedicatedLogger.warn("Error while adding rating for {} : {}", item, e.getMessage());
			}
		}

		for (final String desc : csvProperties.getDescription()) {

			String description = getFromCsvRow(item, desc);

			if (!StringUtils.isEmpty(description) && null != config.getDescriptionRemoveToken()) {

				for (String token : config.getDescriptionRemoveToken()) {
					description = description.replace(token, "");
				}
			}
		}

		try {
			for (final String imgCell : csvProperties.getImage()) {
				String r = getFromCsvRow(item, imgCell);
				if (!StringUtils.isEmpty(r)) {

					// Checking for image tokens exclusions
					if (null != csvProperties.getImageTokenExclusions()) {
						boolean skip = false;
						for (String re : csvProperties.getImageTokenExclusions()) {
							if (r.contains(re)) {
								skip = true;
								break;
							}
						}
						if (skip) {
							continue;
						}
					}

					p.addResource(r);

				}
			}
		} catch (final ValidationException e1) {
			dedicatedLogger.warn("Problem while adding resource for {}", item);
		}

		// Assuming that by default, for referentiels they are in stock
		p.setInStock(InStock.INSTOCK);
		if (null != csvProperties.getInStock()) {
			for (String inStock : csvProperties.getInStock()) {
				// Instock
				try {
					String val = getFromCsvRow(item, inStock);
					InStock stock = InStockParser.parse(val);
					if (null != stock) {
						p.setInStock(stock);
						break;
					}
				} catch (final Exception e1) {
					dedicatedLogger.info("Cannot parse InStock : {} ", e1.getMessage());
				}
			}
		}

		// Shipping time
		if (!StringUtils.isEmpty(csvProperties.getShippingTime())) {
			final String strW = getFromCsvRow(item, csvProperties.getShippingTime());
			try {
				p.setShippingTime(ShippingTimeParser.parse(strW));
			} catch (final Exception e1) {
				dedicatedLogger.info("Cannot parse shippingTime : {} ", e1.getMessage());
			}
		}

		// Instock quantity
		if (!StringUtils.isEmpty(csvProperties.getQuantityInStock())) {
			final String strW = getFromCsvRow(item, csvProperties.getQuantityInStock());
			if (StringUtils.isEmpty(strW)) {
				dedicatedLogger.info("No  ShippingCost in csv column {}", csvProperties.getQuantityInStock());
			} else {
				try {
					p.setQuantityInStock(Integer.valueOf(strW));
				} catch (final Exception e1) {
					dedicatedLogger.info("Cannot parse QuantityInStock : {} ", e1.getMessage());
				}
			}
		}

		// Shipping price
		if (!StringUtils.isEmpty(csvProperties.getShippingCost())) {
			final String strW = getFromCsvRow(item, csvProperties.getShippingCost());
			try {
				p.setShippingCost(ShippingCostParser.parse(strW));
			} catch (final Exception e1) {
				dedicatedLogger.info("Cannot parse ShippingCost : {} ", e1.getMessage());
			}
		}

		// Warranty
		if (!StringUtils.isEmpty(csvProperties.getWarranty())) {
			try {
				final String strW = getFromCsvRow(item, csvProperties.getWarranty());
				if (StringUtils.isEmpty(strW)) {
					dedicatedLogger.warn("No  warranty in csv column {}", csvProperties.getWarranty());
				} else {
					p.setWarranty(Integer.valueOf(strW));
				}
			} catch (final Exception e1) {
				dedicatedLogger.info("Cannot parse Warranty : {} ", e1.getMessage());
			}
		}

		// ProductCondition
		p.setProductState(config.getDefaultItemCondition());
		if (null != csvProperties.getProductState()) {
			for (String productState : csvProperties.getProductState()) {

				try {
					ProductCondition state = ProductConditionParser.parse(getFromCsvRow(item, productState));
					if (null != state) {
						p.setProductState(state);
						break;
					}
				} catch (final Exception e1) {
					dedicatedLogger.info("Cannot parse product state : {} ", e1.getMessage());
				}
			}
		}

		// TODO : Complete to get commons
		for (final Entry<ReferentielKey, Set<String>> refs : csvProperties.getReferentiel().entrySet()) {
			for (String csvKey : refs.getValue()) {

				final String val = getFromCsvRow(item, csvKey);
				if (StringUtils.isEmpty(val)) {
					dedicatedLogger.debug("No data for {} in {}", csvKey, item);
				} else {
					p.addReferentielAttribute(refs.getKey(), val);
					break;
				}
			}

			if (null == p.getReferentielAttributes().get(refs.getKey())) {
				dedicatedLogger.info("No referentiel attribute found for {} in {}", refs.getKey(), item);
			}
		}

		// If an affiliated url and no url, use affiliatedUrl
		// NOTE : Enforcement, to be sure than in all the processing and restitution
		// process we have a "by default" affiliatedUrl use
		if (!StringUtils.isEmpty(p.getAffiliatedUrl())) {
			p.setUrl(p.getAffiliatedUrl());
		}

		// We import all columns as attributes
		if (csvProperties.getImportAllAttributes()) {
			for (Entry<String, String> entry : item.entrySet()) {
				p.addAttribute(entry.getKey(), entry.getValue(), config.getLanguage(), true, Sets.newHashSet());
			}

		}

		// Completing with the web data if defined
		if (null != crawler) {
			dedicatedLogger.info("Completing CSV data fragment {} with web data at url {}", p, p.getUrl());

			if (!StringUtils.isEmpty(p.getUrl())) {
				// Completing the datafragment with the configured url
				DataFragment fragment = crawler.visitNow(controler, p.getUrl(), p);
				// TODO(p3,not working) Complete if need direct indexation from CSV

				p.setDatasourceConfigName(datasourceConfigName);

			} else {
				dedicatedLogger.warn("No url to crawl extracted from datafragment {}, will index without web crawling completion", p);
			}
		} else {
			// NOTE : completion service is made by completionCrawler
			////// "Standard" completion of the data fragment
			completionService.complete(p, datasourceConfigName, config, dedicatedLogger);
		}

		p.validate(config.getValidationFields());

		return p;

	}




	private void handleAttributes(final DataFragment pd, final DataSourceProperties config, final String attrRaw, Logger dedicatedLogger) {

		if (StringUtils.isEmpty(attrRaw)) {
			return;
		}

		final String[] lines = attrRaw.split(config.getCsvDatasource().getAttributesSplitChar());

		for (final String line : lines) {

			final String[] frags = line.split(config.getCsvDatasource().getAttributesKeyValSplitChar());
			if (frags.length != 2) {
				dedicatedLogger.info("Was expecting two fragments, got {} : {} at {}", frags.length, line, config.getName());
			} else {
				String key = frags[0];
				if (null != config.getCsvDatasource().getAttributesKeyKeepAfter()) {

					int pos = key.indexOf(config.getCsvDatasource().getAttributesKeyKeepAfter());
					if (-1 != pos) {
						key = key.substring(pos + 1).trim();
					}
				}

				// Splitters from conf
				pd.addAttribute(key, frags[1], config.getLanguage(), true, Sets.newHashSet());
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
	private String getFromCsvRow(final Map<String, String> item, final String colName) {

		final String val = item.get(colName);
		if (null != val) {
			return val;

		} else {
			return null;
		}
	}

	private List<String> getCategoryFromCsvRows(Map<String, String> item, List<String> colNames) {
		List<String> ret = new ArrayList<>();

		for (String colName : colNames) {
			String val = getFromCsvRow(item, colName);
			if (!StringUtils.isEmpty(val)) {
				ret.add(val);
			}
		}
		return ret;
	}

	/**
	 * Say this thread to stop
	 */
	public void stop() {
		this.stop = true;
	}

	public synchronized IndexationJobStat stats() {
		return stats;
	}

}