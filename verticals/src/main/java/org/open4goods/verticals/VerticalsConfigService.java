package org.open4goods.verticals;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import org.open4goods.model.attribute.AttributeType;
import org.open4goods.model.vertical.AttributeComparisonRule;
import org.open4goods.model.vertical.AttributeConfig;
import org.open4goods.model.vertical.AttributeParserConfig;
import org.open4goods.model.vertical.AttributesConfig;
import org.open4goods.model.vertical.ImpactScoreConfig;
import org.open4goods.model.vertical.NudgeToolConfig;
import org.open4goods.model.vertical.NudgeToolScore;
import org.open4goods.model.vertical.NudgeToolSubsetGroup;
import org.open4goods.model.vertical.Order;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.model.vertical.VerticalSubset;
import org.open4goods.services.serialisation.exception.SerialisationException;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;

import com.fasterxml.jackson.databind.ObjectReader;

import jakarta.annotation.PreDestroy;

/**
 * This service is in charge to provide the Verticals configurations.
 * Configurations are provided from the classpath AND from a git specific
 * project (fresh local clone on app startup)
 *
 * @author goulven
 *
 */
public class VerticalsConfigService {

	public static final Logger logger = LoggerFactory.getLogger(VerticalsConfigService.class);

	private static final String DEFAULT_CONFIG_FILENAME = "_default.yml";

	private static final String CLASSPATH_VERTICALS = "classpath:/verticals/*.yml";
	private static final String CLASSPATH_VERTICALS_DEFAULT = "classpath:/verticals/_default.yml";
	private static final String CLASSPATH_ATTRIBUTES = "classpath:/attributes/*.yml";

	private SerialisationService serialisationService;

	private final Map<String, VerticalConfig> configs = new ConcurrentHashMap<>(100);

	// The cache of categories to verticalconfig association. datasource (or all) ->
	// category -> VerticalConfig
	private final Map<String, Map<String, VerticalConfig>> categoriesToVertical = new ConcurrentHashMap<>();

	private Map<String, VerticalConfig> byUrl = new HashMap<>();

	private Map<String, String> toLang = new HashMap<>();

	private Map<Integer, VerticalConfig> byTaxonomy = new HashMap<>();

	private Map<String, AttributeConfig> attributeCatalog = new HashMap<>();

	private ResourcePatternResolver resourceResolver;

	//
	private final ExecutorService executorService = Executors.newFixedThreadPool(1);

	// The default config
	private VerticalConfig defaultConfig;

	public VerticalsConfigService(SerialisationService serialisationService,
			GoogleTaxonomyService googleTaxonomyService, ResourcePatternResolver resourceResolver) {
		super();
		this.serialisationService = serialisationService;
		this.resourceResolver = resourceResolver;

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
	// @Scheduled(fixedDelay = 1000 * 60 * 10, initialDelay = 1000 * 60 * 10)
	public synchronized void loadConfigs() {

		Map<String, VerticalConfig> vConfs = new ConcurrentHashMap<>(100);

		attributeCatalog = loadAttributeCatalog();
		byUrl.clear();
		toLang.clear();
		byTaxonomy.clear();

		// Setting the default config
		try {
			Resource r = resourceResolver.getResource(CLASSPATH_VERTICALS_DEFAULT);
			defaultConfig = serialisationService.fromYaml(r.getInputStream(), VerticalConfig.class);
			resolveAttributeConfigs(defaultConfig);
		} catch (Exception e) {
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
					logger.error("Error loading categories matching map : {}", c, e);
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
				VerticalConfig config = getConfig(r.getInputStream(), getDefaultConfig());
				if (config.getImpactScoreConfig() == null) {
					logger.warn("Vertical {} (from {}) has a NULL impact score configuration. It might not behave as expected.",
							config.getId(), r.getFilename());
				}

				// Check if ID is unique in the current list
				boolean exists = ret.stream().anyMatch(v -> v.getId().equals(config.getId()));
				if (exists) {
					logger.error("DUPLICATE VERTICAL ID DETECTED: '{}' is already defined in another file. Ignoring definition from '{}'.",
							config.getId(), r.getFilename());
					continue;
				}

				ret.add(config);
			} catch (Exception e) {
				logger.error("Cannot retrieve vertical config : {}", r.getFilename(), e);
			}
		}

		return ret;
	}

