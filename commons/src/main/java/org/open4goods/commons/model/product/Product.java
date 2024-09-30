
package org.open4goods.commons.model.product;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DurationFieldType;
import org.joda.time.Period;
import org.joda.time.PeriodType;
import org.joda.time.format.PeriodFormat;
import org.joda.time.format.PeriodFormatter;
import org.open4goods.commons.dao.ProductRepository;
import org.open4goods.commons.exceptions.ValidationException;
import org.open4goods.commons.model.EcoScoreRanking;
import org.open4goods.commons.model.Localisable;
import org.open4goods.commons.model.Standardisable;
import org.open4goods.commons.model.constants.Currency;
import org.open4goods.commons.model.constants.ProductCondition;
import org.open4goods.commons.model.constants.ReferentielKey;
import org.open4goods.commons.model.constants.ResourceType;
import org.open4goods.commons.model.data.AiDescriptions;
import org.open4goods.commons.model.data.Resource;
import org.open4goods.commons.model.data.Score;
import org.open4goods.commons.model.data.UnindexedKeyVal;
import org.open4goods.commons.model.data.UnindexedKeyValTimestamp;
import org.open4goods.commons.services.StandardiserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Dynamic;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Mapping;
import org.springframework.data.elasticsearch.annotations.Setting;
import org.springframework.data.elasticsearch.annotations.WriteTypeHint;
import org.springframework.data.redis.core.RedisHash;

@Document(indexName = Product.DEFAULT_REPO, createIndex = true, writeTypeHint = WriteTypeHint.FALSE, dynamic = Dynamic.FALSE)
@RedisHash(value=Product.DEFAULT_REPO, timeToLive = ProductRepository.VALID_UNTIL_DURATION)
@Setting( settingPath = "/product-settings.json")
@Mapping(mappingPath = "/product-mappings.json")
// TODO : Disabling to see/test  if a clean jackson serial
//@JsonIgnoreProperties(ignoreUnknown = true)
public class Product implements Standardisable {

	private final static Logger logger = LoggerFactory.getLogger(Product.class);

	public static final String DEFAULT_REPO = "products";

	// Should not be used 
	// If true, the referentiel attribute will be updated if a shortest version exists in alternativeModels
	private static final boolean FORCE = false;

	/**
	 * The ID is the gtin
	 */
	@Id
	private long id;

	
	/**
	 * The list of external id's for this product
	 */
	private ExternalIds externalIds = new ExternalIds();
		
	/**
	 * The date this item has been created
	 */
	private long creationDate;
	
	/**
	 * The last date this product has changed (new data, price change, new comment,	 * so on...)
	 */
	private long lastChange;

	/** The associated vertical, if any**/
	private String vertical;
	
	
	/** If true, means the item is excluded from vertical representation (because not enough data, ....)**/
	private boolean excluded = false;
	
	/** The list of other model's known for this product **/
	private Set<String> alternativeModels = new HashSet<>();
	
	
	/** The list of other id's known for this product **/
	private Set<UnindexedKeyValTimestamp> alternativeBrands = new HashSet<>();
		
	/** Namings informations for this product **/
	private ProductTexts names = new ProductTexts();

	private Set<String> offerNames = new HashSet<>();

	
	//	@Field(index = false, store = false, type = FieldType.Object)
	//	/** The comments, aggregated and nlp processed **/
	//	private AggregatedComments comments = new AggregatedComments();

	private AggregatedAttributes attributes = new AggregatedAttributes();

	private AggregatedPrices price = new AggregatedPrices();

	private Set<String> datasourceNames = new HashSet<>();
	
	/**
	 * The media resources for this data
	 */
	private Set<Resource> resources = new HashSet<>();
	
	private String coverImagePath;

	
	/** The ai generated texts, keyed by language**/
	private Localisable<String,AiDescriptions> genaiTexts = new Localisable<>();

	/**
	 * Informations and resources related to the gtin
	 */
	private GtinInfo gtinInfos = new GtinInfo();

