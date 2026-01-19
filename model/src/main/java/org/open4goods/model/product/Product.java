
package org.open4goods.model.product;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
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
import org.open4goods.model.Localisable;
import org.open4goods.model.Standardisable;
import org.open4goods.model.StandardiserService;
import org.open4goods.model.attribute.IndexedAttribute;
import org.open4goods.model.attribute.ProductAttribute;
import org.open4goods.model.attribute.ProductAttributes;
import org.open4goods.model.attribute.ReferentielKey;
import org.open4goods.model.eprel.EprelProduct;
import org.open4goods.model.exceptions.ValidationException;
import org.open4goods.model.price.AggregatedPrice;
import org.open4goods.model.price.AggregatedPrices;
import org.open4goods.model.price.Currency;
import org.open4goods.model.resource.Resource;
import org.open4goods.model.resource.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Dynamic;
import org.springframework.data.elasticsearch.annotations.Mapping;
import org.springframework.data.elasticsearch.annotations.Setting;
import org.springframework.data.elasticsearch.annotations.WriteTypeHint;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = Product.DEFAULT_REPO, alwaysWriteMapping = true, createIndex = true, writeTypeHint = WriteTypeHint.FALSE, dynamic = Dynamic.FALSE)
@Setting(settingPath = "/product-settings.json")
@Mapping(mappingPath = "/product-mappings.json")
public class Product implements Standardisable {

	private final static Logger logger = LoggerFactory.getLogger(Product.class);

	/**
	 * Name of the elastic index
	 */
	public static final String DEFAULT_REPO = "products";

	/**
	 * Name of the ecoscore
	 */
	private static final String ECOSCORE_NAME = "ECOSCORE";

	// Should not be used
	// If true, the referentiel attribute will be updated if a shortest version
	// exists in akaModels
	private static final boolean FORCE = false;

	/**
	 * The ID is the gtin
	 */
	@Id
	private Long id;

	/**
	 * The list of external id's for this product
	 */
	private ExternalIds externalIds = new ExternalIds();

	/**
	 * The date this item has been created
	 */
	private long creationDate;

	/**
	 * The last date this product has changed (new data, new comment, * so on...)
	 */
	private long lastChange;

	/** The associated vertical, if any **/
	private String vertical;

	/**
	 * If true, means the item is excluded from vertical representation (because not
	 * enough data, ....)
	 **/
	private boolean excluded = false;

	/** If set that contains cause of exclusion. **/
	private Set<String> excludedCauses = new HashSet<String>();

	/** If associated eprel product if any **/
	private EprelProduct eprelDatas = null;

	/** The list of other model's known for this product **/
	private Set<String> akaModels = new HashSet<>();

	/** The list of other brands known for this product **/
	private Map<String, String> akaBrands = new HashMap<String, String>();

	/** Namings informations for this product **/
	private ProductTexts names = new ProductTexts();

	private Set<String> offerNames = new HashSet<>();

	private ProductAttributes attributes = new ProductAttributes();

	private AggregatedPrices price = new AggregatedPrices();

	/**
	 * The datasources participating to this product, with a hashcode used for
	 * fastening data processing (by skipping already processed items)
	 */
	private Map<String, Long> datasourceCodes = new HashMap<>();

	/**
	 * The media resources for this data
	 */
	private Set<Resource> resources = new HashSet<>();

	private String coverImagePath;

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

	/**
	 * The product category path by datasources
	 */
	private Map<String, String> categoriesByDatasources = new HashMap<String, String>();

	private Map<String, Score> scores = new HashMap<>();



	/**
	 * The ai generated review for this product
	 */
	private Localisable<String, AiReviewHolder> reviews = new Localisable<String, AiReviewHolder>();

	private EcoScoreRanking ranking = new EcoScoreRanking();

	/**
	 * Multimodal embedding vector for semantic search (CLIP = 512 dims)
	 */
	@Field(type = FieldType.Dense_Vector, dims = 512)
	private float[] embedding;

