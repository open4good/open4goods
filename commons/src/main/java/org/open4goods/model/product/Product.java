
package org.open4goods.model.product;

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
import org.open4goods.dao.ProductRepository;
import org.open4goods.exceptions.ValidationException;
import org.open4goods.model.EcoScoreRanking;
import org.open4goods.model.Standardisable;
import org.open4goods.model.constants.Currency;
import org.open4goods.model.constants.ProductCondition;
import org.open4goods.model.constants.ProviderType;
import org.open4goods.model.constants.ReferentielKey;
import org.open4goods.model.constants.ResourceType;
import org.open4goods.model.data.AiDescription;
import org.open4goods.model.data.Description;
import org.open4goods.model.data.Resource;
import org.open4goods.model.data.Score;
import org.open4goods.model.data.UnindexedKeyVal;
import org.open4goods.model.data.UnindexedKeyValTimestamp;
import org.open4goods.services.StandardiserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;
import org.springframework.data.redis.core.RedisHash;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Document(indexName = Product.DEFAULT_REPO, createIndex = true)
@RedisHash(value=Product.DEFAULT_REPO, timeToLive = ProductRepository.VALID_UNTIL_DURATION)
@Setting( settingPath = "/elastic-settings.json")
// TODO : Disabling to see/test  if a clean jackson serial
@JsonIgnoreProperties(ignoreUnknown = true)
public class Product implements Standardisable {

	private final static Logger logger = LoggerFactory.getLogger(Product.class);

	public static final String DEFAULT_REPO = "products";

	// TODO : Conf
	// If true, the referentiel atrribute will be updated if a shortes version exists in alternativeModels
	private static final boolean FORCE = true;

	/**
	 * The ID is the gtin
	 */
	@Id
	@Field(index = true, store = false, type = FieldType.Keyword)
	private String id;

	
	/**
	 * The list of external id's for this product
	 */
	private ExternalIds externalId = new ExternalIds();
		
	/**
	 * The date this item has been created
	 */
	@Field(type = FieldType.Date)
	private long creationDate;
	
	/**
	 * The last date this product has changed (new data, price change, new comment,	 * so on...)
	 */
	@Field(type = FieldType.Date)
	private long lastChange;

	/** The associated vertical, if any**/
	@Field(index = true, store = false, type = FieldType.Keyword)
	private String vertical;
	
	
	/** If true, means the item is excluded from vertical representation (because not enough data, ....)**/
	@Field(index = true, store = false, type = FieldType.Boolean)
	private boolean excluded = false;
	
	/** The list of other id's known for this product **/
	// TODO : Rename to alternativ model names
//	private Set<UnindexedKeyValTimestamp> alternativeIds = new HashSet<>();

	@Field(index = true, store = false, type = FieldType.Keyword)
	private Set<String> alternativeModels = new HashSet<>();
	
	
	/** The list of other id's known for this product **/
	@Field(index = false, store = false, type = FieldType.Object)
	private Set<UnindexedKeyValTimestamp> alternativeBrands = new HashSet<>();
		
	/** Namings informations for this product **/
	// TODO : Should be computed
	@Field(index = true, store = false, type = FieldType.Object)
	private ProductTexts names = new ProductTexts();

	//	@Field(index = false, store = false, type = FieldType.Object)
	//	/** The comments, aggregated and nlp processed **/
	//	private AggregatedComments comments = new AggregatedComments();

	@Field(index = false, store = false, type = FieldType.Object)
	private AggregatedAttributes attributes = new AggregatedAttributes();

	@Field(index = false, store = false, type = FieldType.Object)
	private AggregatedPrices price = new AggregatedPrices();

	@Field(index = false, store = false, type = FieldType.Keyword)
	private Set<String> datasourceNames = new HashSet<>();
	
	/**
	 * The media resources for this data
	 */
	@Field(index = false, store = false, type = FieldType.Object)
	private Set<Resource> resources = new HashSet<>();
	
	@Field(index = false, store = false, type = FieldType.Keyword)
	private String coverImagePath;

	
	/** The descriptions **/
	@Field(index = false, store = false, type = FieldType.Object)
	private Set<Description> descriptions = new HashSet<>();

	/** The ai generated texts**/
	// TODO : Buggy for i18n
	@Field(index = false, store = false, type = FieldType.Object)
	private Map<String,AiDescription> aiDescriptions = new HashMap<>();

	/** The human crafted description**/
	// TODO : remove
	@Field(index = false, store = false, type = FieldType.Object)
	private Description humanDescription;

	/**
	 * Informations and resources related to the gtin
	 */
	@Field(index = false, store = false, type = FieldType.Object)
	private GtinInfo gtinInfos = new GtinInfo();

	/**
	 * The google taxonomy id
	 */
	@Field(index = true, store = false, type = FieldType.Integer)
	private Integer googleTaxonomyId;
	
	/**
	 * The set of participating "productCategories", on datasources that build this
	 * aggregatedData
	 */
	@Field(index = true, store = false, type = FieldType.Keyword)
	private Set<String> datasourceCategories = new HashSet<>();

	@Field(index = false, store = false, type = FieldType.Object)
	private Set<UnindexedKeyVal> mappedCategories = new HashSet<>();
	
	@Field(index = true, store = false, type = FieldType.Object)
	private Map<String, Score> scores = new HashMap<>();
	
	@Field(index = false, store = false, type = FieldType.Object)
	private EcoScoreRanking ranking = new EcoScoreRanking();
	
	
	
	
	// 
	//	/**
	//	 * All the ratings
	//	 */
	//	@Field(index = false, store = false, type = FieldType.Object)
	//	private Set<SourcedRating> ratings = new HashSet<>();

