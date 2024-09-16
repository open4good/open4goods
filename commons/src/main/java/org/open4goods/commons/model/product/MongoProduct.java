
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
import org.open4goods.commons.services.StandardiserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

/**
 * Representation of a product
 */
public class MongoProduct implements Standardisable {

	private final static Logger logger = LoggerFactory.getLogger(MongoProduct.class);

	/**
	 * The ID is the gtin
	 */
	@Id
	private long id;

	/**
	 * The model name, set the first time a model name is encounterd. All variations are 
	 * availlable in the modelNames attribute
	 */
	private String model;

	/**
	 * The model name, set the first time a brand name is encounterd. All variations are 
	 * availlable in the modelNames attribute
	 */
	private String brand;
	/**
	 * The list of external id's for this product
	 */
	private ExternalIds externalIds = new ExternalIds();

	/**
	 * The date this item has been created
	 */
	private long creationDate;

	/**
	 * The last date this product has changed (new data, price change, new comment,
	 * * so on...)
	 */
	private long lastChange;

	/**
	 * Associated product vertical, if any
	 */
	private String vertical;

	/**
	 * If true, means the item is excluded from vertical representation (because not
	 * enough data, ....)
	 **/
	private boolean excluded = false;

	/** The list of all model's names , associated with datasources raising the product**/
	private Map<String,Set<String>> modelNames = new HashMap<>();

	/** The list of all brands known for this product, by datasourcename **/
	private Map<String,Set<String>> brandNames = new HashMap<>();

	/**
	 * Datasources that participates to this product informations
	 */
	private Set<String> datasourceNames = new HashSet<>();

	/**
	 * Informations and resources related to the gtin
	 */
	private MongoGtinInfo gtinInfos = new MongoGtinInfo();
	

	/**
	 * The categories, by datasources
	 */
	private Map<String,String> mappedCategories = new HashMap<>();
	
	private AggregatedPrices price = new AggregatedPrices();
	
	
	/**
	 * The google taxonomy id
	 */
	private Integer googleTaxonomyId;
	
	/** number of commercial offers **/
	private Integer offersCount = 0;


	/**
	 * The media resources for this data
	 */
	private Set<Resource> resources = new HashSet<>();
	
	/** The image used as cover **/
	private String coverImagePath;

	/** The ai generated texts, keyed by language **/
	private Localisable<String, AiDescriptions> genaiTexts = new Localisable<>();

	/**
	 * The scores computed for this product
	 */
	private Map<String, Score> scores = new HashMap<>();

	/**
	 * The ranking (position of the product in its category)
	 */
	private EcoScoreRanking ranking = new EcoScoreRanking();


	@Field(index = false, store = false, type = FieldType.Object)
	private AggregatedAttributes attributes = new AggregatedAttributes();
	
	/** Namings informations for this product **/
	@Field(index = true, store = false, type = FieldType.Object)
	// TODO(p1,design) : Refactor to a localisable at this level, check impact on already generated names, check url's can not be erased
	private ProductTexts names = new ProductTexts();

	
	// /**
	// * Informations about participant datas and aggegation process
	// */
	// @Field(index = false, store = false, type = FieldType.Object)
	// private AggregationResult aggregationResult = new AggregationResult();

	public MongoProduct() {
		super();
	}