	//////////////////// :
	// Stored (and computed) to help elastic querying / sorting
	////////////////////
	/** number of commercial offers **/
	private Integer offersCount = 0;

	// /**
	// * Informations about participant datas and aggegation process
	// */
	// @Field(index = false, store = false, type = FieldType.Object)
	// private AggregationResult aggregationResult = new AggregationResult();

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

	/**
	 * Removes all data coming from the given datasource. This includes datasource
	 * bookkeeping, category markers and attribute sources. Datasource-specific
	 * payloads (such as EPREL external ids and cached product data) are also
	 * cleared when the datasource matches.
	 *
	 * @param datasourceName the datasource identifier (case-insensitive)
	 */
	public void removeDatasourceData(String datasourceName) {
		if (StringUtils.isBlank(datasourceName)) {
			return;
		}

		akaBrands.remove(datasourceName);

		removeDatasourceFromIndexedAttributes(attributes.getIndexed(), datasourceName);
		removeDatasourceFromProductAttributes(attributes.getAll(), datasourceName);

		datasourceCodes.remove(datasourceName);

		String removedCategory = categoriesByDatasources.remove(datasourceName);
		if (removedCategory != null) {
			datasourceCategories.remove(removedCategory);
		}
		datasourceCategories.remove(datasourceName);

		if ("eprel".equalsIgnoreCase(datasourceName)) {
			externalIds.setEprel(null);
			eprelDatas = null;
		}
	}