	private Map<String, AttributeConfig> loadAttributeCatalog() {
		Map<String, AttributeConfig> attributes = new HashMap<>();
		Resource[] resources;
		try {
			resources = resourceResolver.getResources(CLASSPATH_ATTRIBUTES);
		} catch (IOException e) {
			throw new IllegalStateException("Cannot load attributes from " + CLASSPATH_ATTRIBUTES, e);
		}

		for (Resource resource : resources) {
			try (InputStream inputStream = resource.getInputStream()) {
				AttributeConfig attributeConfig = serialisationService.fromYaml(inputStream, AttributeConfig.class);
				if (attributeConfig == null || attributeConfig.getKey() == null) {
					throw new IllegalStateException(
							"Attribute config defined in " + resource.getFilename() + " has no key");
				}

				if (attributes.containsKey(attributeConfig.getKey())) {
					logger.warn("Duplicate attribute config detected for key {}. Keeping last declared instance.",
							attributeConfig.getKey());
				}

				attributes.put(attributeConfig.getKey(), attributeConfig);
			} catch (Exception e) {
				throw new IllegalStateException("Cannot load attribute config from " + resource.getFilename(), e);
			}
		}

		return attributes;
	}

	/**
	 * Instanciate a config with a previously defaulted one
	 *
	 * @param inputStream
	 * @param defaul
	 * @return
	 * @throws IOException
	 */
	public VerticalConfig getConfig(InputStream inputStream, VerticalConfig defaul)
			throws SerialisationException, IOException {

		// TODO(p3,perf) : chould be cached
		VerticalConfig copy = serialisationService.fromYaml(serialisationService.toYaml(defaul), VerticalConfig.class);
		ObjectReader objectReader = serialisationService.getYamlMapper().readerForUpdating(copy);
		VerticalConfig ret = objectReader.readValue(inputStream);
		inputStream.close();
		mergeDefaults(defaul, ret);
		return resolveAttributeConfigs(ret);
	}

	private void mergeDefaults(VerticalConfig defaults, VerticalConfig config) {
		if (defaults == null || config == null) {
			return;
		}

		if (defaults.getI18n() != null) {
			if (config.getI18n() == null) {
				config.setI18n(new HashMap<>());
			}
			defaults.getI18n().forEach(config.getI18n()::putIfAbsent);
		}

		config.setAvailableImpactScoreCriterias(
				mergeStringList(defaults.getAvailableImpactScoreCriterias(), config.getAvailableImpactScoreCriterias()));
		config.setExcludingTokensFromCategoriesMatching(mergeStringSet(defaults.getExcludingTokensFromCategoriesMatching(),
				config.getExcludingTokensFromCategoriesMatching()));
		config.setGenerationExcludedFromCategoriesMatching(mergeStringSet(defaults.getGenerationExcludedFromCategoriesMatching(),
				config.getGenerationExcludedFromCategoriesMatching()));
		config.setGenerationExcludedFromAttributesMatching(mergeStringSet(defaults.getGenerationExcludedFromAttributesMatching(),
				config.getGenerationExcludedFromAttributesMatching()));
		config.setRequiredAttributes(
				mergeStringSet(defaults.getRequiredAttributes(), config.getRequiredAttributes()));
		config.setBrandsAlias(mergeMap(defaults.getBrandsAlias(), config.getBrandsAlias()));
		config.setSubsets(mergeByKey(defaults.getSubsets(), config.getSubsets(), VerticalSubset::getId));

		ImpactScoreConfig impactScoreConfig = config.getImpactScoreConfig();
		ImpactScoreConfig defaultImpactScoreConfig = defaults.getImpactScoreConfig();
		if (impactScoreConfig != null && defaultImpactScoreConfig != null) {
			if (impactScoreConfig.getMinDistinctValuesForSigma() == null) {
				impactScoreConfig.setMinDistinctValuesForSigma(defaultImpactScoreConfig.getMinDistinctValuesForSigma());
			}
		}

		AttributesConfig attributesConfig = config.getAttributesConfig();
		AttributesConfig defaultAttributesConfig = defaults.getAttributesConfig();
		if (attributesConfig != null && defaultAttributesConfig != null) {
			attributesConfig.setConfigs(mergeByKey(defaultAttributesConfig.getConfigs(), attributesConfig.getConfigs(),
					AttributeConfig::getKey));
			attributesConfig.setFeaturedValues(
					mergeStringSet(defaultAttributesConfig.getFeaturedValues(), attributesConfig.getFeaturedValues()));
			attributesConfig.setExclusions(
					mergeStringSet(defaultAttributesConfig.getExclusions(), attributesConfig.getExclusions()));
		}

		NudgeToolConfig nudgeToolConfig = config.getNudgeToolConfig();
		NudgeToolConfig defaultNudgeToolConfig = defaults.getNudgeToolConfig();
		if (defaultNudgeToolConfig != null) {
			if (nudgeToolConfig == null) {
				nudgeToolConfig = new NudgeToolConfig();
				config.setNudgeToolConfig(nudgeToolConfig);
			}

			nudgeToolConfig.setScores(mergeByKey(defaultNudgeToolConfig.getScores(), nudgeToolConfig.getScores(),
					NudgeToolScore::getScoreName));
			nudgeToolConfig.setSubsets(mergeByKey(defaultNudgeToolConfig.getSubsets(), nudgeToolConfig.getSubsets(),
					VerticalSubset::getId));
			nudgeToolConfig.setSubsetGroups(mergeByKey(defaultNudgeToolConfig.getSubsetGroups(),
					nudgeToolConfig.getSubsetGroups(), NudgeToolSubsetGroup::getId));
		}
	}

