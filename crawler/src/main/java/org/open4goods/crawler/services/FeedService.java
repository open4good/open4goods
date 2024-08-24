package org.open4goods.crawler.services;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.commons.config.yml.datasource.DataSourceProperties;
import org.open4goods.commons.config.yml.datasource.FeedConfiguration;
import org.open4goods.commons.helper.IdHelper;
import org.open4goods.commons.services.DataSourceConfigService;
import org.open4goods.commons.services.SerialisationService;
import org.open4goods.crawler.services.fetching.CsvDatasourceFetchingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

/**
 * TODO : Optimize by checking last updated dates sometimes provided by the platforms
 * TODO : Should merge with CsvDatasourceFetchingService
 */
public class FeedService {
		
	private static final Logger logger = LoggerFactory.getLogger(FeedService.class);	
	private final ObjectMapper csvMapper = new CsvMapper().enable((CsvParser.Feature.IGNORE_TRAILING_UNMAPPABLE));
	
	private SerialisationService serialisationService;
	private CsvDatasourceFetchingService fetchingService;
	private DataSourceConfigService datasourceConfigService;
	private Map<String, FeedConfiguration>  feedConfigs;

	
	public FeedService(SerialisationService serialisationService, DataSourceConfigService datasourceConfigService, CsvDatasourceFetchingService fetchingService, Map<String, FeedConfiguration> feedConfigs) {
		super();
		this.feedConfigs = feedConfigs;
		this.fetchingService = fetchingService;
		this.datasourceConfigService = datasourceConfigService;
		this.serialisationService = serialisationService;
	}
	
	/**
	 * Fetch the feeds
	 * TODO : Make it from conf, not to retrive datasources at the same time than beta.nudger.fr
	 * ISSUE : MAke it from conf, not to retrieve datasources at the exact same time than beta.nudger.fr
	 */
	@Scheduled(cron = "0 0 20,8 * * ?")
	public void fetchFeeds() {
		logger.info("Fetching CSV affiliation feeds");
		// 1 - Loads the whole feeds as a list of DataSourceProperties, eventually hot defaulted
		
		Set<DataSourceProperties> ds = getFeedsUrl();
		List<DataSourceProperties> dsl =new ArrayList<DataSourceProperties>(ds);


		long seed = System.nanoTime();
		Collections.shuffle(dsl, new Random(seed));
		
		
		// Fetching the feeds
		
		
		logger.info("{} feeds to fetch", ds.size());		
		dsl.forEach((k) -> {
			try {
			logger.info("Fetching feed {} ", k);
			fetchingService.start(k,k.getDatasourceConfigName());
			} catch (Exception e) {
				logger.error("Error loading feed {}", k, e);
			}
		});
		
		
		
	}

	/**
	 * Fetch the feeds corresponding a given catalogurl
	 */
	public void fetchFeedsByUrl(String url) {
		logger.info("Fetching CSV affiliation feeds matching url : {}",url);
		// 1 - Loads the whole feeds as a list of DataSourceProperties, eventually hot defaulted
		
		Set<DataSourceProperties> ds = getFeedsUrl();

		logger.info("{} feeds to fetch", ds.size());		
		ds.forEach((k) -> {
			try {
			logger.info("Fetching feed {}", k);
			
			if (k.getCsvDatasource().getDatasourceUrls().contains(url)) {
					logger.info("Fetching feed {} - {}", k);
					fetchingService.start(k,k.getDatasourceConfigName());
				} else {
					logger.info("Skipping feed {} ", k);
			}			
			} catch (Exception e) {
				logger.error("Error loading feed {}", k, e);
			}
		});
	}
	
	
	/**
	 * Fetch the feeds corresponding a given feedkey
	 */
	public void fetchFeedsByKey(String feedKey) {
		logger.info("Fetching CSV affiliation feeds matching feed key : {}",feedKey);
		// 1 - Loads the whole feeds as a list of DataSourceProperties, eventually hot defaulted
		
		matchingKey(feedKey).forEach((v) -> {
			try {
				logger.info("Fetching feed by key {} - {}", v.getDatasourceConfigName(), v);
				fetchingService.start(v, v.getDatasourceConfigName());
			} catch (Exception e) {
				logger.error("Error loading feed {}", v.getDatasourceConfigName(), e);
			}
		});
	}

	/**
	 * Select the datasource properties matching a given feed key
	 * @param feedKey
	 * @return
	 */
	private Set<DataSourceProperties> matchingKey(String feedKey) {
		String cleanedKey = IdHelper.azCharAndDigits(feedKey).toLowerCase();
		Set<DataSourceProperties> ds = getFeedsUrl();
		Set<DataSourceProperties> ret = new HashSet<DataSourceProperties>();
		ds.forEach((k) -> {
			try {
				if (cleanedKey.equals(IdHelper.azCharAndDigits(k.getDatasourceConfigName()).toLowerCase()) || cleanedKey.equals(IdHelper.azCharAndDigits(k.getName()).toLowerCase())) {
					ret.add(k);
					logger.info("Found feed byKey :  {}", k);
				} else {
				}
			} catch (Exception e) {
				logger.error("Error searching feed by key {}", k, e);
			}
		});
		return ret;
	}
	
