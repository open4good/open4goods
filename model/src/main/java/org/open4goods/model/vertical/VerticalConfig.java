package org.open4goods.model.vertical;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.open4goods.model.StandardiserService;
import org.open4goods.model.constants.CacheConstants;
import org.open4goods.model.constants.UrlConstants;
import org.open4goods.model.helper.IdHelper;
import org.open4goods.model.product.Score;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;

import com.fasterxml.jackson.annotation.JsonMerge;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Le parametrage Yaml d'une verticale. Celui ci dispose soit de propriétés de
 * premier niveau, soit de sous-objets de configuration.
 *
 * @author goulven
 *
 */

public class VerticalConfig {

	private static final Logger LOGGER = LoggerFactory.getLogger(VerticalConfig.class);

	/**
	 * Vertical ID
	 */
	private String id;

	/**
	 * The corresponding google taxonomy ID
	 */
	private Integer googleTaxonomyId;

	/**
	 * The corresponding icecat taxonomy ID
	 */
	private Integer icecatTaxonomyId;

	/**
	 * The corresponding eprel taxonomy ID
	 */
	private String eprelGroupName;

	/**
	 * If true, then the vertical is handled through batch processing, but is not
	 * exposed on UI / sitemap.
	 **/
	private boolean enabled = false;

	/** Flag indicating whether this category should be highlighted as popular. */
	private boolean popular = false;

	/**
	 * Absolute path to the vertical image (should be hosted on nudger to avoid
	 * recaching failures). Provide a large one, thumnails will be generated
	 * automaticaly. MUST be PNG or JPG
	 */
	@JsonMerge
	@NotBlank
	private String verticalImage;

	/**
	 * The product url, metas title, description, ....
	 */
	@JsonMerge
	private Map<String, ProductI18nElements> i18n = new HashMap<>();

//	@JsonMerge
//	private GenAiConfig genAiConfig = new GenAiConfig();

	/**
	 * The list of filters to be added to the ecological filters group
	 */
	private List<String> ecoFilters = new ArrayList<>();

	/**
	 * The list of filters to be added to the technical filters group
	 */
	private List<String> technicalFilters = new ArrayList<>();

	/**
	 * The list of attributes considered popular for frontend rendering.
	 */
	private List<String> popularAttributes = new ArrayList<>();

	/**
	 * The list of global technicals filters (weight, ..)
	 */
	private List<String> globalTechnicalFilters = new ArrayList<>();

	/**
	 * The categories that MUST BE PRESENT to associate to this vertical. Prefixed
	 * by datasource on which it applies, using "all" for all datasources
	 */

	private Map<String, Set<String>> matchingCategories = new HashMap<>();

	/**
	 * A list a words that will exclude the item from the category if encountered
	 */
	@JsonMerge
	private Set<String> excludingTokensFromCategoriesMatching = new HashSet<String>();

	/**
	 * The set of datasourcenames that will be excluded in generation of categories
	 * matching
	 */
	@JsonMerge
	private Set<String> generationExcludedFromCategoriesMatching = new HashSet<String>();

	/**
	 * The set of attributes names that will be excluded in generation of attributes
	 * suggestion
	 */

	@JsonMerge
	private Set<String> generationExcludedFromAttributesMatching = new HashSet<String>();

	/**
	 * The categories that MUST NOT BE PRESENT to associate to this vertical
	 */
//	private Set<String> unmatchingCategories = new HashSet<>();

	/**
	 * The attributes that must be present. If not, the product will have excluded
	 * set to true
	 */
	@JsonMerge
	private Set<String> requiredAttributes = new HashSet<String>();

	/**
	 * if true, will override generated url name, even if already generated
	 */
	private boolean forceNameGeneration = false;

	@JsonMerge
	/**
	 * Brand alias mappings (eg : LG ELECTRONICS : LG)
	 */
	private Map<String, String> brandsAlias = new HashMap<>();

	/**
	 * The brands that must be removes (eg. NON COMMUNIQUE)
	 */
	private Set<String> brandsExclusion = new HashSet<String>();

	/**
	 * The order of this vertical, for restitution
	 */

	private Integer order = Integer.MAX_VALUE;

	/**
	 * The I18n URL Mappings. Think SEO !
	 */
	@JsonMerge
	private SiteNaming namings = new SiteNaming();