	//	@Field(index = false, store = false, type = FieldType.Object)
	//	private Set<Question> questions = new HashSet<>();
	//
	//	@Field(index = false, store = false, type = FieldType.Object)
	//	private Set<ProsOrCons> pros = new HashSet<>();
	//
	//	@Field(index = false, store = false, type = FieldType.Object)
	//	private Set<ProsOrCons> cons = new HashSet<>();


	//////////////////// :
	// Stored (and computed) to help elastic querying / sorting
	////////////////////
	/** number of commercial offers **/
	@Field(index = true, store = false, type = FieldType.Integer)
	private Integer offersCount = 0;


	//	/**
	//	 * Informations about participant datas and aggegation process
	//	 */
	//	@Field(index = false, store = false, type = FieldType.Object)
	//	private AggregationResult aggregationResult = new AggregationResult();

	public Product() {
		super();
	}

	public Product(final String id) {
		super();
		this.id = id;

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
			return id.equals(((Product) obj).getId());
		}

		return super.equals(obj);
	}

	/**
	 * Get descriptions for a given language
	 *
	 * @param language
	 * @return
	 */
	public Map<String, Set<Description>> descriptions(final String language) {
		final Map<String, Set<Description>> ret = new HashMap<>();

		for (final Description d : getDescriptions()) {
			if (d.getContent().getLanguage().equals(language)) {
				final String lang = d.getContent().getLanguage();
				if (!ret.containsKey(lang)) {
					ret.put(lang, new HashSet<>());
				}
				ret.get(lang).add(d);
			}
		}
		return ret;
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
	
	

	public String ecological() {
		
		
		return id;
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
	
	/**
	 *
	 * @param language
	 * @return the shortest description in a given language
	 */
	public String shortestDescription(String language) {
		return descriptions.stream()
				.filter(e -> e.getContent().getLanguage().equals(language))
				.map(e -> e.getContent().getText())
				.min (Comparator.comparingInt(String::length))
				.orElse(null);
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

	public List<Description> reviewsDescriptions() {
		return descriptions.stream().filter(d -> d.getProviderType() == ProviderType.CONTENT_PROVIDER)
				.collect(Collectors.toList());
	}

	/**
	 * Return the descriptions related to the product (filtering expansionsOf)
	 *
	 * @return
	 */
	public List<Description> productDescriptions() {
		return new ArrayList<>(descriptions);
	}

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
	 * @return the gtin, if availlable from referentiel attributes
	 */
	public String gtin() {
		try {
			return id;
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
			ret = names.shortestOfferName();
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
		return String.format("%.2f", bestPrice().getCompensation());
	}
	/**
	 *
	 * @return The shortest category for this product
	 */
	public String shortestCategory() {
		return datasourceCategories.stream().min (Comparator.comparingInt(String::length)).orElse(null);
	}

	/**
	 *
	 * @return all names and descriptions, excluding the longest offer name
	 */
	public List<String> namesAndDescriptionsWithoutShortestName() {

		Set<String> ret = new HashSet<>();
		ret.addAll(names.getOfferNames());
		ret.addAll(descriptions.stream().map(e -> e.getContent().getText()).collect(Collectors.toSet()));
		ret.remove(names.shortestOfferName());

        List<String> list = new ArrayList<>(ret);

		list.sort(Comparator.naturalOrder());

		return list;
	}


	public String namesAndDescriptionsWithoutShortestNameWithCariage() {
		return StringUtils.join(namesAndDescriptionsWithoutShortestName(),"\n");
	}

	/**
	 *
	 * @return all names and descriptions, excluding the longest offer name
	 */
	public List<String> namesAndDescriptionsWithoutLongestName() {

		Set<String> ret = new HashSet<>();
		ret.addAll(names.getOfferNames());
		ret.addAll(descriptions.stream().map(e -> e.getContent().getText()).collect(Collectors.toSet()));
		ret.remove(names.longestOfferName());

        List<String> list = new ArrayList<>(ret);

		list.sort(Comparator.naturalOrder());

		return list;
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


	public List<String> tagCloudTokens() {
		List<String> tokens = new ArrayList<>();
		getNames().getOfferNames().stream().map(e -> e.split(" ")).forEach(e -> {
			for (String token : e) {
				if (token.length() > 1 && !StringUtils.isNumeric(token)) {
					tokens.add(token);
				}
			}
		});

		getDescriptions().stream().map(e -> e.getContent().getText().split(" ")).forEach(e -> {
			for (String token : e) {
				if (token.length() > 1 && !StringUtils.isNumeric(token)) {
					tokens.add(token);
				}
			}
		});

		return tokens;
	}

	
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

	
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}



	public Set<UnindexedKeyValTimestamp> getAlternativeBrands() {
		return alternativeBrands;
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

	public Set<Description> getDescriptions() {
		return descriptions;
	}

	public void setDescriptions(Set<Description> descriptions) {
		this.descriptions = descriptions;
	}

	public Description getHumanDescription() {
		return humanDescription;
	}

	public EcoScoreRanking getRanking() {
		return ranking;
	}

	public void setRanking(EcoScoreRanking ranking) {
		this.ranking = ranking;
	}

	public void setHumanDescription(Description humanDescription) {
		this.humanDescription = humanDescription;
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

	public Map<String, AiDescription> getAiDescriptions() {
		return aiDescriptions;
	}

	public void setAiDescriptions(Map<String, AiDescription> aiDescriptions) {
		this.aiDescriptions = aiDescriptions;
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


	public ExternalIds getExternalId() {
		return externalId;
	}

	public void setExternalId(ExternalIds externalId) {
		this.externalId = externalId;
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






	




	
	
}