	/**
	 * The google taxonomy id
	 */
	private Integer googleTaxonomyId;
	
	/**
	 * The set of participating "productCategories", on datasources that build this
	 * aggregatedData
	 */
	private Set<String> datasourceCategories = new HashSet<>();

	private Set<UnindexedKeyVal> mappedCategories = new HashSet<>();
	
	private Map<String, Score> scores = new HashMap<>();
	
	private EcoScoreRanking ranking = new EcoScoreRanking();
	

	//////////////////// :
	// Stored (and computed) to help elastic querying / sorting
	////////////////////
	/** number of commercial offers **/
	private Integer offersCount = 0;


	//	/**
	//	 * Informations about participant datas and aggegation process
	//	 */
	//	@Field(index = false, store = false, type = FieldType.Object)
	//	private AggregationResult aggregationResult = new AggregationResult();

	public Product() {
		super();
	}

	public Product(final Long id) {
		super();
		this.id = id.longValue();

	}

	@Override
	public Set<Standardisable> standardisableChildren() {
        final Set<Standardisable> ret = new HashSet<>(price.standardisableChildren());
		return ret;
	}

	@Override
	public void standardize(final StandardiserService standardiser, final Currency currency) {

		for (final Standardisable s : standardisableChildren()) {
			s.standardize(standardiser, currency);
		}

	}

	@Override
	public String toString() {
		return "id:" + id;
	}

	@Override
	public boolean equals(final Object obj) {

		if (obj instanceof Product) {
			return id == (((Product) obj).getId());
		}

		return super.equals(obj);
	}

	
	
	public String longestOfferName() {
		return offerNames.stream().max (Comparator.comparingInt(String::length)).get();
	}

	public String shortestOfferName() {
		return offerNames.stream().min (Comparator.comparingInt(String::length)).orElse(null);
	}

	
	/**
	 *
	 * @return all names and descriptions, excluding the longest offer name
	 */
	public List<String> namesAndDescriptionsWithoutShortestName() {

		Set<String> ret = new HashSet<>();
		ret.addAll(getOfferNames());
		ret.remove(shortestOfferName());

        List<String> list = new ArrayList<>(ret);

		list.sort(Comparator.naturalOrder());

		return list;
	}

	
	public List<Score> realScores() {
		List<Score> ret = scores.values().stream()
				.filter(e -> !e.getVirtual())
				// TODO : Const
				.filter(e -> !e.getName().equals("ECOSCORE"))
				.sorted( (o1, o2) -> o2.getRelativ().getValue().compareTo(o1.getRelativ().getValue()))
				.toList();
		
		return ret;
	}
	
	/**
	 * 
	 * @return the ecoscore or null
	 */
	public Score ecoscore() {
		// TODO : const
		return scores.get("ECOSCORE");
	}
	
		
	public String caracteristics() {
		
		StringBuilder sb = new StringBuilder();
		
		for (Entry<ReferentielKey, String> attr : attributes.getReferentielAttributes().entrySet()) {
			sb.append(" - ").append(attr.getKey().toString()).append(" : ").append(attr.getValue()).append("\n");
		}
		

		for (AggregatedAttribute attr : attributes.getUnmapedAttributes()) {
			
			if (attr.getIcecatTaxonomyIds().size() > 0) {
				sb.append(" - ").append(attr.getName().toString()).append(" : ").append(attr.getValue()).append("\n");
			}
		}

		
		
		/**
		 
		for (Entry<String, AggregatedAttribute> attr : attributes.getAggregatedAttributes().entrySet()) {
			sb.append(" - ").append(attr.getKey().toString()).append(" : ").append(attr.getValue().getValue()).append("\n");
		}
		
		for (AggregatedFeature attr : attributes.getFeatures()) {
			sb.append(" - ").append(attr.getName().toString()).append("\n");
		}

		for (AggregatedAttribute attr : attributes.getUnmapedAttributes()) {
			sb.append(" - ").append(attr.getName().toString()).append(" : ").append(attr.getValue()).append("\n");
		}
		 */

		return sb.toString();
		
	}
	
	
	public List<Resource> pdfs () {
		return resources.stream().filter(e-> e.getResourceType() != null &&  e.getResourceType().equals(ResourceType.PDF)).toList();
	}
		
	
	public String externalCover() {
		String coverPath="/icons/no-image.png";

		List<Resource> images = unprocessedimages();
		if (images != null && images.size() > 0) {
			Resource first = images.getFirst();
			if (null != first) {
				coverPath = (first.getUrl());
			}
		}
		return coverPath;
	}
	
	
	public List<Resource> unprocessedimages() {
		// TODO Auto-generated method stub
		return resources.stream()
				.filter(e-> e.getUrl() != null)
				.filter(e -> e.getUrl().endsWith(".jpg") || e.getUrl().endsWith(".png") || e.getUrl().endsWith(".jpeg"))
				.toList();
	}
	