	/**
	 * The behavior of resource aggregation
	 */
	@JsonMerge
	private ResourcesAggregationConfig resourcesConfig = new ResourcesAggregationConfig();

	/**
	 * Configuration relativ to attributes aggregation
	 */
	@JsonMerge
	private AttributesConfig attributesConfig = new AttributesConfig();

	/**
	 * Preferred aggregation parameters for attributes and impact scores. The map is
	 * indexed by the document mapping (e.g. {@code attributes.indexed.WEIGHT}) used
	 * by Elasticsearch so the same configuration can be reused across attributes
	 * and scores exposed by the API.
	 */
	@JsonMerge
	private Map<String, AggregationConfiguration> aggregationConfiguration = new HashMap<>();

	/**
	 * The scores that are eligible to participate to the impact score construction
	 */
	@JsonMerge
	private Map<String, ImpactScoreCriteria> availableImpactScoreCriterias = new HashMap<>();

	/**
	 * Configuration relativ to ecoscore computation. Key / values are : scoreName
	 * -> Ponderation (0.1 = 10%) NOTE : No json merge, since default score has to
	 * be fully described if overrided
	 */
	private ImpactScoreConfig impactScoreConfig = new ImpactScoreConfig();

	////////////////////
	// The subsets for this vertical
	////////////////////

	/**
	 * The vertical custom subsets
	 */
	@JsonMerge
	private List<VerticalSubset> subsets = new ArrayList<VerticalSubset>();

	/**
	 * The subset dedicated to brands; Technical, no yaml def needed TODO(p2,
	 * design) : move outside
	 */
	private VerticalSubset brandsSubset = new VerticalSubset();

//
//	/**
//	 * Configuration relativ to ratings aggregation
//	 */
//	@JsonMerge
//	private RatingsConfig ratingsConfig = new RatingsConfig();

	/**
	 * The behavior of barcode generation
	 */
	@JsonMerge
	private BarcodeAggregationProperties barcodeConfig = new BarcodeAggregationProperties();

	/**
	 * The recommandations configuration
	 */
	private RecommandationsConfig recommandationsConfig = new RecommandationsConfig();

	/**
	 * The behaviour of descriptions aggregation
	 */
	private DescriptionsAggregationConfig descriptionsAggregationConfig = new DescriptionsAggregationConfig();

	/**
	 * The product scoring configuration
	 */
	@NotNull
	private ScoringAggregationConfig scoringAggregationConfig;

	/**
	 * The feature groups, to order / render attributes
	 */
	private List<FeatureGroup> featureGroups = new ArrayList<>();

	// A local cache for token names
	private Set<String> cacheTokenNames;

	/**
	 * If an item has a score is the last "worseLimit", then it will be bagged in
	 * product.worsesScores
	 */
	private Integer worseLimit = 3;

	/**
	 * If an item has a score is the top "bettersLimit", then it will be bagged in
	 * product.bestsScores
	 */
	private Integer bettersLimit = 3;

	@Override
	public String toString() {
		return "config:" + id;
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return id.equals(((VerticalConfig) obj).id);
	}

	public ProductI18nElements i18n(String lang) {
		return i18n.getOrDefault(lang, i18n.get("default"));
	}

	/**
	 * Groups VerticalSubset objects into a LinkedHashMap, where the key is the
	 * group name and the value is the list of VerticalSubset objects for that
	 * group. The order of the keys in the map matches the order of appearance of
	 * the group values in the input list.
	 *
	 * @param subsets The input list of VerticalSubset objects to group.
	 * @return A LinkedHashMap where the key is the group name and the value is the
	 *         list of VerticalSubset objects.
	 */
	public LinkedHashMap<String, List<VerticalSubset>> getSubsetGroups() {
		// Use LinkedHashMap to preserve the order of appearance of groups
		return subsets.stream().collect(Collectors.groupingBy(VerticalSubset::getGroup, LinkedHashMap::new, // Ensures order of keys matches order of appearance
				Collectors.toList()));
	}