	private static <T> List<T> mergeByKey(List<T> defaults, List<T> overrides, Function<T, String> keyExtractor) {
		if (defaults == null && overrides == null) {
			return null;
		}
		LinkedHashMap<String, T> merged = new LinkedHashMap<>();
		addToMerge(merged, defaults, keyExtractor);
		addToMerge(merged, overrides, keyExtractor);
		return new ArrayList<>(merged.values());
	}

	private static <T> void addToMerge(LinkedHashMap<String, T> merged, List<T> values, Function<T, String> keyExtractor) {
		if (values == null) {
			return;
		}
		int index = 0;
		for (T value : values) {
			if (value == null) {
				index++;
				continue;
			}
			String key = keyExtractor.apply(value);
			if (key == null || key.isBlank()) {
				key = "__index__" + merged.size() + "_" + index;
			}
			merged.put(key, value);
			index++;
		}
	}

	private static List<String> mergeStringList(List<String> defaults, List<String> overrides) {
		if (defaults == null && overrides == null) {
			return null;
		}
		LinkedHashSet<String> merged = new LinkedHashSet<>();
		if (defaults != null) {
			merged.addAll(defaults);
		}
		if (overrides != null) {
			merged.addAll(overrides);
		}
		return new ArrayList<>(merged);
	}

	private static Set<String> mergeStringSet(Set<String> defaults, Set<String> overrides) {
		if (defaults == null && overrides == null) {
			return null;
		}
		LinkedHashSet<String> merged = new LinkedHashSet<>();
		if (defaults != null) {
			merged.addAll(defaults);
		}
		if (overrides != null) {
			merged.addAll(overrides);
		}
		return merged;
	}

	private static <K, V> Map<K, V> mergeMap(Map<K, V> defaults, Map<K, V> overrides) {
		if (defaults == null && overrides == null) {
			return null;
		}
		LinkedHashMap<K, V> merged = new LinkedHashMap<>();
		if (defaults != null) {
			merged.putAll(defaults);
		}
		if (overrides != null) {
			merged.putAll(overrides);
		}
		return merged;
	}

	private VerticalConfig resolveAttributeConfigs(VerticalConfig config) {
		if (config == null || config.getAttributesConfig() == null
				|| config.getAttributesConfig().getConfigs() == null) {
			return config;
		}

		List<AttributeConfig> resolved = new ArrayList<>();
		for (AttributeConfig attributeConfig : config.getAttributesConfig().getConfigs()) {
			AttributeConfig resolvedConfig = resolveAttributeConfig(attributeConfig);
			if (resolvedConfig != null && resolvedConfig.getName() == null) {
				logger.error(
						"Invalid configuration for attribute '{}' in vertical '{}': Name is missing. This will cause issues in frontend.",
						resolvedConfig.getKey(), config.getId());
			}
			resolved.add(resolvedConfig);
		}
		config.getAttributesConfig().setConfigs(resolved);
		return config;
	}

	private AttributeConfig resolveAttributeConfig(AttributeConfig attributeConfig) {
		if (attributeConfig == null) {
			return null;
		}

		if (!isKeyOnly(attributeConfig)) {
			return attributeConfig;
		}

		AttributeConfig catalogConfig = attributeCatalog.get(attributeConfig.getKey());
		if (catalogConfig == null) {
			throw new IllegalStateException("Missing attribute definition for key " + attributeConfig.getKey());
		}

		return serialisationService.getYamlMapper().convertValue(catalogConfig, AttributeConfig.class);
	}

	private boolean isKeyOnly(AttributeConfig attributeConfig) {
		if (attributeConfig == null || attributeConfig.getKey() == null) {
			return false;
		}

		AttributeParserConfig parser = attributeConfig.getParser();
		return attributeConfig.getName() == null
				&& attributeConfig.getUnit() == null
				&& attributeConfig.getSuffix() == null
				&& (attributeConfig.getSynonyms() == null || attributeConfig.getSynonyms().isEmpty())
				&& attributeConfig.getIcecatFeaturesIds().isEmpty()
				&& attributeConfig.getEprelFeatureNames().isEmpty()
				&& attributeConfig.getNumericMapping().isEmpty()
				&& attributeConfig.getMappings().isEmpty()
				&& (attributeConfig.getFaIcon() == null || "fa-wrench".equals(attributeConfig.getFaIcon()))
				&& (attributeConfig.getBetterIs() == null
						|| AttributeComparisonRule.GREATER.equals(attributeConfig.getBetterIs()))
				&& (attributeConfig.getFilteringType() == null
						|| AttributeType.TEXT.equals(attributeConfig.getFilteringType()))
				&& !attributeConfig.isAsScore()
				&& (attributeConfig.getAttributeValuesOrdering() == null
						|| Order.COUNT.equals(attributeConfig.getAttributeValuesOrdering()))
				&& (attributeConfig.getAttributeValuesReverseOrder() == null
						|| Boolean.FALSE.equals(attributeConfig.getAttributeValuesReverseOrder()))
				&& hasDefaultParser(parser);
	}

