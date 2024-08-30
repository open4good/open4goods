package org.open4goods.commons.config.yml.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.open4goods.commons.config.yml.CommentsAggregationConfig;
import org.open4goods.commons.config.yml.attributes.AttributeConfig;
import org.open4goods.commons.helper.IdHelper;
import org.open4goods.commons.model.constants.CacheConstants;
import org.open4goods.commons.model.constants.UrlConstants;
import org.open4goods.commons.model.data.FeatureGroup;
import org.open4goods.commons.services.StandardiserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.annotation.JsonMerge;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotNull;

@Configuration
@ConfigurationProperties
/**
 * Le parametrage Yaml d'une verticale. Celui ci dispose soit de propriétés de premier niveau, soit de sous-objets de configuration.
 * @author goulven
 *
 */

public class VerticalConfig{

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
	 * The product url, metas title, description, ....
	 */
	@JsonMerge
	private Map<String,ProductI18nElements> i18n = new HashMap<>();
	
	@JsonMerge
	private GenAiConfig genAiConfig = new GenAiConfig();
	

	
	/**
	 * The categories to associate to this vertical
	 */
	private List<String> verticalFilters = new ArrayList<>();

	/**
	 * The categories that MUST BE PRESENT to associate to this vertical
	 */
	private Set<String> matchingCategories = new HashSet<>();

	/**
	 * The categories that MUST NOT BE PRESENT to associate to this vertical
	 */
	private Set<String> unmatchingCategories = new HashSet<>();


	/**
	 * Configuration for commentsConfig aggregation (tagcloud rules, ...)
	 */
	@JsonMerge
	private CommentsAggregationConfig commentsConfig = new CommentsAggregationConfig();

	@JsonMerge
	private Map<String, String> brandsCompanyMapping = new HashMap<>();
	
	
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
	 * Configuration relativ to ecoscore computation. Key / values are : scoreName -> Ponderation (0.1 = 10%)
	 * TODO : Ensure sum is 1
	 */ 
	@JsonMerge
	private Map<String, Double> ecoscoreConfig = new HashMap<>();

	
	@JsonMerge
	/**
	 * Configuration of the dedicated elastic index
	 */
	private VerticalIndexMappingConfig inexMappingConfig = new VerticalIndexMappingConfig();

	
	
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
	 * The config for swagger API
	 */
	private ApiConfig apiConfig = new ApiConfig();


	/**
	 * The behavior of relation data aggregation service, by relation type
	 */
	@NotNull
	private Map<String,RelationDataAggregationConfig> relationDataAggregationConfig = new HashMap<String, RelationDataAggregationConfig>();

	/**
	 * The behaviour of descriptions aggregation
	 */
	private DescriptionsAggregationConfig descriptionsAggregationConfig  = new DescriptionsAggregationConfig();


	/**
	 * The product scoring configuration
	 */
	@NotNull
	private ScoringAggregationConfig scoringAggregationConfig;

	/**
	 * The feature groups, to order / render attributes
	 */
	private List<FeatureGroup> featureGroups = new ArrayList<>();


	@Override
	public String toString() {
		return "config:"+ id;
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		return id.equals(((VerticalConfig)obj).id);
	}
	
	public ProductI18nElements i18n(String lang) {		
		return i18n.getOrDefault(lang, i18n.get("default"));		
	}
	/**
	 *
	 * @return the list of AttributeConfig that have to appear in search results,
	 *         ordered by their display position
	 */
	public List<AttributeConfig> searchTableAttributes() {
		final List<AttributeConfig> ret = new ArrayList<>();
		for (final AttributeConfig a : attributesConfig.getConfigs()) {
			if (null != a.getSearchTableOrder()) {
				try {
					ret.add(a.getSearchTableOrder(), a);
				} catch (final IndexOutOfBoundsException e) {
					LOGGER.warn("Attribute {} has invalid position 'searchTableOrders'. Adding last.", a);
					ret.add(a);
				}
			}
		}
		return ret;
	}

	
	/**
	 * Return the participation in percentage of a score in the ecoscore
	 * @param scoreName
	 * @return
	 */
	public Integer ecoscorePercentOf(String scoreName) {
		
		Double ponderation = ecoscoreConfig.get(scoreName);
		if (null == ponderation) {
			return -1;
		} else {
			return (int) Math.round(ponderation * 100);
		}
	}
	
	/**
	 * Return the participation in points of  the score
	 * @param scoreName
	 * @return
	 */
	public Double ecoscoreParticipationPointsOf20(String scoreName, Double relValue) {
		
		return  ecoscoreParticipationMaxPointsOf20(scoreName) * relValue  / StandardiserService.DEFAULT_MAX_RATING;
	}
	
	/**
	 * Return the max participation  points of the score 
	 * @param scoreName
	 * @return
	 */
	public Double ecoscoreParticipationMaxPointsOf20(String scoreName) {
		return Double.valueOf(ecoscorePercentOf(scoreName)) / 5;
	}
	
	
//	/**
//	 *
//	 * @return the list of AttributeConfig that have to appear in search results,
//	 *         ordered by their display position
//	 */
//	public List<AttributeConfig> statsAttributes() {
//		final List<AttributeConfig> ret = new ArrayList<>();
//		for (final AttributeConfig a : attributesConfig.getConfigs()) {
//			if (null != a.getStatsOrder()) {
//				try {
//					ret.add(a.getStatsOrder(), a);
//				} catch (final IndexOutOfBoundsException e) {
//					LOGGER.warn("statsOrder {} is invalid for attribute {}. Will place last", a.getStatsOrder(),
//							a.getName());
//					ret.add(a);
//				}
//			}
//		}
//		return ret;
//	}
//