	/**
	 * Compute the token names, used to do a product.offernames matching categories
	 *
	 * @param additionalNames
	 * @return
	 */
	public Set<String> getTokenNames(Collection<String> additionalNames) {

		if (null == cacheTokenNames) {

			Set<String> verticalNames = new HashSet<String>();
			verticalNames.add(getId());
			verticalNames.addAll(additionalNames);

			getI18n().entrySet().forEach(e -> {
				// verticalNames.add(e.getValue().getH1Title().getPrefix().toLowerCase());
				verticalNames.add(e.getValue().getVerticalHomeUrl());
				// verticalNames.add(e.getValue().getUrl().getPrefix());

			});

			// Adding singular derivativs, removing -
			Set<String> derivativs = new HashSet<String>();
			verticalNames.forEach(e -> {
				String derivativ = e.replace("-", " ");
				derivativs.add(derivativ);

				if (derivativ.endsWith("s")) {
					derivativ = derivativ.substring(0, derivativ.length() - 1);
				}
				derivativs.add(derivativ);

				// If composed word (laves linges"
				derivativ = derivativ.replace("s ", " ");
				derivativs.add(derivativ);

			});

			verticalNames.addAll(derivativs);
			cacheTokenNames = verticalNames;
		}
		return cacheTokenNames;
	}

	/**
	 * Return the participation in percentage of a score in the ecoscore
	 *
	 * @param scoreName
	 * @return
	 */
	public Integer ecoscorePercentOf(String scoreName) {

		Double ponderation = impactScoreConfig.getCriteriasPonderation().get(scoreName);
		if (null == ponderation) {
			return -1;
		} else {
			return (int) Math.round(ponderation * 100);
		}
	}

	/**
	 * Return the participation in points of the score
	 *
	 * @param scoreName
	 * @return
	 */
	public Double ecoscoreParticipationPointsOf20(String scoreName, Double relValue) {

		return ecoscoreParticipationMaxPointsOf20(scoreName) * relValue / StandardiserService.DEFAULT_MAX_RATING;
	}

	/**
	 * Return the max participation points of the score
	 *
	 * @param scoreName
	 * @return
	 */
	public Double ecoscoreParticipationMaxPointsOf20(String scoreName) {
		return Double.valueOf(ecoscorePercentOf(scoreName)) / 5;
	}

	/**
	 *
	 * @return the specific attributes config for this vertical
	 */
	public List<AttributeConfig> verticalFilters() {

		return getVerticalFilters().stream()

				.map(e -> getAttributesConfig().getAttributeConfigByKey(e)).filter(Objects::nonNull).collect(Collectors.toList());

	}

	/**
	 *
	 * @return all attributes filters (eco & technical)
	 */
	public List<String> getVerticalFilters() {
		List<String> filters = new ArrayList<String>();

		if (null != ecoFilters) {
			filters.addAll(ecoFilters);
		}

		if (null != technicalFilters) {
			filters.addAll(technicalFilters);
		}
		filters.addAll(globalTechnicalFilters);
		return filters;

	}

	/**
	 * Return the root url for a given sitelocale, with the "default" behavior
	 *
	 * @param siteLocale
	 * @return
	 */
	public String getBaseUrl(final Locale siteLocale) {
		return i18n.getOrDefault(siteLocale.getLanguage(), i18n.get("default")).getVerticalHomeUrl();
	}

	/**
	 *
	 * @return the sum of the coefficient applyed to this ecoscore
	 */
	public Double getEcoscoreCriteriasSum() {
		return impactScoreConfig.getCriteriasPonderation().values().stream().mapToDouble(Double::doubleValue).sum();
	}

	public String getBaseUrl(final String siteLocale) {
		return i18n.getOrDefault(siteLocale, i18n.get("default")).getVerticalHomeUrl();
	}

	/**
	 * Retrieves the locale for the site through the request.
	 *
	 * @param key the key
	 * @return the language by server name
	 */
	@Cacheable(keyGenerator = CacheConstants.KEY_GENERATOR, cacheNames = CacheConstants.FOREVER_LOCAL_CACHE_NAME)
	// TODO(perf) : check effectiv caching
	public String getLanguageByServerName(final String key) {
		HashMap<String, String> hashedSiteLocales = null;
		// Caching if needed
		if (null == hashedSiteLocales) {
			LOGGER.info("Hashing languageByServerName");
			hashedSiteLocales = new HashMap<>();
			for (final Entry<String, String> a : getNamings().getServerNames().entrySet()) {

				if (hashedSiteLocales.containsKey(a.getValue())) {
					LOGGER.error("Duplicate server name for : {} server will be ignored.", a.getKey());
				}
				hashedSiteLocales.put(a.getValue(), a.getKey());
			}
		}
		return hashedSiteLocales.getOrDefault(key, "default");
	}