	private boolean hasDefaultParser(AttributeParserConfig parser) {
		AttributeParserConfig defaults = new AttributeParserConfig();
		if (parser == null) {
			return true;
		}

		return Objects.equals(parser.getNormalize(), defaults.getNormalize())
				&& Objects.equals(parser.getTrim(), defaults.getTrim())
				&& Objects.equals(parser.getLowerCase(), defaults.getLowerCase())
				&& Objects.equals(parser.getUpperCase(), defaults.getUpperCase())
				&& parser.isRemoveParenthesis() == defaults.isRemoveParenthesis()
				&& Objects.equals(parser.getClazz(), defaults.getClazz())
				&& (parser.getDeleteTokens() == null || parser.getDeleteTokens().isEmpty())
				&& (parser.getTokenMatch() == null || parser.getTokenMatch().isEmpty());
	}

	/**
	 * Instanciate a vertical config for a given categories bag
	 *
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
		if (null != vc && null != vc.getExcludingTokensFromCategoriesMatching()) {
			for (String token : vc.getExcludingTokensFromCategoriesMatching()) {
				for (String category : productCategoriessByDatasource.values()) {
					if (category.contains(token)) {
						logger.warn("Excluded from matching category {} because categories contains word {}",
								vc.getId(), token, category);
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
	 *
	 * @param path
	 * @return
	 */
	public VerticalConfig getVerticalForPath(String path) {
		return byUrl.get(path);
	}

	/**
	 * Return the path for a vertical language, if any
	 *
	 * @param config
	 * @param path
	 * @return
	 */
	public String getPathForVerticalLanguage(String language, VerticalConfig config) {
		String path = configs.get(config.getId()).i18n(language).getVerticalHomeUrl();
		return path;
	}

	/**
	 * Splits the VerticalConfig objects into buckets of a specified size, limiting
	 * the total number of buckets.
	 * This is UI HELPER METHOD
	 *
	 * @param bucketSize the size of each bucket (number of VerticalConfig objects
	 *                   per bucket).
	 * @param maxBucket  the maximum number of buckets to return.
	 * @return a list of buckets, where each bucket is a list of VerticalConfig
	 *         objects.
	 */
	// TODO(p1, perf) : cache
	public List<List<VerticalConfig>> getImpactScoreVerticalsByBuckets(int bucketSize, int maxBucket) {
		// Get the map of VerticalConfig objects
		Map<String, VerticalConfig> theConfigs = getConfigs();

		// Create a list to hold all VerticalConfig objects
		List<VerticalConfig> configList = new ArrayList<>(getConfigsWithoutDefault());

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
	 *
	 * @param icecatCategoryId
	 *                         TODO(p2,perf) : Maintain a map for key/val access
	 * @return
	 */
	public VerticalConfig getByIcecatCategoryId(Integer icecatCategoryId) {
		return configs.values().stream().filter(e -> icecatCategoryId.equals(e.getIcecatTaxonomyId())).findFirst()
				.orElse(null);
	}

	/**
	 * Return a config by it's google taxonomy id
	 *
	 * @param icecatCategoryId
	 * @return
	 */
	public VerticalConfig getByGoogleTaxonomy(Integer googleCategoryId) {
		return configs.values().stream().filter(e -> googleCategoryId.equals(e.getGoogleTaxonomyId())).findFirst()
				.orElse(null);
	}

	/**
	 * Return a vertical config for a given taxonomy id
	 *
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
	 *
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
	 * @return all configs, except the _default. Allows filtering on enabled
	 *         verticals,
	 *         and returns the list ordered by the VerticalConfig.order field.
	 */
	// TODO (p1, perf): cache
	public List<VerticalConfig> getConfigsWithoutDefault(boolean onlyEnabled) {
		return getConfigs().values().stream()
				.filter(config -> !onlyEnabled || config.isEnabled())
				.sorted(Comparator.comparingInt(VerticalConfig::getOrder))
				.toList();
	}

	/**
	 *
	 * @return all configs, except the _default.
	 */
	public Collection<VerticalConfig> getConfigsWithoutDefault() {
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
	 * Shuts down the ExecutorService gracefully, waiting for existing tasks to
	 * complete.
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