	/**
	 * TODO(p2,dsign) : Here the hard mapping to convert our elastic items to"blobed" mongodb version 
	 * @param other
	 */
	public MongoProduct(final Product other) {
		super();
		
		// Converting gtin and info
		this.id = Long.valueOf(other.getId());
		this.gtinInfos.getEncounteredBarcodeType().add(other.getGtinInfos().getUpcType()); 
		this.gtinInfos.setCountry(other.getGtinInfos().getCountry());
		
		this.externalIds = other.getExternalIds(); // Using getter directly
		this.creationDate = other.getCreationDate();
		this.lastChange = other.getLastChange();
		this.vertical = other.getVertical();
		this.excluded = other.isExcluded();
		
		// Skipping because we don't know the the datasource providers in 
		//this.alternativeModels = other.getAlternativeModels(); 
		if (!StringUtils.isEmpty(other.model())) {
			this.model = StringUtils.normalizeSpace(other.model()).toUpperCase();;
		}

		if (!StringUtils.isEmpty(other.brand())) {
			this.brand =  StringUtils.stripAccents(StringUtils.normalizeSpace(other.brand()).toUpperCase());
		}
		
		
		other.getAlternativeBrands().stream().forEach(d -> {
			addBrand(d.getValue(), d.getKey());
		});

		
		// Categories datasource
		other.getMappedCategories().stream().forEach(cat -> {
			this.mappedCategories.put(cat.getKey(), cat.getValue());
		});
		
		
		
		
		
		
		this.names = other.getNames(); // Using getter from ProductTexts
		this.attributes = other.getAttributes(); // Using getter from AggregatedAttributes
		this.price = other.getPrice(); // Using getter from AggregatedPrices
		this.datasourceNames = other.getDatasourceNames(); // Directly using getter
		this.resources = other.getResources(); // Directly using getter
		this.coverImagePath = other.getCoverImagePath();
		this.genaiTexts = other.getGenaiTexts(); // Using getter from Localisable
		this.googleTaxonomyId = other.getGoogleTaxonomyId();
		this.scores = other.getScores(); // Directly using getter
		this.ranking = other.getRanking(); // Using getter from EcoScoreRanking
		this.offersCount = other.getOffersCount();
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

		if (obj instanceof MongoProduct) {
			return id == (((MongoProduct) obj).getId());
		}

		return super.equals(obj);
	}

	/**
	 *
	 * @return all names and descriptions, excluding the longest offer name
	 */
	public List<String> namesAndDescriptionsWithoutShortestName() {

		Set<String> ret = new HashSet<>();
		ret.addAll(names.getOfferNames());
		ret.remove(names.shortestOfferName());

		List<String> list = new ArrayList<>(ret);

		list.sort(Comparator.naturalOrder());

		return list;
	}