	/**
	 * Return the site locale, given the user request and the config
	 *
	 * @param request
	 * @return
	 */
	// TODO(perf) : caching on request.getservername
	public Locale getSiteLocaleOrDefault(final HttpServletRequest request) {
		Locale l;
		try {
			final String lang = getLanguageByServerName(request.getServerName());
			l = lang.equals("default") ? Locale.ENGLISH : Locale.forLanguageTag(lang.toUpperCase());
		} catch (final Exception e) {
			LOGGER.warn("Error resolving site locale {}", e.getMessage());
			l = Locale.getDefault();
		}
		return l;
	}

	/**
	 * Return the name of the product index for this vertical
	 *
	 * @return
	 */
	// TODO : Local cache, better, store as attribute
	public String indexName() {
		return "vertical-" + IdHelper.azCharAndDigits(id, "-").toLowerCase();
	}

	public String getSiteLocaleAsStringOrDefault(final HttpServletRequest request) {
		return getLanguageByServerName(request.getServerName());
	}

	/**
	 * Return the base server for a given sitelocale, with the "default" behavior
	 *
	 * @param siteLocale
	 * @return
	 */
	public String getServerName(final String siteLocale) {
		return namings.getServerNames().getOrDefault(siteLocale, namings.getServerNames().get("default"));
	}

	public String topProductsEndPoint() {
		return UrlConstants.API_TOP_PRODUCTS;
	}

	public String getRecommandationFilterFor(final String id) {

		for (final RecommandationCriteria rc : recommandationsConfig.getRecommandations()) {
			for (final RecommandationChoice rf : rc.getChoices()) {
				if (rf.name().equals(id)) {
					return rf.queryFragment();
				}
			}
		}

		return "";
	}

	/**
	 * Ui Helper : return the ecoscore composing score, allowing filtering on
	 * provided (existing) score
	 *
	 * @param existing
	 * @return
	 */
	public List<Score> ecoScoreDetails(Collection<Score> existing) {

		List<Score> ret = new ArrayList<Score>();

		impactScoreConfig.getCriteriasPonderation().keySet().forEach(ecoConfig -> {
			existing.forEach(exisiting -> {
				if (exisiting.getName().equals(ecoConfig)) {
					ret.add(exisiting);
				}
			});
		});
		return ret;
	}

	/**
	 * Return the name of the resource index for this capsule
	 *
	 * @return
	 */
	public String resourceIndex() {

		return indexName() + "-resources";
	}

	// TODO : Perf, could use a cached map
	public FeatureGroup getOrCreateByIceCatCategoryFeatureGroup(int categoryFeatureGroupId) {

		Optional<FeatureGroup> existing = featureGroups.stream().filter(e -> e.getIcecatCategoryFeatureGroupId() == categoryFeatureGroupId).findFirst();
		if (existing.isPresent()) {
			return existing.get();
		} else {
			FeatureGroup fg = new FeatureGroup(categoryFeatureGroupId);
			featureGroups.add(fg);
			return fg;
		}
//
//
//		return .orElse(new FeatureGroup(categoryFeatureGroupId));
	}

	//////////////////////////////////////
	// Getters / Setters
	//////////////////////////////////////

	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public SiteNaming getNamings() {
		return namings;
	}

	public void setNamings(final SiteNaming urls) {
		namings = urls;
	}

	public ResourcesAggregationConfig getResourcesConfig() {
		return resourcesConfig;
	}

	public void setResourcesConfig(ResourcesAggregationConfig resourcesConfig) {
		this.resourcesConfig = resourcesConfig;
	}

	public AttributesConfig getAttributesConfig() {
		return attributesConfig;
	}

	public void setAttributesConfig(AttributesConfig attributesConfig) {
		this.attributesConfig = attributesConfig;
	}

	public Map<String, AggregationConfiguration> getAggregationConfiguration() {
		return aggregationConfiguration;
	}