	// TODO : Should be outsided / cached
	public List<Resource> images() {
	    // Filter resources of type IMAGE
	    List<Resource> images = resources.stream()
	                                     .filter(e -> e.getResourceType() != null && e.getResourceType().equals(ResourceType.IMAGE))
	                                     .toList();
	    
	    //////////////////////////////////
	    // Applying filtering / ordering
	    //////////////////////////////////
	    
	    // Keep the covers
	    List<Resource> covers = images.stream()
	                                  .filter(e -> e.getTags().contains("cover"))
	                                  .toList();
	    Set<Integer> coversGroupsId = covers.stream()
	                                        .map(Resource::getGroup)
	                                        .collect(Collectors.toSet());
	    Set<Integer> otherGroupsId = images.stream()
	                                       .filter(e -> !coversGroupsId.contains(e.getGroup()))
	                                       .map(Resource::getGroup)
	                                       .collect(Collectors.toSet());
	    
	    List<Resource> ret = new ArrayList<>();
	    
	    // First, add the best cover images
	    coversGroupsId.forEach(coverGroupId -> ret.add(bestByGroup(images, coverGroupId)));
	    
	    // Then, add the other images by groups
	    otherGroupsId.forEach(otherGroupId -> ret.add(bestByGroup(images, otherGroupId)));
	    
	    // TODO : perf : null check Could be avoided
	    return ret.stream().filter(e-> null != e).toList();
	}

	/**
	 * Finds the best resource by group based on the number of pixels
	 * 
	 * @param images The list of images
	 * @param groupId The group identifier
	 * @return The best resource in the group
	 */
	private Resource bestByGroup(List<Resource> images, Integer groupId) {
		return images.stream()
		        .filter(image -> image.getGroup() != null && image.getGroup().equals(groupId)) // Filtrer les images du groupe spécifié
		        .max(Comparator.comparingInt(image -> image.getImageInfo().pixels())) // Trouver l'image avec le plus de pixels
		        .orElse(null); // Retourner null si aucune image n'est trouvée

		
	}
	

	

	public List<Resource> videos () {
		return resources.stream().filter(e-> e.getResourceType() != null &&  e.getResourceType().equals(ResourceType.VIDEO)).toList();
	}
	
	public AggregatedPrice bestPrice() {
		return price == null ? null : price.getMinPrice();
	}

	//	/**
	//	 * Return ratings having specific tags
	//	 *
	//	 * @param tag
	//	 * @return
	//	 */
	//	public Set<SourcedRating> ratingsByTag(final String tag) {
	//
	//		if (null == tag || null == ratings) {
	//			return null;
	//		}
	//
	//		return ratings.stream().filter(e -> e.getTags().contains(tag)).collect(Collectors.toSet());
	//	}

	//	/**
	//	 * Return a rating having specific tags
	//	 *
	//	 * @param tag
	//	 * @return
	//	 */
	//	public SourcedRating ratingByTag(final String tag) {
	//
	//		if (null == tag || null == ratings) {
	//			return null;
	//		}
	//
	//		return ratings.stream().filter(e -> e.getTags().contains(tag)).findAny().orElse(null);
	//	}

