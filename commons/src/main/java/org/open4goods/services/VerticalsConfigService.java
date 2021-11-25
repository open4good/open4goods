package org.open4goods.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.open4goods.config.yml.ui.VerticalConfig;
import org.open4goods.model.constants.CacheConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.scheduling.annotation.Scheduled;

import com.fasterxml.jackson.databind.ObjectReader;
import com.google.common.base.Optional;

/**
 * This service is in charge to provide the Verticals configurations.
 * Configurations are provided from the classpath AND from a git specific
 * project (fresh local clone on app startup)
 * 
 * @author goulven
 *
 */
public class VerticalsConfigService {

	public static final String MAIN_VERTICAL_NAME = "all";

	public static final Logger logger = LoggerFactory.getLogger(VerticalsConfigService.class);

	private static final String DEFAULT_CONFIG_FILENAME = "_default.yml";
	private static final String CLASSPATH_VERTICALS = "classpath:**verticals/*.yml";
	private static final String CLASSPATH_VERTICAL_PREFIX = "classpath:verticals/";

	private SerialisationService serialisationService;

	private Map<String, VerticalConfig> configs = new ConcurrentHashMap<>(100);

	private String verticalsFolder;

	@Autowired
	public VerticalsConfigService(SerialisationService serialisationService, String verticalsFolder) {
		super();
		this.serialisationService = serialisationService;
		this.verticalsFolder = verticalsFolder;

		// initial configs loads
		loadConfigs();

	}

	/**
	 * Loads configs from classpath and from giit repositories. Thread safe
	 * operation, so can be used in refresh
	 */
	@Scheduled(fixedDelay = 1000 * 60 * 10, initialDelay = 1000 * 60 * 10)
	public synchronized void loadConfigs() {

		Map<String, VerticalConfig> configs2 = new ConcurrentHashMap<>(100);

		/////////////////////////////////////////
		// Load configurations from classpath
		/////////////////////////////////////////

		for (VerticalConfig uc : getFolderVerticalConfigs()) {
			logger.info("Adding config {} from classpath", uc.getId());
			configs2.put(uc.getId(), uc);
		}
		// Switching confs
		synchronized (configs) {
			configs.clear();
			configs.putAll(configs2);
		}
	}

	/**
	 * 
	 * @return the available verticals configurations from classpath
	 */
	private List<VerticalConfig> getFolderVerticalConfigs() {
		List<VerticalConfig> ret = new ArrayList<>();

		File verticalFolder = new File(verticalsFolder);

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

	/**
	 * 
	 * @return
	 * @throws IOException
	 */
	@Cacheable(cacheNames = CacheConstants.FOREVER_LOCAL_CACHE_NAME)
	public VerticalConfig getDefaultConfig() throws IOException {
		FileInputStream f = new FileInputStream(new File(verticalsFolder + "/" + DEFAULT_CONFIG_FILENAME));
		VerticalConfig ret = serialisationService.fromYaml(f, VerticalConfig.class);
		f.close();
		return ret;
	}

	/**
	 * Return a config by it's Id
	 * 
	 * @param verticalName
	 * @return
	 */
	public Optional<VerticalConfig> getConfigById(String verticalName) {
		return Optional.of(configs.get(verticalName));
	}

	/**
	 * 
	 * @return all Vertical configs
	 */
	public Map<String, VerticalConfig> getConfigs() {
		return configs;
	}

}
