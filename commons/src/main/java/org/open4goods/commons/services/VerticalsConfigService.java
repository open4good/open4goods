package org.open4goods.commons.services;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.open4goods.commons.config.yml.ui.VerticalConfig;
import org.open4goods.commons.dao.ProductRepository;
import org.open4goods.commons.model.constants.CacheConstants;
import org.open4goods.commons.model.dto.ExpandedTaxonomy;
import org.open4goods.commons.model.product.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;

import com.fasterxml.jackson.databind.ObjectReader;

import jakarta.annotation.PreDestroy;

/**
 * This service is in charge to provide the Verticals configurations.
 * Configurations are provided from the classpath AND from a git specific
 * project (fresh local clone on app startup)
 * @author goulven
 *
 */
public class VerticalsConfigService {

	public static final Logger logger = LoggerFactory.getLogger(VerticalsConfigService.class);

	private static final String DEFAULT_CONFIG_FILENAME = "_default.yml";

	private static final String CLASSPATH_VERTICALS = "classpath:/verticals/*.yml";
	private static final String CLASSPATH_VERTICALS_DEFAULT = "classpath:/verticals/_default.yml";

	private SerialisationService serialisationService;

	private final Map<String, VerticalConfig> configs = new ConcurrentHashMap<>(100);

	// The cache of categories to verticalconfig association. datasource (or all) -> category -> VerticalConfig
	private final Map<String,Map<String, VerticalConfig>> categoriesToVertical = new ConcurrentHashMap<>();

	private Map<String,VerticalConfig> byUrl = new HashMap<>();

	private Map<String,String> toLang = new HashMap<>();

	private Map<Integer,VerticalConfig> byTaxonomy = new HashMap<>();


	private ProductRepository productRepository;

	private GoogleTaxonomyService googleTaxonomyService;

	private ResourcePatternResolver resourceResolver;

	private ImageGenerationService imageGenerationService;

	//
	private final ExecutorService executorService = Executors.newFixedThreadPool(1);

	// The default config
	private VerticalConfig defaultConfig;


	public VerticalsConfigService(SerialisationService serialisationService, GoogleTaxonomyService googleTaxonomyService, ProductRepository productRepository, ResourcePatternResolver resourceResolver, ImageGenerationService imageGenerationService) {
		super();
		this.serialisationService = serialisationService;
		this.googleTaxonomyService = googleTaxonomyService;
		this.productRepository = productRepository;
		this.resourceResolver = resourceResolver;
		this.imageGenerationService = imageGenerationService;

		// initial configs loads
		loadConfigs();
		
		
		// Update the google product categories with defined verticals
		getConfigsWithoutDefault().stream().forEach(v -> {
			googleTaxonomyService.updateCategoryWithVertical(v);
		});
		

	}

	/**
	 * Loads configs from classpath and from giit repositories. Thread safe
	 * operation, so can be used in refresh
	 */
//	@Scheduled(fixedDelay = 1000 * 60 * 10, initialDelay = 1000 * 60 * 10)
	public synchronized void loadConfigs() {

		Map<String, VerticalConfig> vConfs = new ConcurrentHashMap<>(100);

		// Setting the default config
		try {
			Resource r = resourceResolver.getResource(CLASSPATH_VERTICALS_DEFAULT);
			defaultConfig =  serialisationService.fromYaml(r.getInputStream(), VerticalConfig.class);
		} catch (IOException e) {
			logger.error("Cannot load  default config from {}", CLASSPATH_VERTICALS_DEFAULT, e);
		}
		
		/////////////////////////////////////////
		// Load configurations from classpath
		/////////////////////////////////////////

		for (VerticalConfig uc : loadFromClasspath()) {
			logger.info("Adding config {} from classpath", uc.getId());
			vConfs.put(uc.getId(), uc);
		}
		// Switching confs
		synchronized (configs) {
			configs.clear();
			configs.putAll(vConfs);
		}

		// Associating categoriesToVertical
		synchronized (categoriesToVertical) {
			categoriesToVertical.clear();
			configs.values().forEach(c -> c.getMatchingCategories().entrySet().forEach(cc -> {
				try {
					if (!categoriesToVertical.containsKey(cc.getKey())) {
						categoriesToVertical.put(cc.getKey(), new HashMap<String, VerticalConfig>());
					}
					if (null != cc.getValue()) {
						cc.getValue().forEach(cat -> {
							categoriesToVertical.get(cc.getKey()).put(cat, c);	
						});
					}
				} catch (Exception e) {
					logger.error("Error loading categories matching map : {}",c,e);
				}
				
			}));
		}

		// Associati
		
		
		// Mapping url to i18n
		getConfigsWithoutDefault().forEach(vc -> vc.getI18n().forEach((key, value) -> {

			toLang.put(value.getVerticalHomeUrl(), key);
			byUrl.put(value.getVerticalHomeUrl(), vc);
			byTaxonomy.put(vc.getGoogleTaxonomyId(), vc);
		}));

	}