	/**
	 *
	 * @return true if this AggrgatedData has alternateIds
	 */
	public Boolean hasAlternateIds() {
		return alternativeModels.size() > 0;
	}

	
	public boolean hasOccasions() {
		return price.getConditions().contains(ProductCondition.OCCASION);
	}
	
	
	
	
	
	
	//	/**
	//	 * Return all the specific ratings
	//	 *
	//	 * @return
	//	 * @throws ResourceNotFoundException
	//	 */
	//	public Set<Rating> ratings(final RatingType ratingType) {
	//		return ratings.stream().filter(e -> e.getTags().contains(ratingType.toString())).collect(Collectors.toSet());
	//	}


	public String alternateIdsAsText() {
		return StringUtils.join(alternativeModels, ", ");
	}

	/**
	 *
	 * @return the brand, if availlable from referentiel attributes
	 */
	public String brand() {
		return attributes.getReferentielAttributes().get(ReferentielKey.BRAND);

	}

	/**
	 *
	 * @return the gtin
	 */
	public String gtin() {
		// TODO(p2, features) : Should store the GTIN type when encountered in gtin infos, and then render with appropriate leading 0
		try {
			return String.valueOf(id);
		} catch (final Exception e) {
			return null;
		}
	}

	/**
	 *
	 * @return the brandUid, if availlable from referentiel attributes
	 */
	public String model() {
		return attributes.getReferentielAttributes().get(ReferentielKey.MODEL);
	}


	public String randomModel() {	
		List<String> names =  new ArrayList<>();
		names.add(model());				
		alternativeModels.forEach(e -> names.add(e));
		Random rand = new Random();
		return names.get(rand.nextInt(names.size()));
		
	}
	
	
	
	//	/**
	//	 * Returns the name (brand - model)
	//	 */
	//	public String name() {
	//		return id();
	//	}
	//
	/**
	 * Returns the best human readable name
	 */
	public String bestName() {
		String ret;
		if (null == brand() || null == model()) {
			ret = shortestOfferName();
		} else {
			ret =  brandAndModel();
		}
		
		if (null == ret) {
			ret = gtin();
		}

		return ret;
	}

	/**
	 *
	 * @return All categories in an IHM purpose, without the shortest one
	 */
	public List<String> datasourceCategoriesWithoutShortest() {

        Set<String> ret = new HashSet<>(datasourceCategories.stream().toList());

		ret.remove(shortestCategory());

        List<String> list = new ArrayList<>(ret);

		list.sort(Comparator.comparingInt(String::length));

		return list;

	}
	
	
	public String ecoscoreAsString() {
		Score s = ecoscore();
		if (null == s) {
			return "";
		} else {
			return s.getRelativ().getValue() + "/" + StandardiserService.DEFAULT_MAX_RATING;
		}
	}
	public String brandAndModel() {
		String ret = "";
		if (!StringUtils.isEmpty(brand())) {
			ret += brand() +"-"; 
		}
		
		ret += model();
		
		return ret;
	}

	
	public String compensation () {
				
		Double c = bestPrice() == null ? 0.0 :  bestPrice().getCompensation();		
		return String.format("%.2f", c);
	}
	/**
	 *
	 * @return The shortest category for this product
	 */
	public String shortestCategory() {
		return datasourceCategories.stream().min (Comparator.comparingInt(String::length)).orElse(null);
	}



	//	/**
	//	 *
	//	 * @return the id
	//	 */
	//	public String id() {
	//		StringBuilder builder = new StringBuilder();
	//
	//		if (null == brand() || null == model()) {
	//			builder.append(gtin());
	//		} else {
	//			builder.append(brand()).append("-").append(model());
	//		}
	//		return builder.toString();
	//	}


	
	/**
	 * TODO : merge with the one on price()
	 * @return a localised formated duration of when the product was last indexed
	 */
	public String ago(Locale locale) {

		long duration = System.currentTimeMillis() - lastChange;
		
		
		
		Period period;
		if (duration < 3600000) {
			DurationFieldType[] min = { DurationFieldType.minutes(), DurationFieldType.seconds() };
			period = new Period(duration, PeriodType.forFields(min)).normalizedStandard();
		} else {
			DurationFieldType[] full = { DurationFieldType.days(), DurationFieldType.hours() };
			period = new Period(duration, PeriodType.forFields(full)).normalizedStandard();

		}
		
		PeriodFormatter formatter = PeriodFormat.wordBased();

		String ret = (formatter. print(period));
		
		
		return ret;
	}
	
