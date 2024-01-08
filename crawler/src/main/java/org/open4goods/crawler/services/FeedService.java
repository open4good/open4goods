package org.open4goods.crawler.services;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Map.Entry;

import org.open4goods.config.yml.datasource.CsvDataSourceProperties;
import org.open4goods.config.yml.datasource.DataSourceProperties;
import org.open4goods.config.yml.datasource.FeedConfiguration;
import org.open4goods.crawler.services.fetching.CsvDatasourceFetchingService;
import org.open4goods.helper.IdHelper;
import org.open4goods.services.DataSourceConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

/**
 * TODO : Optimize by checking last updated dates sometimes provided by the platforms
 */
public class FeedService {
		
	private static final Logger logger = LoggerFactory.getLogger(FeedService.class);	
	private final ObjectMapper csvMapper = new CsvMapper().enable((CsvParser.Feature.IGNORE_TRAILING_UNMAPPABLE));
	
	private CsvDatasourceFetchingService fetchingService;
	private DataSourceConfigService datasourceConfigService;
	private Map<String, FeedConfiguration>  feedConfigs;

	
	public FeedService(DataSourceConfigService datasourceConfigService, CsvDatasourceFetchingService fetchingService, Map<String, FeedConfiguration> feedConfigs) {
		super();
		this.feedConfigs = feedConfigs;
		this.fetchingService = fetchingService;
		this.datasourceConfigService = datasourceConfigService;
	}
	
	/**
	 * Fetch the feeds
	 */
	 
	public void fetchFeeds() {
		logger.info("Fetching CSV affiliation feeds");
		// 1 - Loads the whole feeds as a list of DataSourceProperties, eventually hot defaulted
		feedConfigs.entrySet().stream().forEach(entry -> {
			try {
				loadCatalog(entry.getValue().getCatalogUrl(), entry.getValue());
			} catch (Exception e) {
				logger.error("Error loading catalog {}", entry.getValue().getCatalogUrl(), e);
			}
		});
	}

	/**
	 * Load a catalog
	 * @param url
	 * @param feedConfig
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	public void loadCatalog(String url, FeedConfiguration feedConfig) throws MalformedURLException, IOException {
		/////////////////////////////
		// Csv Shema definition
		////////////////////////////
		logger.info("Loading CSV catalog from : {}", url);
		
		CsvSchema schema;
		
		
//		if (config.getCsvSanitisation().booleanValue()) {				
//			schema = CsvSchema.emptySchema()
//								.withHeader()
//								.withColumnSeparator(SANITISED_COLUMN_SEPARATOR)
//								.withEscapeChar(SANITIZED_ESCAPE_CHAR)
//								.withQuoteChar(SANITIZED_QUOTE_CHAR)
//								;
//		} else {
//			 schema = CsvSchema.emptySchema()
//					.withHeader()
//					.withColumnSeparator(config.getCsvSeparator())						
//					;
//
//			 if (null != config.getCsvQuoteChar()) {
//				 schema = schema.withQuoteChar(config.getCsvQuoteChar().charValue());
//			 } else {
//				 schema = schema.withoutQuoteChar();
//			 }
//			 
//			 if (null != config.getCsvEscapeChar()) {
//				 schema = schema.withEscapeChar(config.getCsvEscapeChar());
//			 }
//		}

		
		 schema = CsvSchema.emptySchema()
					.withHeader()
					.withColumnSeparator(',')
					.withQuoteChar('"')
					;


				// configure the reader on what bean to read and how we want to write
				// that bean
				final ObjectReader oReader = csvMapper.readerFor(Map.class).with(schema);

				
				final MappingIterator<Map<String, String>> mi = oReader.readValues(new URL(url));
				
				while (mi.hasNext()) {
					try {

						// Handle the csv line
						Map<String, String> line = mi.next();
						//////////
						// Filter the line on defined attributes
						//////////
						boolean abort = false;
						
						
						for (Entry<String, String> filter : feedConfig.getFilterAttributes().entrySet()) {
							if (!filter.getValue().equals(line.get(filter.getKey()))) {
                                abort = true;		
                                break;
                            }
						}						
						if (abort) {
							logger.info("Skipping line {} because of config filters", line);
							continue;
						}

						//////////////////////////
						// Retrieving the feed key
						//////////////////////////
						String feedKey =  line.get(feedConfig.getDatasourceKeyAttribute());
						
						//////////////////////////
						// Fetch the corresponding datasource, default one if none
						//////////////////////////						
						DataSourceProperties ds = datasourceConfigService.getDatasourcePropertiesForFeed( feedKey);
						// If not, fallback to the default one and alert
						if (null == ds) {
							logger.error("NO DATASOURCE found for feed key {}, getting the default one", feedKey);
							ds = datasourceConfigService.getDefaultDataSource();
							String name = IdHelper.azCharAndDigits(feedKey);
							
							CsvDataSourceProperties csvDatasource = feedConfig.getDefaultCsvProperties();
							csvDatasource.setName(name);
							csvDatasource.getDatasourceUrls().add(line.get(feedConfig.getDatasourceUrlAttribute()));
							ds.setDatasourceConfigName(name+"-FEED");
							ds.setName(name);
							ds.setCsvDatasource(csvDatasource);
						} else {
							ds.setDatasourceConfigName(ds.getName());
						}
						
						
						if (feedConfig.getDatasourceLanguageAttribute() != null) {
							// TODO normalize the languge
							ds.setLanguage(line.get(feedConfig.getDatasourceLanguageAttribute()));
						}
						
						//////////////////////////
						// Add the csv fetching request to the queue
						//////////////////////////
						
						fetchingService.start(ds, ds.getDatasourceConfigName());
						
					} catch (Exception e) {
                        logger.error("Error handling line {}", e);
                    }
				}
	}

}
