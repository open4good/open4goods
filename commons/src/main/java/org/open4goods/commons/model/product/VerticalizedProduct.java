
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
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DurationFieldType;
import org.joda.time.Period;
import org.joda.time.PeriodType;
import org.joda.time.format.PeriodFormat;
import org.joda.time.format.PeriodFormatter;
import org.open4goods.commons.exceptions.ValidationException;
import org.open4goods.commons.helper.IdHelper;
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
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Dynamic;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;
import org.springframework.data.elasticsearch.annotations.WriteTypeHint;
/**
 * This object modelize a repository mapping for a verticalized product. 
 */
@Document(indexName = "noop", createIndex = true, writeTypeHint = WriteTypeHint.FALSE, dynamic = Dynamic.FALSE)
@Setting(settingPath = "/elastic-verticalized-product-settings.json")
public class VerticalizedProduct {

	private final static Logger logger = LoggerFactory.getLogger(VerticalizedProduct.class);

	/**
	 * The ID is the gtin
	 */
	@Id
	private long id;

	/**
	 * The list of external id's for this product
	 */
	@Field(enabled = false, store = false, type = FieldType.Object)
	private ExternalIds externalIds = new ExternalIds();

	/**
	 * The date this item has been created
	 */
	@Field(index = false, store = false, type = FieldType.Date)
	private long creationDate;

	/**
	 * The last date this product has changed (new data, price change, new comment,
	 * * so on...)
	 */
	@Field(type = FieldType.Date)
	private long lastChange;

	/** The associated vertical, if any **/
	@Field(index = true, store = false, type = FieldType.Keyword)
	private String vertical;

	/**
	 * If true, means the item is excluded from vertical representation (because not
	 * enough data, ....)
	 **/
	@Field(index = true, store = false, type = FieldType.Boolean)
	private boolean excluded = false;

	/** The list of other model's known for this product **/
	@Field(index = false, store = false, type = FieldType.Keyword)
	private Set<String> altModels = new HashSet<>();

	/**
	 * The list of other id's known for this product, associated with datasources
	 **/
	@Field(enabled = false, store = false, type = FieldType.Object)
	private Map<String, Set<String>> altBrands = new HashMap<>();

	/** Namings informations for this product **/
	// TODO : move the offernames inside
	// TODO : Could be a better name
	@Field(enabled = false, store = false, type = FieldType.Object)
	private ProductTexts names = new ProductTexts();

	// @Field(index = false, store = false, type = FieldType.Object)
	// /** The comments, aggregated and nlp processed **/
	// private AggregatedComments comments = new AggregatedComments();

	@Field(enabled = false, store = false, type = FieldType.Object)
	private AggregatedAttributes attributes = new AggregatedAttributes();

	@Field(enabled = false, store = false, type = FieldType.Object)
	private AggregatedPrices price = new AggregatedPrices();

	@Field(index = false, store = false, type = FieldType.Keyword)
	private Set<String> datasourceNames = new HashSet<>();

	/**
	 * The media resources for this data
	 */
	@Field(enabled = false, store = false, type = FieldType.Object)
	private Set<Resource> resources = new HashSet<>();

	@Field(index = false, store = false, type = FieldType.Keyword)
	private String coverImagePath;

	/** The ai generated texts, keyed by language **/
	@Field(enabled = false, store = false, type = FieldType.Object)
	private Localisable<String, AiDescriptions> genaiTexts = new Localisable<>();

	/**
	 * Informations and resources related to the gtin
	 */
	@Field(enabled = false, store = false, type = FieldType.Object)
	private GtinInfo gtinInfos = new GtinInfo();

	/**
	 * The google taxonomy id
	 */
	@Field(index = false, store = false, type = FieldType.Integer)
	private Integer googleTaxonomyId;

	/**
	 * The set of participating "productCategories", on datasources that build this
	 * aggregatedData
	 */
	@Field(index = true, store = false, type = FieldType.Keyword)
	private Set<String> categories = new HashSet<>();

	/**
	 * The encountered categories, by datasources
	 */
	@Field(enabled = false, store = false, type = FieldType.Object)
	private Map<String, String> dsCategories = new HashMap<>();