	/**
	 * Return text version of the creation date
	 * @param locale
	 * @return
	 */
	public String creationDate(Locale locale) {
		DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.DEFAULT, locale);
		String date = dateFormat.format(new Date(creationDate));
		return date;
	}
	
	
	
//	/**
//	 * Initialize a dummy DataFragment from this product, (used to "touch" products to replay batch scenarios with realtimeAggregationService)
//	 * @return
//	 */
//	public DataFragment getFragment() {
//		DataFragment ret = new DataFragment();
//		ret.setLastIndexationDate(lastChange);
//		ret.setCreationDate(creationDate);
//		ret.setReferentielAttributes(attributes.getReferentielAttributes());
//		
//		return ret;
//		
//	}


	/**
	 * Add an image to this product
	 * @param url
	 */
//	public void addImage(String url, String tag) {
//		if (!StringUtils.isEmpty(url)) {
//			Resource r = new Resource(url);
//			r.addTag(tag);
// TODO : Check incidence			
//			resources.remove(r);
	
	
//			resources.add(r);			
//		}
//	}
	
	public void addResource(final Resource resource) throws ValidationException {

		
		if (null == resource) {
			return;
		}

		resource.validate();

		// Smart update, time consuming but necessary.
		// TODO : Involve on a map on the new model
		
		Resource existing = resources.stream().filter(e -> e.equals(resource)).findFirst().orElse(null);
		
		if (null == existing) {
			logger.info("Adding new resource : {}",resource);
			resources.add(resource);
		} else {
			logger.info("Updating existing resource : {}",resource);
			// Smart update
			existing.setTags(resource.getTags());
			existing.setHardTags(resource.getHardTags());
			existing.setDatasourceName(resource.getDatasourceName());
			
			resources.remove(resource);
			resources.add(existing);
		}
		
	}
	
	
	public String url (String language) {
		return names.getUrl().getOrDefault(language, names.getUrl().get("default"));
	}
	
	/**
	 * Add the model referentiel attribute, applying some spliting mechanism and cleaning pass
	 * @param value
	 */
	public void addModel(String value) {
		
		String model = StringUtils.normalizeSpace(value).toUpperCase();
		
		// TODO : Eviction size from conf
		if (StringUtils.isEmpty(value) || value.length() < 3) {
			return;
		}
		// Splitting on conventionnal suffixes (/ - .)
		// TODO : Const / conf
		String[]frags = model.split("/|\\|.|-");

		alternativeModels.add(value);
		if (frags.length > 1) {
			logger.info("Found an alternative model : " + frags[0]);
			alternativeModels.add(frags[0]);
		}

		// Case ref attribute is already set, we keep as it and we remove the elected one from alternativeModels
		String existing = model();
		
		if ( StringUtils.isEmpty(existing) || FORCE) {
			String shortest = shortestModel();
			if (null != shortest) {
				attributes.getReferentielAttributes().put(ReferentielKey.MODEL, shortest);
				alternativeModels.remove(shortest);
			}
		} else {
			alternativeModels.remove(existing);
		}		
	}

	/**
	 * 
	 * @return the shortest model name	
	 */
	public String shortestModel() {		
		Set<String> names = new HashSet<>();
		names.add(model());
		names.addAll(alternativeModels);
		if (names.size() == 0) {
			return null;
		} else {
			return names.stream().filter(e-> e != null). min(Comparator.comparingInt(String::length)).orElse(null);
		}
	}
	
	//////////////////////////////////////////
	// Getters / Setters
	//////////////////////////////////////////

	
	


	public Set<UnindexedKeyValTimestamp> getAlternativeBrands() {
		return alternativeBrands;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public void setAlternativeBrands(Set<UnindexedKeyValTimestamp> alternativeBrands) {
		this.alternativeBrands = alternativeBrands;
	}

	public String getVertical() {
		return vertical;
	}

	public void setVertical(String vertical) {
		this.vertical = vertical;
	}

	public long getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(long creationDate) {
		this.creationDate = creationDate;
	}

	public long getLastChange() {
		return lastChange;
	}

	public void setLastChange(long lastChange) {
		this.lastChange = lastChange;
	}

	public ProductTexts getNames() {
		return names;
	}

	public void setNames(ProductTexts names) {
		this.names = names;
	}

	public AggregatedAttributes getAttributes() {
		return attributes;
	}

	public void setAttributes(AggregatedAttributes attributes) {
		this.attributes = attributes;
	}

	public AggregatedPrices getPrice() {
		return price;
	}

	public void setPrice(AggregatedPrices price) {
		this.price = price;
	}

	public Set<Resource> getResources() {
		return resources;
	}

	public void setResources(Set<Resource> resources) {
		this.resources = resources;
	}

	public EcoScoreRanking getRanking() {
		return ranking;
	}

	public void setRanking(EcoScoreRanking ranking) {
		this.ranking = ranking;
	}

	public GtinInfo getGtinInfos() {
		return gtinInfos;
	}

	public void setGtinInfos(GtinInfo gtinInfos) {
		this.gtinInfos = gtinInfos;
	}

	public Set<String> getDatasourceCategories() {
		return datasourceCategories;
	}

	public void setDatasourceCategories(Set<String> datasourceCategories) {
		this.datasourceCategories = datasourceCategories;
	}

	


	public Set<UnindexedKeyVal> getMappedCategories() {
		return mappedCategories;
	}

	public void setMappedCategories(Set<UnindexedKeyVal> mappedCategories) {
		this.mappedCategories = mappedCategories;
	}

	public Map<String, Score> getScores() {
		return scores;
	}

	public void setScores(Map<String, Score> scores) {
		this.scores = scores;
	}

	public Integer getOffersCount() {
		return offersCount;
	}

	public void setOffersCount(Integer offersCount) {
		this.offersCount = offersCount;
	}


	public Localisable<String, AiDescriptions> getGenaiTexts() {
		return genaiTexts;
	}

	public void setGenaiTexts(Localisable<String, AiDescriptions> genaiDescriptions) {
		this.genaiTexts = genaiDescriptions;
	}

	public Integer getGoogleTaxonomyId() {
		return googleTaxonomyId;
	}

	public void setGoogleTaxonomyId(Integer googleTaxonomyId) {
		this.googleTaxonomyId = googleTaxonomyId;
	}

	public Set<String> getDatasourceNames() {
		return datasourceNames;
	}

	public void setDatasourceNames(Set<String> datasourceNames) {
		this.datasourceNames = datasourceNames;
	}


	public ExternalIds getExternalIds() {
		return externalIds;
	}

	public void setExternalIds(ExternalIds externalId) {
		this.externalIds = externalId;
	}

	public String getCoverImagePath() {
		return coverImagePath;
	}

	public void setCoverImagePath(String coverImagePath) {
		this.coverImagePath = coverImagePath;
	}

	public Set<String> getAlternativeModels() {
		return alternativeModels;
	}

	public void setAlternativeModels(Set<String> alternativeModels) {
		this.alternativeModels = alternativeModels;
	}

	public boolean isExcluded() {
		return excluded;
	}

	public void setExcluded(boolean excluded) {
		this.excluded = excluded;
	}

	public Set<String> getOfferNames() {
		return offerNames;
	}

	public void setOfferNames(Set<String> offerNames) {
		this.offerNames = offerNames;
	}






	




	
	
}