	private void removeDatasourceFromIndexedAttributes(Map<String, IndexedAttribute> attributesByKey, String datasourceName) {
		Iterator<Entry<String, IndexedAttribute>> iterator = attributesByKey.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<String, IndexedAttribute> entry = iterator.next();
			IndexedAttribute attribute = entry.getValue();

			boolean removed = attribute.getSource().removeIf(source -> datasourceName.equalsIgnoreCase(source.getDataSourcename()));

			if (!removed) {
				continue;
			}

			if (attribute.getSource().isEmpty()) {
				iterator.remove();
				continue;
			}

			String bestValue = attribute.bestValue();
			attribute.setValue(bestValue);
			attribute.setNumericValue(parseNumericOrNull(bestValue));
			attribute.setBoolValue(IndexedAttribute.getBool(bestValue));
		}
	}

	private void removeDatasourceFromProductAttributes(Map<String, ProductAttribute> attributesByKey, String datasourceName) {
		Iterator<Entry<String, ProductAttribute>> iterator = attributesByKey.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<String, ProductAttribute> entry = iterator.next();
			ProductAttribute attribute = entry.getValue();

			boolean removed = attribute.getSource().removeIf(source -> datasourceName.equalsIgnoreCase(source.getDataSourcename()));

			if (!removed) {
				continue;
			}

			if (attribute.getSource().isEmpty()) {
				iterator.remove();
				continue;
			}

			String bestValue = attribute.bestValue();
			attribute.setValue(bestValue);
		}
	}

	private Double parseNumericOrNull(String rawValue) {
		if (rawValue == null) {
			return null;
		}
		try {
			return Double.valueOf(rawValue.trim().replace(",", "."));
		} catch (NumberFormatException e) {
			return null;
		}
	}

	@Override
	public boolean equals(final Object obj) {

		if (obj instanceof Product) {
			return id == (((Product) obj).getId());
		}

		return super.equals(obj);
	}

	/**
	 * Return a set containing AKA brands if not matching the set brand
	 *
	 * @return
	 */
	public Set<String> akaBrands() {
		return akaBrands.values().stream().filter(e -> !e.equals(brand())).collect(Collectors.toSet());

	}

	public String longestOfferName() {
		return offerNames.stream().max(Comparator.comparingInt(String::length)).get();
	}

	public String shortestOfferName() {
		return offerNames.stream().min(Comparator.comparingInt(String::length)).orElse(null);
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

	public List<Score> virtualScores() {
		List<Score> ret = scores.values().stream().filter(e -> e.getVirtual()).filter(e -> !e.getName().equals(ECOSCORE_NAME)).sorted((o1, o2) -> o2.getRelativ().getValue().compareTo(o1.getRelativ().getValue())).toList();

		return ret;
	}

	public List<Score> realScores() {
		List<Score> ret = scores.values().stream().filter(e -> !e.getVirtual()).filter(e -> !e.getName().equals(ECOSCORE_NAME)).sorted((o1, o2) -> o2.getRelativ().getValue().compareTo(o1.getRelativ().getValue())).toList();

		return ret;
	}

	/**
	 *
	 * @return the ecoscore or null
	 */
	public Score ecoscore() {
		return scores.get(ECOSCORE_NAME);
	}

	public String caracteristics() {

		StringBuilder sb = new StringBuilder();

		for (Entry<ReferentielKey, String> attr : attributes.getReferentielAttributes().entrySet()) {
			sb.append(" - ").append(attr.getKey().toString()).append(" : ").append(attr.getValue()).append("\n");
		}

		for (IndexedAttribute attr : attributes.getIndexed().values()) {
			sb.append(" - ").append(attr.getName().toString()).append(" : ").append(attr.getValue()).append("\n");
		}

		for (ProductAttribute attr : attributes.getAll().values()) {
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
		List<Resource> ret = resources.stream().filter(e -> e.getResourceType() != null && e.getResourceType().equals(ResourceType.PDF)).toList();
		return ret;
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
		return resources.stream().filter(e -> e.getUrl() != null)
				// TODO : Extension from conf
				.filter(e -> e.getUrl().endsWith(".jpg") || e.getUrl().endsWith(".png") || e.getUrl().endsWith(".jpeg") || e.getUrl().endsWith(".webp")).toList();
	}

	// Helper for ui
	public List<String> unprocessedImagesUrl() {
		List<String> ret = unprocessedimages().stream().map(e -> e.getUrl()).toList();
		return ret;
	}

	// TODO(p2,perf) : Should be cached
	public List<Resource> images() {
		Map<Integer, List<Resource>> imagesByGroup = new HashMap<>();
		Map<Integer, Resource> bestByGroup = new HashMap<>();
		Set<Integer> coverGroups = new HashSet<>();
		List<Resource> coverNoGroup = new ArrayList<>();
		List<Resource> otherNoGroup = new ArrayList<>();

		for (Resource r : resources) {
			if (r.getResourceType() != null && r.getResourceType().equals(ResourceType.IMAGE)) {
				Integer groupId = r.getGroup();
				boolean cover = r.getTags().contains("cover");

				if (groupId != null) {
					imagesByGroup.computeIfAbsent(groupId, g -> new ArrayList<>()).add(r);

					Resource best = bestByGroup.get(groupId);
					if (best == null || pixels(r) > pixels(best)) {
						bestByGroup.put(groupId, r);
					}

					if (cover) {
						coverGroups.add(groupId);
					}
				} else {
					if (cover) {
						coverNoGroup.add(r);
					} else {
						otherNoGroup.add(r);
					}
				}
			}
		}

		List<Resource> ret = new ArrayList<>();

		coverGroups.forEach(g -> {
			Resource r = bestByGroup.get(g);
			if (r != null) {
				ret.add(r);
			}
		});

		ret.addAll(coverNoGroup);

		bestByGroup.forEach((g, r) -> {
			if (!coverGroups.contains(g)) {
				ret.add(r);
			}
		});

		ret.addAll(otherNoGroup);

		return ret;
	}

	private int pixels(Resource resource) {
		return resource.getImageInfo() == null ? 0 : resource.getImageInfo().pixels();
	}

	public List<Resource> videos() {
		return resources.stream().filter(e -> e.getResourceType() != null && e.getResourceType().equals(ResourceType.VIDEO)).toList();
	}

	public AggregatedPrice bestPrice() {
		return price == null ? null : price.getMinPrice();
	}

	/**
	 *
	 * @return true if this AggrgatedData has alternateIds
	 */
	public Boolean hasAlternateIds() {
		return akaModels.size() > 0;
	}

	public boolean hasOccasions() {
		return price.getConditions().contains(ProductCondition.OCCASION);
	}

	public String alternateIdsAsText() {
		return StringUtils.join(akaModels, ", ");
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
		try {
			// TODO(p2, features) : Should render with appropriate leading 0
			return String.valueOf(id);
		} catch (final Exception e) {
			System.err.println("NOT A INTEGER GTIN");
			return "-1";
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
		akaModels.forEach(e -> names.add(e));
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
		if (StringUtils.isEmpty(brand()) || StringUtils.isEmpty(model())) {
			ret = shortestOfferName();
		} else {
			ret = brandAndModel();
		}

		if (null == ret) {
			ret = gtin();
		}

		return ret;
	}

	/**
	 * Returns the preferred localized name for this product.
	 * <p>
	 * Resolution order:
	 * <ol>
	 *     <li>short name</li>
	 *     <li>long name</li>
	 *     <li>H1 title</li>
	 *     <li>pretty name</li>
	 *     <li>{@link #bestName()}</li>
	 * </ol>
	 * </p>
	 *
	 * @param language language key (e.g. "en", "fr", or {@code null} for default)
	 * @return the preferred name, never {@code null}
	 */
	public String preferredName(final String language) {
		String resolved = resolveLocalisedName(names == null ? null : names.getShortName(), language);
		if (StringUtils.isNotBlank(resolved)) {
			return resolved;
		}

		resolved = resolveLocalisedName(names == null ? null : names.getLongName(), language);
		if (StringUtils.isNotBlank(resolved)) {
			return resolved;
		}

		resolved = resolveLocalisedName(names == null ? null : names.getH1Title(), language);
		if (StringUtils.isNotBlank(resolved)) {
			return resolved;
		}

		resolved = resolveLocalisedName(names == null ? null : names.getPrettyName(), language);
		if (StringUtils.isNotBlank(resolved)) {
			return resolved;
		}

		return bestName();
	}

	/**
	 * Resolve a localized string from the provided {@link Localisable} container.
	 *
	 * @param localisable localized content
	 * @param language language key (e.g. "en" or "fr")
	 * @return resolved string or empty when missing
	 */
	private String resolveLocalisedName(final Localisable<String, String> localisable, final String language) {
		if (localisable == null || localisable.isEmpty()) {
			return "";
		}

		String resolved = localisable.i18n(language);
		return resolved == null ? "" : resolved.trim();
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
		return datasourceCategories.stream().min(Comparator.comparingInt(String::length)).orElse(null);
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

	public void addResource(final Resource resource) throws ValidationException {

		if (null == resource) {
			return;
		}

		resource.validate();

		// Smart update, time consuming but necessary.

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

	public void forceModel(String extractedModel) {
		String oldModel = model();
		getAttributes().getReferentielAttributes().put(ReferentielKey.MODEL, extractedModel);
		akaModels.add(oldModel);
		akaModels.remove(extractedModel);

	}

	/**
	 * Add the model referentiel attribute, applying some spliting mechanism and
	 * cleaning pass
	 *
	 * @param value
	 */
	public void addModel(String value) {

		String model = StringUtils.normalizeSpace(value).toUpperCase();

		// TODO(conf,p2) : Eviction size from conf
		if (StringUtils.isEmpty(model) || model.length() < 3) {
			return;
		}
		// Splitting on conventionnal suffixes (/ - .)
		// TODO(conf,p2) : splitters from Const / conf
		String[] frags = model.split("/|\\|.|-");

		if (!model.equals(model())) {
			akaModels.add(model);
		}

		if (frags.length > 3) {
			logger.info("Found an alternative model : " + frags[0]);
			akaModels.add(frags[0]);
		}

		// Case ref attribute is already set, we keep as it and we remove the elected
		// one from alternativeModels
		String existing = model();

		if (StringUtils.isEmpty(existing) || FORCE) {
			String shortest = shortestModel();
			if (null != shortest) {
				attributes.getReferentielAttributes().put(ReferentielKey.MODEL, shortest);
				akaModels.remove(shortest);
			}
		} else {
			akaModels.remove(existing);
		}
	}

	/**
	 * Add the brand referentiel attribute, applying some sanitisation mechanism
	 *
	 * @param value
	 */
	public void addBrand(String datasource, String value, Set<String> brandExclusions, Map<String, String> brandsMappings) {

		if (StringUtils.isEmpty(value)) {
			return;
		}

		String brand = StringUtils.stripAccents(StringUtils.normalizeSpace(value)).toUpperCase();

		if (null != brandExclusions && brandExclusions.contains(brand)) {
			logger.info("Excluding brand {}", brand);
			return;
		}

		if (null != brandsMappings) {
			String replacement = brandsMappings.get(brand);
			if (null != replacement) {
				brand = replacement;
				logger.info("replacing brand {} with {}", brand, replacement);
			}
		}

		if (StringUtils.isEmpty(brand())) {
			attributes.addReferentielAttribute(ReferentielKey.BRAND, brand);
		} else {
			if (!akaBrands.values().contains(brand)) {
				akaBrands.put(datasource, brand);
			}

		}
	}

	/**
	 *
	 * @return the shortest model name
	 */
	public String shortestModel() {
		Set<String> names = new HashSet<>();
		names.add(model());
		names.addAll(akaModels);
		if (names.size() == 0) {
			return null;
		} else {
			return names.stream().filter(e -> e != null).min(Comparator.comparingInt(String::length)).orElse(null);
		}
	}

	//////////////////////////////////////////
	// Getters / Setters
	//////////////////////////////////////////

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
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

	public ProductAttributes getAttributes() {
		return attributes;
	}

	public void setAttributes(ProductAttributes attributes) {
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

	public Integer getGoogleTaxonomyId() {
		return googleTaxonomyId;
	}

	public void setGoogleTaxonomyId(Integer googleTaxonomyId) {
		this.googleTaxonomyId = googleTaxonomyId;
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

	public Map<String, String> getCategoriesByDatasources() {
		return categoriesByDatasources;
	}

	public void setCategoriesByDatasources(Map<String, String> categoriesByDatasources) {
		this.categoriesByDatasources = categoriesByDatasources;
	}

	public Map<String, Long> getDatasourceCodes() {
		return datasourceCodes;
	}

	public void setDatasourceCodes(Map<String, Long> datasourceCodes) {
		this.datasourceCodes = datasourceCodes;
	}

	public Map<String, String> getAkaBrands() {
		return akaBrands;
	}

	public void setAkaBrands(Map<String, String> akaBrands) {
		this.akaBrands = akaBrands;
	}

	public Set<String> getAkaModels() {
		return akaModels;
	}

	public void setAkaModels(Set<String> akaModels) {
		this.akaModels = akaModels;
	}

	public Set<String> getExcludedCauses() {
		return excludedCauses;
	}

	public void setExcludedCauses(Set<String> excludedCauses) {
		this.excludedCauses = excludedCauses;
	}



	public Localisable<String, AiReviewHolder> getReviews() {
		return reviews;
	}

	public void setReviews(Localisable<String, AiReviewHolder> aiReviews) {
		this.reviews = aiReviews;
	}

	public EprelProduct getEprelDatas() {
		return eprelDatas;
	}

	public float[] getEmbedding() {
		return embedding;
	}

	public void setEmbedding(float[] embedding) {
		this.embedding = embedding;
	}

	public void setEprelDatas(EprelProduct eprelDatas) {
		this.eprelDatas = eprelDatas;
	}

}