	/**
	 * 
	 * @return the available verticals configurations from classpath
	 */
	private List<VerticalConfig> loadFromClasspath() {
		List<VerticalConfig> ret = new ArrayList<>();
		Resource[] resources = null;
		try {
			resources = resourceResolver.getResources(CLASSPATH_VERTICALS);
		} catch (IOException e) {
			logger.error("Cannot load  verticals from {} : {}", CLASSPATH_VERTICALS, e.getMessage());
			return ret;
		}

		for (Resource r : resources) {
			if (r.getFilename().equals(DEFAULT_CONFIG_FILENAME)) {
				continue;
			}
			try {
				ret.add(getConfig(r.getInputStream(), getDefaultConfig()));
			} catch (IOException e) {
				logger.error("Cannot retrieve vertical config : {}",r.getFilename(), e);
			}
		}
			
		return ret;
	}
	
	
	
	/**
	 * Instanciate a config with a previously defaulted one
	 *
	 * @param inputStream
	 * @param defaul
	 * @return
	 * @throws IOException
	 */
	public VerticalConfig getConfig(InputStream inputStream, VerticalConfig defaul) throws IOException {

		// TODO(p3,perf) : chould be cached
		VerticalConfig copy = serialisationService.fromYaml(serialisationService.toYaml(defaul),VerticalConfig.class);
		ObjectReader objectReader = serialisationService.getYamlMapper().readerForUpdating(copy);
		VerticalConfig ret = objectReader.readValue(inputStream);
		inputStream.close();
		return ret;
	}

	/**
	 * Instanciate a vertical config for a given categories bag
	 * @param inputStream
	 * @param existing
	 * @return
	 * @throws IOException
	 */
	public VerticalConfig getVerticalForCategories(Map<String, String> productCategoriessByDatasource) {
		
		VerticalConfig vc = null;

		for (Entry<String, String> category : productCategoriessByDatasource.entrySet()) {
			// Searching in the specific category
			Map<String, VerticalConfig> keys = categoriesToVertical.get(category.getKey());
			if (null != keys) {
				
				vc = keys.get(category.getValue());
				if (null != vc) {
					break;
				}
			}
			
			// Searching in the ALL category
			vc = categoriesToVertical.get("all").get(category.getValue());
			if (null != vc) {
				break;
			}
			
		}
		
		// Looking for words exclusions in categories
		if (null != vc) {
		    for (String token : vc.getExcludingTokensFromCategoriesMatching()) {
		        for (String category : productCategoriessByDatasource.values()) {
		            if (category.contains(token)) {
		                logger.warn("Excluded from matching category {} because categories contains word {}", vc.getId(), token, category);
		                vc = null;
		                return null;
		            }
		        }
		    }
		}

		return vc;
	}



	/**
	 * Return the language for a vertical path, if any
	 * @param path
	 * @return
	 */
	public VerticalConfig getVerticalForPath(String path) {
		return byUrl.get(path);
	}

	
	
	/**
 	 *  Return the path for a vertical language, if any
	 * @param config 
	 * @param path
	 * @return
	 */
	public String getPathForVerticalLanguage(String language, VerticalConfig config) {
		String path = configs.get(config.getId()).i18n(language).getVerticalHomeUrl();		
		return path;
	}

	/**
	 * Splits the VerticalConfig objects into buckets of a specified size, limiting the total number of buckets.
	 * This is UI HELPER METHOD
	 * @param bucketSize the size of each bucket (number of VerticalConfig objects per bucket).
	 * @param maxBucket the maximum number of buckets to return.
	 * @return a list of buckets, where each bucket is a list of VerticalConfig objects.
	 */
	public List<List<VerticalConfig>> getImpactScoreVerticalsByBuckets(int bucketSize, int maxBucket) {
	    // Get the map of VerticalConfig objects
	    Map<String, VerticalConfig> theConfigs = getConfigs();

	    // Create a list to hold all VerticalConfig objects
	    List<VerticalConfig> configList = new ArrayList<>(theConfigs.values());

	    // Create a list to hold the final buckets
	    List<List<VerticalConfig>> buckets = new ArrayList<>();

	    // Iterate and create buckets
	    for (int i = 0; i < configList.size() && buckets.size() < maxBucket; i += bucketSize) {
	        // Create a sublist for the current bucket
	        List<VerticalConfig> bucket = configList.subList(i, Math.min(i + bucketSize, configList.size()));

	        // Add the bucket to the list of buckets
	        buckets.add(new ArrayList<>(bucket)); // Use a new ArrayList to ensure immutability of sublists
	    }

	    return buckets;
	}

	
	/**
	 *
	 * @return
	 * @throws IOException
	 */
	public VerticalConfig getDefaultConfig() {

		return defaultConfig;
	
	}