	public void setAggregationConfiguration(Map<String, AggregationConfiguration> aggregationConfiguration) {
		this.aggregationConfiguration = aggregationConfiguration;
	}

	public AggregationConfiguration getAggregationConfigurationFor(String key) {
		if (aggregationConfiguration == null || aggregationConfiguration.isEmpty() || key == null) {
			return null;
		}
		String normalizedKey = key.trim();
		if (normalizedKey.isEmpty()) {
			return null;
		}
		AggregationConfiguration directMatch = aggregationConfiguration.get(normalizedKey);
		if (directMatch != null) {
			return directMatch;
		}
		for (Entry<String, AggregationConfiguration> entry : aggregationConfiguration.entrySet()) {
			if (entry.getKey() == null) {
				continue;
			}
			if (normalizedKey.equals(entry.getKey().trim())) {
				return entry.getValue();
			}
		}
		return null;
	}

//
//	public RatingsConfig getRatingsConfig() {
//		return ratingsConfig;
//	}
//
//
//	public void setRatingsConfig(RatingsConfig ratingsConfig) {
//		this.ratingsConfig = ratingsConfig;
//	}

	public BarcodeAggregationProperties getBarcodeConfig() {
		return barcodeConfig;
	}

	public void setBarcodeConfig(BarcodeAggregationProperties barcodeConfig) {
		this.barcodeConfig = barcodeConfig;
	}

	public RecommandationsConfig getRecommandationsConfig() {
		return recommandationsConfig;
	}

	public void setRecommandationsConfig(RecommandationsConfig recommandationsConfig) {
		this.recommandationsConfig = recommandationsConfig;
	}

	public DescriptionsAggregationConfig getDescriptionsAggregationConfig() {
		return descriptionsAggregationConfig;
	}

	public void setDescriptionsAggregationConfig(DescriptionsAggregationConfig descriptionsAggregationConfig) {
		this.descriptionsAggregationConfig = descriptionsAggregationConfig;
	}

	public ScoringAggregationConfig getScoringAggregationConfig() {
		return scoringAggregationConfig;
	}

	public void setScoringAggregationConfig(ScoringAggregationConfig scoringAggregationConfig) {
		this.scoringAggregationConfig = scoringAggregationConfig;
	}

	public Map<String, Set<String>> getMatchingCategories() {
		return matchingCategories;
	}

	public void setMatchingCategories(Map<String, Set<String>> matchingCategories) {
		this.matchingCategories = matchingCategories;
	}

//	public Set<String> getUnmatchingCategories() {
//		return unmatchingCategories;
//	}
//
//	public void setUnmatchingCategories(Set<String> unmatchingCategories) {
//		this.unmatchingCategories = unmatchingCategories;
//	}

	public Integer getGoogleTaxonomyId() {
		return googleTaxonomyId;
	}

	public ImpactScoreConfig getImpactScoreConfig() {
		return impactScoreConfig;
	}

	public void setImpactScoreConfig(ImpactScoreConfig impactScoreConfig) {
		this.impactScoreConfig = impactScoreConfig;
	}

	public void setGoogleTaxonomyId(Integer taxonomyId) {
		this.googleTaxonomyId = taxonomyId;
	}

	public Map<String, ProductI18nElements> getI18n() {
		return i18n;
	}

	public void setI18n(Map<String, ProductI18nElements> texts) {
		this.i18n = texts;
	}

//
//	public GenAiConfig getGenAiConfig() {
//		return genAiConfig;
//	}
//
//
//
//	public void setGenAiConfig(GenAiConfig genAiConfig) {
//		this.genAiConfig = genAiConfig;
//	}

	public List<FeatureGroup> getFeatureGroups() {
		return featureGroups;
	}

	public void setFeatureGroups(List<FeatureGroup> featureGroups) {
		this.featureGroups = featureGroups;
	}

	public Integer getIcecatTaxonomyId() {
		return icecatTaxonomyId;
	}

	public void setIcecatTaxonomyId(Integer icecatTaxonomyId) {
		this.icecatTaxonomyId = icecatTaxonomyId;
	}

	public List<String> getGlobalTechnicalFilters() {
		return globalTechnicalFilters;
	}

	public void setGlobalTechnicalFilters(List<String> globalTechnicalFilters) {
		this.globalTechnicalFilters = globalTechnicalFilters;
	}