	@Field(enabled = false, store = false, type = FieldType.Object)
	private Map<String, Score> scores = new HashMap<>();

	@Field(enabled = false, store = false, type = FieldType.Object)
	private EcoScoreRanking ranking = new EcoScoreRanking();

	//////////////////// :
	// Stored (and computed) to help elastic querying / sorting
	////////////////////
	/** number of commercial offers **/
	@Field(index = true, store = false, type = FieldType.Integer)
	private Integer offersCount = 0;

	// /**
	// * Informations about participant datas and aggegation process
	// */
	// @Field(index = false, store = false, type = FieldType.Object)
	// private AggregationResult aggregationResult = new AggregationResult();



	@Override
	public String toString() {
		return "id:" + id;
	}

	@Override
	public boolean equals(final Object obj) {

		if (obj instanceof Product) {
			return id == ((Product) obj).getId();
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

		for (AggregatedAttribute attr : attributes.getUnmatchedAttributes()) {

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
	public Boolean hasAlternativModelNames() {
		return altModels.size() > 0;
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
		return StringUtils.join(altModels, ", ");
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
		// TODO(p2,design) : Use gtin info to format to the according gtin type
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
		List<String> names = new ArrayList<>();
		names.add(model());
		altModels.forEach(e -> names.add(e));
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
		if (null == brand() || null == model()) {
			ret = names.shortestOfferName();
		} else {
			ret = brandAndModel();
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

		Set<String> ret = new HashSet<>(categories.stream().toList());

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
			ret += brand() + "-";
		}

		ret += model();

		return ret;
	}

	public String compensation() {

		Double c = bestPrice() == null ? 0.0 : bestPrice().getCompensation();
		return String.format("%.2f", c);
	}

	/**
	 *
	 * @return The shortest category for this product
	 */
	public String shortestCategory() {
		return categories.stream().min(Comparator.comparingInt(String::length)).orElse(null);
	}

	// /**
	// *
	// * @return the id
	// */
	// public String id() {
	// StringBuilder builder = new StringBuilder();
	//
	// if (null == brand() || null == model()) {
	// builder.append(gtin());
	// } else {
	// builder.append(brand()).append("-").append(model());
	// }
	// return builder.toString();
	// }

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
	 * 
	 * @return the shortest model name
	 */
	public String shortestModel() {
		Set<String> names = new HashSet<>();
		names.add(model());
		names.addAll(altModels);
		if (names.size() == 0) {
			return null;
		} else {
			return names.stream().filter(e -> e != null).min(Comparator.comparingInt(String::length)).orElse(null);
		}
	}

	/**
	 * 
	 * @param datasourceName
	 * @param brand
	 */
	public void addBrand(String datasourceName, String brand) {

		if (StringUtils.isEmpty(brand)) {
			return;
		}
		String name = IdHelper.brandName(brand);

		if (null == brand()) {
			getAttributes().addReferentielAttribute(ReferentielKey.BRAND, name);
		}

		altBrands.computeIfAbsent(name, k -> new HashSet<>()).add(datasourceName);

	}

	//////////////////////////////////////////
	// Getters / Setters
	//////////////////////////////////////////

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

	public Map<String, Set<String>> getAltBrands() {
		return altBrands;
	}

	public void setAltBrands(Map<String, Set<String>> alternativeBrands) {
		this.altBrands = alternativeBrands;
	}

	public Set<String> getCategories() {
		return categories;
	}

	public void setCategories(Set<String> categories) {
		this.categories = categories;
	}

	public Map<String, String> getDsCategories() {
		return dsCategories;
	}

	public void setDsCategories(Map<String, String> datasourceCategories) {
		this.dsCategories = datasourceCategories;
	}

	public void setId(long id) {
		this.id = id;
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

	public Set<String> getAltModels() {
		return altModels;
	}

	public void setAltModels(Set<String> alternativeModels) {
		this.altModels = alternativeModels;
	}

	public boolean isExcluded() {
		return excluded;
	}

	public void setExcluded(boolean excluded) {
		this.excluded = excluded;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

}