	/**
	 *
	 * @return the specific attributes config for this vertical
	 */
	public List<AttributeConfig> verticalFilters() {
		if (null == verticalFilters) {
			return new ArrayList<>();
		}
		
		return verticalFilters.stream()
				
				.map(e -> getAttributesConfig().getAttributeConfigByKey(e))
				.filter(Objects::nonNull)
				.filter(AttributeConfig::isAsSearchFilter)
				.collect(Collectors.toList());

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


	public String getBaseUrl(final String siteLocale) {
		return i18n.getOrDefault(siteLocale, i18n.get("default")).getVerticalHomeUrl();
	}

	/**
	 * Return the company for a given brand, if defined in vertical configuration
	 * @param brand
	 * @return
	 */
	public String resolveCompany(String brand) {
		String resolved = brandsCompanyMapping.get(brand.toUpperCase());
		
		if (null == resolved) {
			return brand;
		} else {
			return resolved.toLowerCase();
		}
		
		
	}

	
	/**
	 * Retrieves the locale for the site through the request.
	 *
	 * @param key the key
	 * @return the language by server name
	 */
	@Cacheable( keyGenerator = CacheConstants.KEY_GENERATOR,  cacheNames=CacheConstants.FOREVER_LOCAL_CACHE_NAME)
	//TODO(perf) : check effectiv caching
	public String getLanguageByServerName(final String key) {
		HashMap<String,String> hashedSiteLocales = null;
		// Caching if needed
		if (null == hashedSiteLocales) {
			LOGGER.info("Hashing languageByServerName");
			hashedSiteLocales = new HashMap<>();
			for (final Entry<String, String> a : getNamings().getServerNames().entrySet()) {

				if (hashedSiteLocales.containsKey(a.getValue())) {
					LOGGER.error("Duplicate server name for : {} server will be ignored.",a.getKey());
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
	//TODO(perf) : caching on request.getservername
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
		return  UrlConstants.API_TOP_PRODUCTS;
	}


	public String getRecommandationFilterFor(final String id) {

		for (final RecommandationCriteria rc : recommandationsConfig.getRecommandations()) {
			for (final RecommandationChoice rf : rc.getChoices()) {
				if (rf.getName().equals(id)) {
					return rf.getQueryFragment();
				}
			}
		}

		return "";
	}





	/**
	 * Return the name of the resource index for this capsule
	 * @return
	 */
	public String resourceIndex() {

		return indexName()  + "-resources";
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

	public CommentsAggregationConfig getCommentsConfig() {
		return commentsConfig;
	}


	public void setCommentsConfig(CommentsAggregationConfig commentsConfig) {
		this.commentsConfig = commentsConfig;
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



	public ApiConfig getApiConfig() {
		return apiConfig;
	}


	public void setApiConfig(ApiConfig apiConfig) {
		this.apiConfig = apiConfig;
	}




	public Map<String, RelationDataAggregationConfig> getRelationDataAggregationConfig() {
		return relationDataAggregationConfig;
	}


	public void setRelationDataAggregationConfig(Map<String, RelationDataAggregationConfig> relationDataAggregationConfig) {
		this.relationDataAggregationConfig = relationDataAggregationConfig;
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




	public Set<String> getMatchingCategories() {
		return matchingCategories;
	}

	public void setMatchingCategories(Set<String> matchingCategories) {
		this.matchingCategories = matchingCategories;
	}

	public List<String> getVerticalFilters() {
		return verticalFilters;
	}

	public void setVerticalFilters(List<String> verticalFilters) {
		this.verticalFilters = verticalFilters;
	}

	
	public Set<String> getUnmatchingCategories() {
		return unmatchingCategories;
	}

	public void setUnmatchingCategories(Set<String> unmatchingCategories) {
		this.unmatchingCategories = unmatchingCategories;
	}

	public Map<String, Double> getEcoscoreConfig() {
		return ecoscoreConfig;
	}

	public void setEcoscoreConfig(Map<String, Double> ecoscoreConfig) {
		this.ecoscoreConfig = ecoscoreConfig;
	}

	public Integer getGoogleTaxonomyId() {
		return googleTaxonomyId;
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



	public GenAiConfig getGenAiConfig() {
		return genAiConfig;
	}



	public void setGenAiConfig(GenAiConfig genAiConfig) {
		this.genAiConfig = genAiConfig;
	}



	public Map<String, String> getBrandsCompanyMapping() {
		return brandsCompanyMapping;
	}



	public void setBrandsCompanyMapping(Map<String, String> brandsCompanyMapping) {
		this.brandsCompanyMapping = brandsCompanyMapping;
	}



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

	public VerticalIndexMappingConfig getInexMappingConfig() {
		return inexMappingConfig;
	}

	public void setInexMappingConfig(VerticalIndexMappingConfig inexMappingConfig) {
		this.inexMappingConfig = inexMappingConfig;
	}

}