	public List<String> getEcoFilters() {
		return ecoFilters;
	}

	public void setEcoFilters(List<String> ecoFilters) {
		this.ecoFilters = ecoFilters;
	}

	public List<String> getTechnicalFilters() {
		return technicalFilters;
	}

	public void setTechnicalFilters(List<String> technicalFilters) {
		this.technicalFilters = technicalFilters;
	}

	public List<String> getPopularAttributes() {
		return popularAttributes;
	}

	public void setPopularAttributes(List<String> popularAttributes) {
		this.popularAttributes = popularAttributes;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isPopular() {
		return popular;
	}

	public void setPopular(boolean popular) {
		this.popular = popular;
	}

	public Set<String> getRequiredAttributes() {
		return requiredAttributes;
	}

	public void setRequiredAttributes(Set<String> requiredAttributes) {
		this.requiredAttributes = requiredAttributes;
	}

	public Map<String, String> getBrandsAlias() {
		return brandsAlias;
	}

	public void setBrandsAlias(Map<String, String> brandsAlias) {
		this.brandsAlias = brandsAlias;
	}

	public Set<String> getBrandsExclusion() {
		return brandsExclusion;
	}

	public void setBrandsExclusion(Set<String> brandsExclusion) {
		this.brandsExclusion = brandsExclusion;
	}

	public boolean isForceNameGeneration() {
		return forceNameGeneration;
	}

	public void setForceNameGeneration(boolean forceUrlNameGeneration) {
		this.forceNameGeneration = forceUrlNameGeneration;
	}

	public Set<String> getGenerationExcludedFromCategoriesMatching() {
		return generationExcludedFromCategoriesMatching;
	}

	public void setGenerationExcludedFromCategoriesMatching(Set<String> excludedFromCategoriesMatching) {
		this.generationExcludedFromCategoriesMatching = excludedFromCategoriesMatching;
	}

	public Set<String> getExcludingTokensFromCategoriesMatching() {
		return excludingTokensFromCategoriesMatching;
	}

	public void setExcludingTokensFromCategoriesMatching(Set<String> excludingTokens) {
		this.excludingTokensFromCategoriesMatching = excludingTokens;
	}

	public Set<String> getGenerationExcludedFromAttributesMatching() {
		return generationExcludedFromAttributesMatching;
	}

	public void setGenerationExcludedFromAttributesMatching(Set<String> generationExcludedFromAttributesMatching) {
		this.generationExcludedFromAttributesMatching = generationExcludedFromAttributesMatching;
	}

	public Map<String, ImpactScoreCriteria> getAvailableImpactScoreCriterias() {
		return availableImpactScoreCriterias;
	}

	public void setAvailableImpactScoreCriterias(Map<String, ImpactScoreCriteria> availableImpactScoreCriterias) {
		this.availableImpactScoreCriterias = availableImpactScoreCriterias;
	}

	public Integer getWorseLimit() {
		return worseLimit;
	}

	public void setWorseLimit(Integer worseLimit) {
		this.worseLimit = worseLimit;
	}

	public Integer getBettersLimit() {
		return bettersLimit;
	}

	public void setBettersLimit(Integer bettersLimit) {
		this.bettersLimit = bettersLimit;
	}

	public List<VerticalSubset> getSubsets() {
		return subsets;
	}

	public void setSubsets(List<VerticalSubset> subsets) {
		this.subsets = subsets;
	}

	public VerticalSubset getBrandsSubset() {
		return brandsSubset;
	}

	public void setBrandsSubset(VerticalSubset brandsSubset) {
		this.brandsSubset = brandsSubset;
	}

	public Integer getOrder() {
		return order;
	}

	public void setOrder(Integer order) {
		this.order = order;
	}

	public String getVerticalImage() {
		return verticalImage;
	}

	public void setVerticalImage(String verticalImage) {
		this.verticalImage = verticalImage;
	}

	public Set<String> getCacheTokenNames() {
		return cacheTokenNames;
	}

	public void setCacheTokenNames(Set<String> cacheTokenNames) {
		this.cacheTokenNames = cacheTokenNames;
	}

	public String getEprelGroupName() {
		return eprelGroupName;
	}

	public void setEprelGroupName(String eprelGroupName) {
		this.eprelGroupName = eprelGroupName;
	}

}