	/**
	 * Fetch the feeds corresponding a given feedkey
	 *
	 * @return
	 */
	public Set<DataSourceProperties> getFeedsUrl() {
		Set<DataSourceProperties> ds = new HashSet<DataSourceProperties>();
		
		feedConfigs.entrySet().stream().forEach(entry -> {
			try {
				ds.addAll(loadCatalog(entry.getValue().getCatalogUrl(),entry.getValue()));
			} catch (Exception e) {
				logger.error("Error loading catalog {} - {} ", entry.getKey(),entry.getValue(), e);
			}
		});
		
		
		// Adding all datasources that do not belong to a catalog (not having feedkey defined)
		
		
		datasourceConfigService.datasourceConfigs(). forEach((k,v) -> {
			try {
				if (null != v.getCsvDatasource() && StringUtils.isEmpty(v.getFeedKey())) {
					logger.info("Adding orphan feed {} ", k);
					// TODO : ugly Tweak
					v.setDatasourceConfigName(k);
					ds.add(v);
				}
			} catch (Exception e) {
				logger.error("Error loading feed {}", k, e);
			}
		});
		
		return ds;
	}

	/**TOTO : Implementa	tion with BeanUtils.copy make me feel like a bad design...
	 * Load a catalog
	 * @param catalogUrl
	 * @param feedConfig
	 * @throws MalformedURLException
	 * @throws IOException
	 * TODO : use set
	 */
	public Set<DataSourceProperties> loadCatalog(String catalogUrl, FeedConfiguration feedConfig) throws MalformedURLException, IOException {
		/////////////////////////////
		// Csv Shema definition
		////////////////////////////
		logger.info("Loading CSV catalog from : {}", catalogUrl);
		
		Set<DataSourceProperties> ret = new HashSet<DataSourceProperties>();
		CsvSchema schema;
		
		// TODO : Schema

		
		 schema = CsvSchema.emptySchema()
					.withHeader()
					.withColumnSeparator(',')
					.withQuoteChar('"')
					;


				// configure the reader on what bean to read and how we want to write
				// that bean
				final ObjectReader oReader = csvMapper.readerFor(Map.class).with(schema);

				
				final MappingIterator<Map<String, String>> mi = oReader.readValues(new URL(catalogUrl));
				
				while (mi.hasNext()) {
					try {

						// Handle the csv line
						Map<String, String> line = mi.next();
						
						//////////////////////////
						// Retrieving the feed key
						//////////////////////////
						String feedKey =  line.get(feedConfig.getDatasourceKeyAttribute());

						
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

						
						for (String filter : feedConfig.getExcludeFeedKeyContains()) {
							if (feedKey.toLowerCase().contains(filter.toLowerCase())) {
                                abort = true;		
                                break;
                            }
						}						
						if (abort) {
							logger.info("Skipping line {} because of config filters", line);
							continue;
						}
						
						
						//////////////////////////
						// Fetch the corresponding datasource, default one if none
						//////////////////////////	
						
						

						// Adding URL
						String feedUrl = line.get(feedConfig.getDatasourceUrlAttribute());
						DataSourceProperties ds = getVolatileDataSource(feedKey,feedConfig, feedUrl);
						
						if (null == ds) {
							logger.warn("NO DATASOURCE created for feed key {}, skipping line {}", feedKey, line);
							continue;
						}
						
						logger.info("Datasource {} add url : {}", ds,feedUrl);
						
						if (feedConfig.getDatasourceLanguageAttribute() != null) {
							// TODO normalize the languge
							ds.setLanguage(line.get(feedConfig.getDatasourceLanguageAttribute()));
						}
						

						
						ret.add(ds);
						
					} catch (Exception e) {
                        logger.error("Error handling line {}", e);
                    }
				}
				return ret;
	}

	/**
	 * Get a copy of datasource corresponding to a given feedKey, or the default one if none
	 * @param feedKey
	 * @param feedConfig
	 * @param feedUrl 
	 * @return
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws NoSuchMethodException 
	 * @throws InstantiationException 
	 * @throws IOException 
	 * @throws JsonMappingException 
	 * @throws JsonParseException 
	 */
	private DataSourceProperties getVolatileDataSource(String feedKey, FeedConfiguration feedConfig, String feedUrl) throws IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException, JsonParseException, JsonMappingException, IOException {
		
		// Checking if a custom vertical has been defined for this feedkey
		DataSourceProperties existing = datasourceConfigService.getDatasourcePropertiesForFeed(feedKey);
				
		// Creating a default datasource
		DataSourceProperties ds = new DataSourceProperties();
				
		if (null == existing) {
			// If not, set the CSV fetcher associated with the catalog provider
			logger.error("NO DATASOURCE found for feed key {}, using the default one", feedKey);
			ds.setCsvDatasource(feedConfig.getDefaultCsvProperties());
			ds.setDatasourceConfigName(feedKey);
		} else {
			logger.error("DATASOURCE found for feed key {}", feedKey);
			ds = existing;
		}
		
		
		// Making a deep copy to avoid side effects
//		DataSourceProperties ret = (DataSourceProperties) SerializationUtils.clone(ds);							
		// Deep copy through jackson
		// TODO : perf
		DataSourceProperties  ret = serialisationService.fromJson(serialisationService.toJson(ds)  , DataSourceProperties.class);
		
		
		// Adding ID infos, only for defaulted config
		if (null == existing) {
			String name = IdHelper.azCharAndDigits(feedKey);
			ret.setName(name);
		} else {			
			ret.setDatasourceConfigName(ds.getName()); 
		}

		// Adding the url associated with the catalog entry (ensure only this url, removing other one inherited from specific ds configs)
		ret.getCsvDatasource().getDatasourceUrls().clear();
		ret.getCsvDatasource().getDatasourceUrls().add(feedUrl);

		return ret;
	}

}