	/**
	 * Return a config by it's icecat categoryId
	 * @param icecatCategoryId
	 * TODO(p2,perf) : Maintain a map for key/val access
	 * @return
	 */
	public VerticalConfig getByIcecatCategoryId(Integer icecatCategoryId) {
		return configs.values().stream().filter(e ->icecatCategoryId.equals(e.getIcecatTaxonomyId())).findFirst().orElse(null);
	}

	/**
	 * Return a config by it's google taxonomy id
	 * @param icecatCategoryId
	 * @return
	 */
	public VerticalConfig getByGoogleTaxonomy(Integer googleCategoryId) {
		return configs.values().stream().filter(e ->googleCategoryId.equals(e.getGoogleTaxonomyId())).findFirst().orElse(null);
	}
	
	/**
	 * Return all expanded taxonomies, from the taxonomy service and from queryning on the store
	 * @return
	 */
	@Cacheable(keyGenerator = CacheConstants.KEY_GENERATOR,  cacheNames = CacheConstants.ONE_DAY_LOCAL_CACHE_NAME)
	public List<ExpandedTaxonomy> expandedTaxonomies() {
		List<ExpandedTaxonomy> ret = new ArrayList<>();
		productRepository.byTaxonomy().entrySet().forEach(t -> {
            ExpandedTaxonomy et = new ExpandedTaxonomy();
            et.setTaxonomyId(t.getKey());
            et.setTaxonomyName(googleTaxonomyService.getTaxonomyName(t.getKey())+"");
            et.setTotal(t.getValue());
            et.setAssociatedVertical(getVerticalForTaxonomy(t.getKey()));
            ret.add(et);
        });
		
		return ret;
	}
	
	/**
	 * Return a vertical config for a given taxonomy id
	 * @param key
	 * @return
	 */

	public VerticalConfig getVerticalForTaxonomy(Integer key) {
		return byTaxonomy.get(key);
	}

	/**
	 * Return a config by it's Id
	 *
	 * @param verticalName
	 * @return
	 */
	public VerticalConfig getConfigById(String verticalName) {
		if (null == verticalName) {
			return null;
		} else {			
			return configs.get(verticalName);
		}
	}

	/**
	 * Return a config by it's Id, or the default one if not found
	 * @param vertical
	 * @return
	 */
	public VerticalConfig getConfigByIdOrDefault(String vertical) {
		// Getting the config for the category, if any
		VerticalConfig vConf = getConfigById(vertical);
		
		if (null == vConf) {
			vConf = getDefaultConfig();
		}
		return vConf;
	}
	

	/**
	 *
	 * @return all configs, except the _default.  Allow to filter on enabled verticals
	 */
	public Collection<VerticalConfig>  getConfigsWithoutDefault(boolean onlyEnabled) {
		if (onlyEnabled) {
			return getConfigs().values().stream().filter(e->e.isEnabled() == true).toList();			
		} else {
			return getConfigs().values();
		}
	}
	

	/**
	 *
	 * @return all configs, except the _default. 
	 */
	public Collection<VerticalConfig>  getConfigsWithoutDefault() {
		return getConfigsWithoutDefault(false);
	}
	/**
	 *
	 * @return all Vertical configs
	 */
	public Map<String, VerticalConfig> getConfigs() {
		return configs;
	}


	/**
	 * Shuts down the ExecutorService gracefully, waiting for existing tasks to complete.
	 * If the tasks do not complete within the timeout, it forces a shutdown.
	 */
	@PreDestroy
	public void shutdownExecutorService() {
		executorService.shutdown();
		try {
			if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
				executorService.shutdownNow();
				if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
					logger.error("ExecutorService did not terminate");
				}
			}
		} catch (InterruptedException ie) {
			executorService.shutdownNow();
			Thread.currentThread().interrupt();
		}
	}

	
	/**
	 * Return all products matching the vertical in the config or already having a
	 * vertical defined
	 * 
	 * @param v
	 * @return
	 */
	// TODO : Could add datasourcename in a virtual "all", then apply the logic filter to batch get all categories matching....
	public Stream<Product> getProductsMatchingCategoriesOrVerticalId(VerticalConfig v) {
		
		// We match larger, on all matching categories cause those fields are not indexed
		Set<String> datasources = new HashSet<String>();
		v.getMatchingCategories().values().forEach(cat -> {
			cat.forEach(elem -> {
				datasources.add(elem);
			});
		});
		
		Criteria c = new Criteria("datasourceCategories").in(datasources)
				.or(new Criteria("vertical").is(v.getId()));
		
		final NativeQuery initialQuery = new NativeQueryBuilder().withQuery(new CriteriaQuery(c)).build();

		return productRepository.getElasticsearchOperations()
				.searchForStream(initialQuery, Product.class, ProductRepository.CURRENT_INDEX).stream()
				.map(SearchHit::getContent);
				// We have all categories matching, refine here to match the standard agg behaviour
//				.filter(e -> {
//					VerticalConfig cat = getVerticalForCategories(e.getCategoriesByDatasources());
//					if (null != cat && cat.getId().equals(v.getId())) {
//						return true;
//					}
//					return false;
//				});
				

	}
	
}