	public List<Score> realScores() {
		List<Score> ret = scores.values().stream().filter(e -> !e.getVirtual())
				// TODO : Const
				.filter(e -> !e.getName().equals("ECOSCORE")).sorted((o1, o2) -> o2.getRelativ().getValue().compareTo(o1.getRelativ().getValue())).toList();

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
		 * 
		 * for (Entry<String, AggregatedAttribute> attr :
		 * attributes.getAggregatedAttributes().entrySet()) { sb.append(" -
		 * ").append(attr.getKey().toString()).append(" :
		 * ").append(attr.getValue().getValue()).append("\n"); }
		 * 
		 * for (AggregatedFeature attr : attributes.getFeatures()) { sb.append(" -
		 * ").append(attr.getName().toString()).append("\n"); }
		 * 
		 * for (AggregatedAttribute attr : attributes.getUnmapedAttributes()) {
		 * sb.append(" - ").append(attr.getName().toString()).append(" :
		 * ").append(attr.getValue()).append("\n"); }
		 */

		return sb.toString();

	}

	public List<Resource> pdfs() {
		return resources.stream().filter(e -> e.getResourceType() != null && e.getResourceType().equals(ResourceType.PDF)).toList();
	}

	public String externalCover() {
		String coverPath = "/icons/no-image.png";

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
		return resources.stream().filter(e -> e.getUrl() != null).filter(e -> e.getUrl().endsWith(".jpg") || e.getUrl().endsWith(".png") || e.getUrl().endsWith(".jpeg")).toList();
	}

	// TODO : Should be outsided / cached
	public List<Resource> images() {
		// Filter resources of type IMAGE
		List<Resource> images = resources.stream().filter(e -> e.getResourceType() != null && e.getResourceType().equals(ResourceType.IMAGE)).toList();

		//////////////////////////////////
		// Applying filtering / ordering
		//////////////////////////////////

		// Keep the covers
		List<Resource> covers = images.stream().filter(e -> e.getTags().contains("cover")).toList();
		Set<Integer> coversGroupsId = covers.stream().map(Resource::getGroup).collect(Collectors.toSet());
		Set<Integer> otherGroupsId = images.stream().filter(e -> !coversGroupsId.contains(e.getGroup())).map(Resource::getGroup).collect(Collectors.toSet());

		List<Resource> ret = new ArrayList<>();

		// First, add the best cover images
		coversGroupsId.forEach(coverGroupId -> ret.add(bestByGroup(images, coverGroupId)));

		// Then, add the other images by groups
		otherGroupsId.forEach(otherGroupId -> ret.add(bestByGroup(images, otherGroupId)));

		// TODO : perf : null check Could be avoided
		return ret.stream().filter(e -> null != e).toList();
	}

	/**
	 * Finds the best resource by group based on the number of pixels
	 * 
	 * @param images  The list of images
	 * @param groupId The group identifier
	 * @return The best resource in the group
	 */
	private Resource bestByGroup(List<Resource> images, Integer groupId) {
		return images.stream().filter(image -> image.getGroup() != null && image.getGroup().equals(groupId)) // Filtrer les images du groupe spécifié
				.max(Comparator.comparingInt(image -> image.getImageInfo().pixels())) // Trouver l'image avec le plus de pixels
				.orElse(null); // Retourner null si aucune image n'est trouvée

	}

	public List<Resource> videos() {
		return resources.stream().filter(e -> e.getResourceType() != null && e.getResourceType().equals(ResourceType.VIDEO)).toList();
	}

	public AggregatedPrice bestPrice() {
		return price == null ? null : price.getMinPrice();
	}

	// /**
	// * Return ratings having specific tags
	// *
	// * @param tag
	// * @return
	// */
	// public Set<SourcedRating> ratingsByTag(final String tag) {
	//
	// if (null == tag || null == ratings) {
	// return null;
	// }
	//
	// return ratings.stream().filter(e ->
	// e.getTags().contains(tag)).collect(Collectors.toSet());
	// }

	// /**
	// * Return a rating having specific tags
	// *
	// * @param tag
	// * @return
	// */
	// public SourcedRating ratingByTag(final String tag) {
	//
	// if (null == tag || null == ratings) {
	// return null;
	// }
	//
	// return ratings.stream().filter(e ->
	// e.getTags().contains(tag)).findAny().orElse(null);
	// }

	/**
	 *
	 * @return true if this AggrgatedData has alternateIds
	 */
	public Boolean hasAlternateIds() {
		return modelNames.size() > 0;
	}

	public boolean hasOccasions() {
		return price.getConditions().contains(ProductCondition.OCCASION);
	}

	// /**
	// * Return all the specific ratings
	// *
	// * @return
	// * @throws ResourceNotFoundException
	// */
	// public Set<Rating> ratings(final RatingType ratingType) {
	// return ratings.stream().filter(e ->
	// e.getTags().contains(ratingType.toString())).collect(Collectors.toSet());
	// }

	public String alternateIdsAsText() {
		return StringUtils.join(modelNames, ", ");
	}

	/**
	 *
	 * @return the brand, if availlable from referentiel attributes
	 */
	public String brand() {
		return attributes.getReferentielAttributes().get(ReferentielKey.BRAND);

	}

	/**
	 * TODO : Format from the long + gtin type
	 * 
	 * @return the gtin, if availlable from referentiel attributes
	 */
	public String gtin() {
		try {
			return id + "";
		} catch (final Exception e) {
			return null;
		}
	}


	/**
	 * 
	 * @return a random model name, from the availlable ones
	 */
	public String randomModel() {
		List<String> names = new ArrayList<>();
		names.add(model);
		modelNames.keySet().forEach(e -> names.add(e));
		Random rand = new Random();
		return names.get(rand.nextInt(names.size()));

	}

	// /**
	// * Returns the name (brand - model)
	// */
	// public String name() {
	// return id();
	// }
	//
	/**
	 * Returns the best human readable name
	 */
	public String bestName() {
		String ret;
		if (null == brand() || null == model) {
			ret = names.shortestOfferName();
		} else {
			ret = brandAndModel();
		}

		if (null == ret) {
			ret = gtin();
		}

		return ret;
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
			ret += brand() + "-";
		}

		ret += model;

		return ret;
	}

	public String compensation() {

		Double c = bestPrice() == null ? 0.0 : bestPrice().getCompensation();
		return String.format("%.2f", c);
	}


	/**
	 * TODO : merge with the one on price()
	 * 
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

		String ret = (formatter.print(period));

		return ret;
	}

	/**
	 * Return text version of the creation date
	 * 
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
	 * 
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
			logger.info("Adding new resource : {}", resource);
			resources.add(resource);
		} else {
			logger.info("Updating existing resource : {}", resource);
			// Smart update
			existing.setTags(resource.getTags());
			existing.setHardTags(resource.getHardTags());
			existing.setDatasourceName(resource.getDatasourceName());

			resources.remove(resource);
			resources.add(existing);
		}

	}

	public String url(String language) {
		return names.getUrl().getOrDefault(language, names.getUrl().get("default"));
	}

	/**
	 * Add the model name referentiel attribute :
	 * > applying some spliting mechanism to extract shortest model name when possible
	 * > Set the "official" model name if first time encountered
	 * > Add to the sourced modelNames
	 * cleaning pass
	 * 
	 * @param modelName
	 */
	public void addModelName(String modelName, String datasourceName) {

		String nModel = StringUtils.normalizeSpace(modelName).toUpperCase();

		// TODO(p3,conf) : Model size eviction threshold from conf
		if (StringUtils.isEmpty(modelName) || modelName.length() < 3) {
			return;
		}
		// Splitting on conventionnal suffixes (/ - .)
		// TODO(p3,conf) : Const / conf
		String[] frags = nModel.split("/|\\|.|-");

		modelNames.computeIfAbsent(nModel, k -> new HashSet<>()).add(datasourceName);
		
		if (frags.length > 1) {
			logger.info("Found an alternative model : " + frags[0]);
			modelNames.computeIfAbsent(frags[0], k -> new HashSet<>()).add(datasourceName);
		}

		// Case ref attribute is already set, we keep as it and we remove the elected
		// one from alternativeModels
		// NOTE : Due to the design (this method being called on each new model name), this election mechanism will only applies to extracted variants
		if (StringUtils.isEmpty(model) ) {
			String shortest = shortestModel();
			if (null != shortest) {
				this.model = shortest;
				attributes.getReferentielAttributes().put(ReferentielKey.MODEL, shortest);
			}
		} 
	}

	
	/**
	 * Add the brand : 
	 * > Set the "official" brand name if first time encountered
	 * > Add to the sourced brandNames
	 * cleaning pass
	 * 
	 * @param brandName
	 */
	public void addBrand(String brandName, String datasourceName) {

		String nBrand = StringUtils.stripAccents(StringUtils.normalizeSpace(brandName).toUpperCase());

		brandNames.computeIfAbsent(nBrand, k -> new HashSet<>()).add(datasourceName);

		// Setting the brand if first time encountered
		if (StringUtils.isEmpty(brand) ) {
			this.brand = nBrand;
		} 
	}
	/**
	 * 
	 * @return the shortest model name
	 */
	public String shortestModel() {
		Set<String> names = new HashSet<>();
		names.add(model);
		names.addAll(modelNames.keySet());
		if (names.size() == 0) {
			return null;
		} else {
			return names.stream().filter(e -> e != null).min(Comparator.comparingInt(String::length)).orElse(null);
		}
	}

	//////////////////////////////////////////
	// Getters / Setters
	//////////////////////////////////////////



	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
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

	public MongoGtinInfo getGtinInfos() {
		return gtinInfos;
	}

	public void setGtinInfos(MongoGtinInfo gtinInfos) {
		this.gtinInfos = gtinInfos;
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

	public Map<String, String> getMappedCategories() {
		return mappedCategories;
	}

	public void setMappedCategories(Map<String, String> mappedCategories) {
		this.mappedCategories = mappedCategories;
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



	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public Map<String, Set<String>> getModelNames() {
		return modelNames;
	}

	public void setModelNames(Map<String, Set<String>> modelNames) {
		this.modelNames = modelNames;
	}



	public String getBrand() {
		return brand;
	}

	public void setBrand(String brand) {
		this.brand = brand;
	}

	public Map<String, Set<String>> getBrandNames() {
		return brandNames;
	}

	public void setBrandNames(Map<String, Set<String>> brandNames) {
		this.brandNames = brandNames;
	}

	public boolean isExcluded() {
		return excluded;
	}

	public void setExcluded(boolean excluded) {
		this.excluded = excluded;
	}

}
