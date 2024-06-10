package org.open4goods.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PreDestroy;
import org.open4goods.config.yml.ui.VerticalConfig;
import org.open4goods.dao.ProductRepository;
import org.open4goods.model.constants.CacheConstants;
import org.open4goods.model.dto.ExpandedTaxonomy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.scheduling.annotation.Scheduled;

import com.fasterxml.jackson.databind.ObjectReader;

/**
 * This service is in charge to provide the Verticals configurations.
 * Configurations are provided from the classpath AND from a git specific
 * project (fresh local clone on app startup)
 * TODO : Should be in the verticals sub projetc
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

	private final Map<String,VerticalConfig> categoriesToVertical = new ConcurrentHashMap<>();

	private Map<String,VerticalConfig> byUrl = new HashMap<>();

	private Map<String,String> toLang = new HashMap<>();

	private Map<Integer,VerticalConfig> byTaxonomy = new HashMap<>();

	private String verticalsFolder;

	private ProductRepository productRepository;

	private GoogleTaxonomyService googleTaxonomyService;

	private ResourcePatternResolver resourceResolver;

	private ImageGenerationService imageGenerationService;

	//
	private final ExecutorService executorService = Executors.newFixedThreadPool(1);


	public VerticalsConfigService(SerialisationService serialisationService, String verticalsFolder, GoogleTaxonomyService googleTaxonomyService, ProductRepository productRepository, ResourcePatternResolver resourceResolver, ImageGenerationService imageGenerationService) {
		super();
		this.serialisationService = serialisationService;
		this.verticalsFolder = verticalsFolder;
		this.googleTaxonomyService = googleTaxonomyService;
		this.productRepository = productRepository;
		this.resourceResolver = resourceResolver;
		this.imageGenerationService = imageGenerationService;

		// initial configs loads
		loadConfigs();

	}

	/**
	 * Loads configs from classpath and from giit repositories. Thread safe
	 * operation, so can be used in refresh
	 */
	@Scheduled(fixedDelay = 1000 * 60 * 10, initialDelay = 1000 * 60 * 10)
	public synchronized void loadConfigs() {

		Map<String, VerticalConfig> vConfs = new ConcurrentHashMap<>(100);

		/////////////////////////////////////////
		// Load configurations from classpath
		/////////////////////////////////////////

		for (VerticalConfig uc : loadFromFolder()) {
			logger.info("Adding config {} from frolder", uc.getId());
			vConfs.put(uc.getId(), uc);
		}

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
			configs.values().forEach(c -> c.getMatchingCategories().forEach(cc -> categoriesToVertical.put(cc, c)));
		}

		// Mapping url to i18n
		getConfigsWithoutDefault().forEach(vc -> vc.getI18n().forEach((key, value) -> {

			toLang.put(value.getVerticalHomeUrl(), key);
			byUrl.put(value.getVerticalHomeUrl(), vc);
			byTaxonomy.put(vc.getTaxonomyId(), vc);
//			verticalUrlByLanguage.put(key, value);
		}));

		generateImagesForVerticals(vConfs);
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
				logger.error("Cannot retrieve vertical config",e);
			}
		}
			
		return ret;
	}
	
	
	
	/**
	 *
	 * @return the available verticals configurations from classpath
	 */
	private List<VerticalConfig> loadFromFolder() {
		List<VerticalConfig> ret = new ArrayList<>();

		File verticalFolder = new File(verticalsFolder);
		if (!verticalFolder.isDirectory()) {
			logger.warn("Cannot load verticals from {} : not a valid directory", verticalsFolder);
			return ret;
		}
		
		for (File filename : verticalFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"))) {
			if (filename.getName().equals(DEFAULT_CONFIG_FILENAME)) {
				continue;
			}
			try {
				ret.add(getConfig(new FileInputStream(filename), getDefaultConfig()));
			} catch (IOException e) {
				logger.error("Cannot retrieve vertical config", e);
			}
		}

		return ret;
	}

	/**
	 * Instanciate a config with a previously defaulted one
	 *
	 * @param inputStream
	 * @param existing
	 * @return
	 * @throws IOException
	 */
	public VerticalConfig getConfig(InputStream inputStream, VerticalConfig existing) throws IOException {

		ObjectReader objectReader = serialisationService.getYamlMapper().readerForUpdating(existing);
		VerticalConfig ret = objectReader.readValue(inputStream);
		inputStream.close();
		return ret;
	}

	/**	Add a config from api endpoint **/
	public void addTmpConfig(VerticalConfig vc) {

		try {
			logger.warn("Adding a non versionned vertical config file. Have to do a PR on open4goods-config to persist it");
			File dest = new File(verticalsFolder + File.separator + vc.getId() + ".yml");
			serialisationService.getYamlMapper().writeValue(dest, vc);

			// Reload configs
			loadConfigs();
		} catch (Exception e) {
			logger.error("Cannot persist vertical config", e);
		}
    }

	/**
	 * Instanciate a vertical config for a given category name
	 * TODO :	Performance : cache the result : https://stackoverflow.com/questions/44529029/spring-cache-with-collection-of-items-entities
	 * @param inputStream
	 * @param existing
	 * @return
	 * @throws IOException
	 */
	@Cacheable(cacheNames = CacheConstants.FOREVER_LOCAL_CACHE_NAME)
	// ISSUE : Performance issue here, cache as a unique hash of categories
	// TODO : Performance issue here, cache as a unique hash of categories
	//labels:bug,perf
	public VerticalConfig getVerticalForCategories(Set<String> categories) {
		

		VerticalConfig vc = null;

		for (String category : categories) {
			vc = categoriesToVertical.get(category);

			// Discarding if unmatching category
			if (null != vc) {
				if (vc.getUnmatchingCategories().contains(category)) {
					vc = null;
				}
			}

			if (null != vc) {
				break;
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

	
	public String getLanguageForPath(String vertical) {
		// TODO Auto-generated method stub
		return null;
	}

	
	
	/**
	 * Return the path for a vertical language, if any
	 * @param config 
	 * @param path
	 * @return
	 */
	public String getPathForVerticalLanguage(String language, VerticalConfig config) {
		String path = configs.get(config.getId()).i18n(language).getVerticalHomeUrl();		
		return path;
	}

	/**
	 *
	 * @return
	 * @throws IOException
	 */
	@Cacheable(cacheNames = CacheConstants.FOREVER_LOCAL_CACHE_NAME)
	public VerticalConfig getDefaultConfig() {
//		VerticalConfig ret = null;
//		try {
//			FileInputStream f = new FileInputStream(verticalsFolder + File.separator + DEFAULT_CONFIG_FILENAME);
//			ret = serialisationService.fromYaml(f, VerticalConfig.class);
//			f.close();
//		} catch (Exception e) {
//			logger.error("Error getting default config", e);
//		}
//		return ret;
		
		
		
		List<VerticalConfig> ret = new ArrayList<>();
		try {
			Resource r = resourceResolver.getResource(CLASSPATH_VERTICALS_DEFAULT);
			return serialisationService.fromYaml(r.getInputStream(), VerticalConfig.class);
		} catch (IOException e) {
			logger.error("Cannot load  verticals from {} : {}", CLASSPATH_VERTICALS, e.getMessage());
			return null;
		}
	}

	/**
	 * Return all expanded taxonomies, from the taxonomy service and from queryning on the store
	 * @return
	 */
	@Cacheable(cacheNames = CacheConstants.ONE_DAY_LOCAL_CACHE_NAME)
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
	 * @return all configs, except the _default 
	 */
	public Collection<VerticalConfig>  getConfigsWithoutDefault() {
		return getConfigs().values();
	}
	/**
	 *
	 * @return all Vertical configs
	 */
	public Map<String, VerticalConfig> getConfigs() {
		return configs;
	}


	/**
	 * Submits tasks to generate images for each vertical configuration using ExecutorService.
	 * Images are generated only if they do not already exist or if forceOverride is enabled.
	 *
	 * @param vConfs A map of vertical configurations.
	 */
	private void generateImagesForVerticals(Map<String, VerticalConfig> vConfs) {
		vConfs.values().forEach(vc -> executorService.submit(() -> {
			String fileName = vc.getId() + ".png";
			if (!imageGenerationService.shouldGenerateImage(fileName)) {
				logger.info("Image for vertical {} already exists with file name {}. Skipping generation.", vc.getId(), fileName);
				return;
			}

			String threadName = Thread.currentThread().getName();
			logger.info("Starting image generation for vertical {} in thread {}", vc.getId(), threadName);

			try {
				String verticalTitle = vc.getI18n().get("default").getVerticalHomeTitle();
				imageGenerationService.fullGenerate(verticalTitle, fileName);
				logger.info("Generated and saved image for vertical {} with file name {} in thread {}", vc.getId(), fileName, threadName);
			} catch (Exception e) {
				logger.error("Failed to generate or save image for vertical {} in thread {}", vc.getId(), threadName, e);
			}
		}));
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

}
