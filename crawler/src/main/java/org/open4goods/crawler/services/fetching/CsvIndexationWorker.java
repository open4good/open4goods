package org.open4goods.crawler.services.fetching;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
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
import org.open4goods.commons.config.yml.datasource.HtmlDataSourceProperties;
import org.open4goods.commons.exceptions.ValidationException;
import org.open4goods.commons.helper.IdHelper;
import org.open4goods.commons.helper.InStockParser;
import org.open4goods.commons.helper.ProductConditionParser;
import org.open4goods.commons.helper.ResourceHelper;
import org.open4goods.commons.helper.ShippingCostParser;
import org.open4goods.commons.helper.ShippingTimeParser;
import org.open4goods.commons.model.constants.InStock;
import org.open4goods.commons.model.constants.ProductCondition;
import org.open4goods.commons.model.constants.ReferentielKey;
import org.open4goods.commons.model.crawlers.IndexationJobStat;
import org.open4goods.commons.model.data.DataFragment;
import org.open4goods.commons.model.data.Price;
import org.open4goods.commons.model.data.Rating;
import org.open4goods.commons.model.data.Resource;
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
//	private volatile boolean stop;

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

		dedicatedLogger.warn("STARTING CRAWL OF {} - {}", dsProperties.getFeedKey(), dsProperties);

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
				csvService.brokenCsv(url);
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


	
	
	
	
	
	
	/**
	 * Parses a CSV line into a DataFragment object.
	 *
	 * @param crawler DataFragmentWebCrawler for web data completion
	 * @param controller CrawlController for controlling crawling operations
	 * @param config DataSourceProperties for configuration settings
	 * @param item Map representing a CSV line
	 * @param datasourceConfigName Name of the data source configuration
	 * @param dedicatedLogger Logger for logging information
	 * @param datasetUrl URL of the dataset being parsed
	 * @return DataFragment object containing parsed data
	 * @throws ValidationException if validation fails
	 */
	private DataFragment parseCsvLine(final DataFragmentWebCrawler crawler, final CrawlController controller, final DataSourceProperties config, final Map<String, String> item, final String datasourceConfigName, final Logger dedicatedLogger, String datasetUrl) throws ValidationException {
	    dedicatedLogger.info("Parsing line : {}", item);
	    
	    DataFragment dataFragment = new DataFragment();
	    dataFragment.setFragmentHashCode(item.hashCode());
	    
	    setUrl(dataFragment, item, config, dedicatedLogger, datasetUrl);
	    setAffiliatedUrl(dataFragment, item, config, dedicatedLogger);
	    trimUrlParameters(dataFragment, config);
	    setPrice(dataFragment, item, config, dedicatedLogger);
	    setNameAndTags(dataFragment, item, config);
	    setAttributes(dataFragment, item, config, dedicatedLogger);
	    setRating(dataFragment, item, config, dedicatedLogger);
	    setDescription(dataFragment, item, config);
	    addResources(dataFragment, item, config, dedicatedLogger);
	    setInStock(dataFragment, item, config, dedicatedLogger);
	    setShippingDetails(dataFragment, item, config, dedicatedLogger);
	    setProductState(dataFragment, item, config, dedicatedLogger);
	    addReferentielAttributes(dataFragment, item, config, dedicatedLogger);
	    enforceAffiliatedUrl(dataFragment);
	    
	    importAllAttributes(dataFragment, item, config);
	    completeWithWebData(dataFragment, crawler, controller, datasourceConfigName, config, dedicatedLogger);
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
	        String url = !StringUtils.isEmpty(csvProperties.getExtractUrlFromParam()) ? extractUrlFromParam(item, csvProperties) : getFromCsvRow(item, csvProperties.getUrl());
	        dataFragment.setUrl(url);
	        // revove from the source to prevent further integration
	        removeFromSource(item, csvProperties.getUrl());
	        
	    } catch (Exception e) {
	        logger.info("Error while extracting url in dataset {} :  {}", datasetUrl, item);
	    }
	}


	/**
	 * Extracts URL from a specific parameter in the CSV row.
	 *
	 * @param item Map representing a CSV line
	 * @param csvProperties CsvDataSourceProperties for configuration settings
	 * @return Extracted URL string
	 * @throws UnsupportedEncodingException if URL decoding fails
	 */
	private String extractUrlFromParam(Map<String, String> item, CsvDataSourceProperties csvProperties) {
	    String url = getFromCsvRow(item, csvProperties.getUrl());
	    UriComponents parsedUrl = UriComponentsBuilder.fromUriString(url).build();
	    return URLDecoder.decode(parsedUrl.getQueryParams().getFirst(csvProperties.getExtractUrlFromParam()), StandardCharsets.UTF_8);
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
	    if (csvProperties.getPrice() != null) {
	        for (String priceColumn : csvProperties.getPrice()) {
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
	            
	            
	        }
	    }
	    if (null == dataFragment.getPrice() || null == dataFragment.getPrice().getPrice()) {
	    	logger.warn("No price extracted for {}",item);
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
	        logger.warn("Error setting fallback price with setPriceAndCurrency(): {}", dataFragment.getUrl());
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
	    dataFragment.addName(getFromCsvRow(item, csvProperties.getName()));
	    
        // Delete from source
        removeFromSource(item, csvProperties.getName());
        
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
	            logger.warn("Error while adding rating for {} : {}", item, e.getMessage());
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
	    for (String descColumn : csvProperties.getDescription()) {
	        String description = getFromCsvRow(item, descColumn);
	        if (!StringUtils.isEmpty(description) && config.getDescriptionRemoveToken() != null) {
	            for (String token : config.getDescriptionRemoveToken()) {
	                description = description.replace(token, "");
	            }
	        }
	        
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
	        for (String imgCell : csvProperties.getImage()) {
	            String resource = getFromCsvRow(item, imgCell);
	            if (!StringUtils.isEmpty(resource) && shouldIncludeResource(resource, csvProperties)) {
	                dataFragment.addResource(resource);
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
	        logger.warn("Problem while adding resource for {}", item);
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
	private void setInStock(DataFragment dataFragment, Map<String, String> item, DataSourceProperties config, Logger logger) {
	    CsvDataSourceProperties csvProperties = config.getCsvDatasource();
	    dataFragment.setInStock(InStock.INSTOCK);
	    if (csvProperties.getInStock() != null) {
	        for (String inStockColumn : csvProperties.getInStock()) {
	            try {
					String value = getFromCsvRow(item, inStockColumn);
					if (!StringUtils.isEmpty(value)) {

						InStock inStock = InStockParser.parse(value);
						if (inStock != null) {
							dataFragment.setInStock(inStock);
							continue;
						}

	                }
					
		            // Delete from source
	                removeFromSource(item, inStockColumn);
	                
	            } catch (Exception e) {
	                logger.info("Cannot parse InStock : {}", e.getMessage());
	            }
	        }
	    }
	}

	/**
	 * Sets the shipping details of the DataFragment.
	 *
	 * @param dataFragment DataFragment to set shipping details
	 * @param item Map representing a CSV line
	 * @param config DataSourceProperties for configuration settings
	 * @param logger Logger for logging information
	 */
	private void setShippingDetails(DataFragment dataFragment, Map<String, String> item, DataSourceProperties config, Logger logger) {
	    CsvDataSourceProperties csvProperties = config.getCsvDatasource();
	    setShippingTime(dataFragment, item, csvProperties, logger);
	    setShippingCost(dataFragment, item, csvProperties, logger);
	    setQuantityInStock(dataFragment, item, csvProperties, logger);
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
	    if (!StringUtils.isEmpty(csvProperties.getShippingTime())) {
	        String shippingTimeStr = getFromCsvRow(item, csvProperties.getShippingTime());
	        try {
	            dataFragment.setShippingTime(ShippingTimeParser.parse(shippingTimeStr));
	            // Delete from source
                removeFromSource(item, csvProperties.getShippingTime());
	        } catch (Exception e) {
	            logger.info("Cannot parse shippingTime : {}", e.getMessage());
	        }
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
	private void setQuantityInStock(DataFragment dataFragment, Map<String, String> item, CsvDataSourceProperties csvProperties, Logger logger) {
	    if (!StringUtils.isEmpty(csvProperties.getQuantityInStock())) {
	        String quantityStr = getFromCsvRow(item, csvProperties.getQuantityInStock());
	        if (null != quantityStr) {
		        try {
		            dataFragment.setQuantityInStock(Integer.valueOf(quantityStr));
		            // Delete from source
	                removeFromSource(item, csvProperties.getQuantityInStock());
		        } catch (Exception e) {
		            logger.info("Cannot parse QuantityInStock : {}", e.getMessage());
		        }
	        }
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
	    if (!StringUtils.isEmpty(csvProperties.getShippingCost())) {
	        String costStr = getFromCsvRow(item, csvProperties.getShippingCost());
	        try {
	            dataFragment.setShippingCost(ShippingCostParser.parse(costStr));
	         // Delete from source
                removeFromSource(item, csvProperties.getShippingCost());
	        } catch (Exception e) {
	            logger.info("Cannot parse ShippingCost : {}", e.getMessage());
	        }
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
	    if (csvProperties.getProductState() != null) {
	        for (String productStateColumn : csvProperties.getProductState()) {
	            try {
	                ProductCondition productState = ProductConditionParser.parse(getFromCsvRow(item, productStateColumn));
	                if (productState != null) {
	                    dataFragment.setProductState(productState);
		                // Delete from source
	                    removeFromSource(item, productStateColumn);
	                    continue;
	                }
	            } catch (Exception e) {
	                logger.info("Cannot parse product state : {}", e.getMessage());
	            }
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
	            dataFragment.addAttribute(entry.getKey(), entry.getValue(), config.getLanguage(), true, Collections.emptySet());
	        }
	    }
	}

	/**
	 * Completes the DataFragment with web data if the crawler is defined.
	 *
	 * @param dataFragment DataFragment to complete with web data
	 * @param crawler DataFragmentWebCrawler for web data completion
	 * @param controller CrawlController for controlling crawling operations
	 * @param datasourceConfigName Name of the data source configuration
	 * @param config DataSourceProperties for configuration settings
	 * @param logger Logger for logging information
	 */
	private void completeWithWebData(DataFragment dataFragment, DataFragmentWebCrawler crawler, CrawlController controller, String datasourceConfigName, DataSourceProperties config, Logger logger) {
	    if (crawler != null && !StringUtils.isEmpty(dataFragment.getUrl())) {
	        logger.info("Completing CSV data fragment {} with web data at url {}", dataFragment, dataFragment.getUrl());
	        crawler.visitNow(controller, dataFragment.getUrl(), dataFragment);
	    } else {
	        completionService.complete(dataFragment, datasourceConfigName, config, logger);
	    }
	}
	
	

	/**
	 * Remove an attribute from the source jackson csv map,
	 * @param item
	 * @param url
	 */
	private void removeFromSource(Map<String, String> item, String key) {
		if (item.containsKey(key)) {
			item.remove(key);
		}
		
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

		return item.get(colName);
		
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

	public synchronized IndexationJobStat stats() {
		return stats;
	}

}