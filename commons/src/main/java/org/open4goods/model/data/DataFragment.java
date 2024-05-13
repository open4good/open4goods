
package org.open4goods.model.data;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.open4goods.exceptions.ResourceNotFoundException;
import org.open4goods.exceptions.ValidationException;
import org.open4goods.helper.IdHelper;
import org.open4goods.model.Localised;
import org.open4goods.model.Standardisable;
import org.open4goods.model.Validable;
import org.open4goods.model.attribute.Attribute;
import org.open4goods.model.constants.Currency;
import org.open4goods.model.constants.InStock;
import org.open4goods.model.constants.ProductCondition;
import org.open4goods.model.constants.ReferentielKey;
import org.open4goods.services.StandardiserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Objects;
import com.google.common.collect.Sets;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * The main product document a some (lot) of structured fields and some other
 * arbitrary ones.
 * @author Goulven.Furet
 */


public class DataFragment implements Standardisable, Validable {

	public static final String DATAFRAGMENTS_INDEX = "datafragments";

	private static final Logger logger = LoggerFactory.getLogger(DataFragment.class);

	private static final Pattern endingCurrencyParser = Pattern.compile("^.*([EUR|USD|$|€])$");

	private static final Pattern middleCurrencyParser = Pattern.compile("^.+([EUR|USD|$|€]).+$");

	private static final Pattern noCurrency = Pattern.compile("^(([0-9])+([\\.|,|\\s])?([0-9])+)+$");

	/**
	 * The url is the url
	 */
	private String url;

	@NotNull
	private Long lastIndexationDate;

	@NotNull
	private Long creationDate;


	@NotNull
	/**
	 * The merchant (website, or any) providing the offer
	 */
	private String datasourceName;

	/**
	 * The name of the datasource config file
	 */
	private String datasourceConfigName;


	/**
	 * The type of support of this datafragment
	 */
	@NotNull
	private ProviderSupportType providerSupportType;



	/**
	 * The real merchant (website, or any) providing the offer, sometimes
	 * exposed by datasourceName
	 */
	private String merchantName;

	@NotNull
	private Set<String> names = new HashSet<>();

	private Price price;

	/**
	 * The descriptions associated with this product
	 */
	private Set<Description> descriptions = new HashSet<>();

	/**
	 * Product data productTags
	 */
	private String category;

	/** The different ratings **/
	private Set<Rating> ratings = new HashSet<>();

	/** The comments, by provider **/
	private Set<Comment> comments = new HashSet<>();


	/** The questions and answers, by provider **/
	private Set<Question> questions = new HashSet<>();

	/** The affiliated url, if any **/
	private String affiliatedUrl;

	/**
	 * The InStock, if this is a "seller" data
	 */
	private InStock inStock;

	/**
	 * The pros
	 */
	private Set<ProsOrCons> pros = new HashSet<>();

	/**
	 * And the cons
	 */
	private Set<ProsOrCons> cons = new HashSet<>();


	/**
	 * The list of alternate ids for this product
	 * It his built (or completed) at the ui level, when
	 * got matching with longest ids
	 */
	private Set<UnindexedKeyValTimestamp> alternateIds = new HashSet<>();

	/**
	 * The unmapped / uncategorized attributes
	 */
	private Set<Attribute> attributes = new HashSet<>();

	@JsonIgnore
	// For performance. Not exposed, not indexed
	//TODO(gof) : check if exists in es
	private Map<String, Attribute> hashedAttributes = null;

	/**
	 * The referentiel attributes
	 */
	// @Field(index = false, datasourceName = true, type = FieldType.Keyword)
	private Map<ReferentielKey, String> referentielAttributes = new HashMap<>();

	/**
	 * If true, means that the referentiel attributes (eg. brandUid) are to be
	 * considered as the "graal".
	 */
	private Boolean referentielData = false;

	/**
	 * The resources representation
	 */
	private Set<Resource> resources = new HashSet<>();

	private Integer quantityInStock;

	// warranty, in monthes
	private Integer warranty;

	// The shipping time, in days
	private Integer shippingTime;

	private Double shippingCost;

	/**
	 * The state of the product (new, occasion, ...)
	 */
	private ProductCondition productState;

	/**
	 * The price history
	 */
	private List<Price> priceHistory = new ArrayList<>();

	@Override
	public boolean equals(final Object obj) {
		if (obj instanceof DataFragment) {
			return Objects.equal(((DataFragment)obj).getUrl(), url);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return null == url ? 0 : url.hashCode();
	}


	//TODO(design,P3,0.25) : should be replaced by constructor
	/**
	 * Instanciate a new timestamped DataFragment
	 *
	 * @return
	 */
	public static DataFragment newOffer(final String url, final String provider) {
		final DataFragment o = new DataFragment();
		o.setUrl(url);
		o.setDatasourceName(provider);
		o.setLastIndexationDate(System.currentTimeMillis());
		return o;
	}

	/**
	 * Instanciate a new timestamped DataFragment
	 *
	 * @return
	 */
	public static DataFragment newOffer(final String url) {

		return newOffer(url,null);
	}

	@Override
	public Set<Standardisable> standardisableChildren() {
		final Set<Standardisable> ret = new HashSet<>();
		if (null != comments) {
			ret.addAll(comments);
		}
		if (null != ratings) {
			ret.addAll(ratings);
		}
		if (null != price) {
			ret.add(price);
		}

		return ret;
	}

	@Override
	public void standardize(final StandardiserService standardiser,final Currency c) {
		for (final Standardisable s : standardisableChildren()) {
			s.standardize(standardiser,c);
		}
	}

	@Override
	public void validate() throws ValidationException {
		final Set<ValidationMessage> ret = new HashSet<>();

		if (StringUtils.isEmpty(url)) {
			ret.add(ValidationMessage.newValidationMessage("unknown", "NO_URL"));

		}

		//		if (null == providerType) {
		//			ret.add(ValidationMessage.newValidationMessage(url, "NO_DATATYPE"));
		//
		//		}

//		if (StringUtils.isEmpty(category)) {
//			ret.add(ValidationMessage.newValidationMessage(url, "NO_PRODUCTCATEGORY"));
//		}

		if (StringUtils.isEmpty(datasourceName)) {
			ret.add(ValidationMessage.newValidationMessage(url, "NO_SELLER"));
		}


		if (null == lastIndexationDate) {
			ret.add(ValidationMessage.newValidationMessage(url, "NO_DATE"));
		}

		if (null != descriptions) {
			for (final Description d : descriptions) {
				try {
					d.validate();
				} catch (final ValidationException e) {
					ret.addAll(e.getResult());
				}
			}
		}
		if (null != questions) {
			for (final Question d : questions) {
				try {
					d.validate();
				} catch (final ValidationException e) {
					ret.addAll(e.getResult());
				}
			}
		}
		if (null != pros) {
			for (final ProsOrCons d : pros) {
				try {
					d.validate();
				} catch (final ValidationException e) {
					ret.addAll(e.getResult());
				}
			}
		}

		if (null != cons) {
			for (final ProsOrCons d : cons) {
				try {
					d.validate();
				} catch (final ValidationException e) {
					ret.addAll(e.getResult());
				}
			}
		}

		if (null != comments) {
			for (final Comment c : comments) {
				try {
					c.validate();
				} catch (final ValidationException e) {
					ret.addAll(e.getResult());
				}
			}
		}


		if (null != ratings) {
			for (final Rating d : ratings) {
				try {
					d.validate();
				} catch (final ValidationException e) {
					ret.addAll(e.getResult());
				}
			}
		}
		if (null != resources) {
			for (final Resource d : resources) {
				try {
					d.validate();
				} catch (final ValidationException e) {
					ret.addAll(e.getResult());
				}
			}
		}

		if (null != price) {
			try {

				// When price is set, we enforce the need of inStock, product state, ...
				if (null == productState) {
					ret.add(ValidationMessage.newValidationMessage(url,"NO_PRODUCT_STATE"));
				}

				if (null == inStock) {
					ret.add(ValidationMessage.newValidationMessage(url,"NO_INSTOCK"));
				}


				price.validate();

			} catch (final ValidationException e) {
				ret.add(ValidationMessage.newValidationMessage(e.getMessage()));
			}
		}
		if (ret.size() != 0) {
			throw new ValidationException("validation failed", ret);
		}

	}

	/**
	 * Validate with given attributes (and price/names) rules
	 *
	 * @param fields
	 * @throws ValidationException
	 */
	public void validate(final Set<String> fields) throws ValidationException {
		final Set<ValidationMessage> ret = new HashSet<>();
		if (null == fields) {
			return;
		}
		

		try {
			this.validate();
		} catch (final ValidationException e) {
			ret.addAll(e.getResult());
		}
		
		for (final String field : fields) {

			switch (field.toLowerCase()) {

			case "names":
				if (null == names || 0 == names.size()) {
					ret.add(ValidationMessage.newValidationMessage(url, "NO_NAMES"));
				}
				break;


			case "rating":
				if (0 == ratings.size()) {
					ret.add(ValidationMessage.newValidationMessage(url, "NO_RATING"));
				} else {

					try {
						for (final Rating r : ratings) {
							r.validate();
						}
					} catch (final Exception e) {
						ret.add(ValidationMessage.newValidationMessage(url, "INVALID_RATING"));
					}
				}
				break;


			case "price":
				if (null == price) {
					ret.add(ValidationMessage.newValidationMessage(url, "NO_PRICE"));
				} else {

					try {
						price.validate();
					} catch (final Exception e) {
						ret.add(ValidationMessage.newValidationMessage(url, "INVALID_PRICE"));
					}
				}
				break;


			default:

				// Trying on attributes, with non empty

				try {
					final ReferentielKey refKey = ReferentielKey.valueOf(field.toUpperCase());
					//TODO : Always this necessary toString. Should be Enum !!
					if (!StringUtils.isEmpty(referentielAttributes.get(refKey))) {
						continue;
					}
				} catch (final IllegalArgumentException e) {
					// NOTE(gof) : Not nice, bu it handles the "no refkey" from
					// ReferentielKey values
				}

				// Trying on attributes, with non empty

				final Attribute a = getAttribute(field);

				if (null == a || StringUtils.isEmpty(a.getRawValue())) {
					ret.add(ValidationMessage.newValidationMessage(url, "NO_" + field));
					break;
				}
			}
		}

		if (ret.size() > 0) {
			throw new ValidationException("Validation failed", ret);
		}

	}

	@Override
	public String toString() {

		return StringUtils.join(getReferentielAttributes().entrySet(), "| ") + " url:" + getUrl();
	}


	public Double priceAsDouble () {
		return getPrice() != null ? getPrice().getPrice() : null;
	}

	public boolean hasPrice() {
		return getPrice() != null && getPrice().getPrice() != null && getPrice().getCurrency() != null;
	}


	/**
	 * Return the longest name for this offer
	 *
	 * @return
	 */
	public String longestDescription() {
		String ret = null;

		for (final Description n : descriptions) {
			if (null == ret || n.getContent().getText().length() > ret.length()) {
				ret = n.getContent().getText();
			}
		}
		return ret == null ? "" : ret;
	}


	/**
	 * Return the longest name for this offer
	 *
	 * @return
	 */
	public String longestName() {
		String ret = null;

		for (final String n : names) {
			if (null == ret || n.length() > ret.length()) {
				ret = n;
			}
		}

		return ret == null ? "" : ret;
	}


	/**
	 *
	 * TODO  (P1, quality, 1) : Versionning does not work on attributes (maybe comments, and other "complex"). Failing on openfoodfacts, on the added/removedAttributes
	 *
	 * Add a versionned state. The version is the "previous" state
	 * @param newItem
	 */
	public void addVersion(@Valid @NotNull final DataFragment newItem) {

		////////////////////
		// Unversionned data
		////////////////////
		names = newItem.getNames();
		datasourceName = newItem.getDatasourceName();
		datasourceConfigName = newItem.getDatasourceConfigName();
		merchantName = newItem.getMerchantName();
		category = newItem.getCategory();
		referentielData = newItem.getReferentielData();
		providerSupportType = newItem.getProviderSupportType();
		affiliatedUrl = newItem.getAffiliatedUrl();
		inStock = newItem.inStock;
		quantityInStock = newItem.quantityInStock;
		warranty = newItem.getWarranty();
		shippingCost = newItem.shippingCost;
		shippingTime = newItem.getShippingTime();

		alternateIds.addAll(newItem.getAlternateIds());


		// TODO(design, P2, 0.5) : Make update, with hashcode, equals...
		ratings = newItem.getRatings();
		descriptions = newItem.getDescriptions();
		comments = newItem.getComments();
		questions = newItem.getQuestions();
		pros = newItem.getPros();
		cons = newItem.getCons();
		attributes = newItem.getAttributes();
		referentielAttributes = newItem.getReferentielAttributes();
		resources = newItem.getResources();
		lastIndexationDate = newItem.getLastIndexationDate();

		// Adding price history (only for ProductCondition.NEW items)
		if (productState.equals(ProductCondition.NEW) &&  !Objects.equal(getPrice(), newItem.getPrice())) {
			priceHistory.add(getPrice());
			setPrice(newItem.getPrice());
		}

		productState = newItem.getProductState();

	}


	/**
	 * Return the longest name for this offer
	 *
	 * @return
	 */
	public String shortestName() {
		String ret = null;

		for (final String n : names) {
			if (null == ret || n.length() < ret.length()) {
				ret = n;
			}
		}

		return ret == null ? "" : ret;
	}

	/////////////////////////////////////////////////////
	// Attributes adding method
	/////////////////////////////////////////////////////

	public void addQuestion(final Question q) throws ValidationException {
		q.validate();
		questions.add(q);
	}

	public void addComment(final Comment comment) throws ValidationException {
		comment.validate();
		comments.add(comment);
	}

	//	public void addAttribute(final ReferentielKey refKey, final String value, final String language) {
	//		return addAttribute(refKey.toString(), value, language, null);
	//	}

	/**
	 * Add referentiel attributes, with cleaning and alternae id's handling
	 * @param key
	 * @param value
	 */

	public void addReferentielAttribute(ReferentielKey key, String val) {
		referentielAttributes.put(key, val);
		
	}
	

	public void addReferentielAttribute(final String key, final String value) {
		if (StringUtils.isBlank(value)) {
			logger.debug("Cannot add empty referentiel attribute for {} at {}",key,this.url);
			return;
		}

		String val = StringUtils.normalizeSpace(value).toUpperCase();

		//		//TODO(conf, P3,0.25) : Number of spaces from configuration
		//		// If too many spaces, exit
		//		if (StringUtils.countMatches(val," ") > 2) {
		//			logger.warn("Cannot add referentiel attribute, having too many spaces for {}:{} at {}",key,val, this);
		//			return;
		//		}


		switch (key) {
		case "BRAND":
			getReferentielAttributes().put(ReferentielKey.BRAND,val );
			break;
		case "MODEL":
			//			try {
			final String cleaned = IdHelper.getHashedName(val);
			getReferentielAttributes().put(ReferentielKey.MODEL,cleaned);
			if (!cleaned.equals(val)) {
				alternateIds.add(new UnindexedKeyValTimestamp(ReferentielKey.MODEL.toString(),val));
			}
			//			} catch (final InvalidParameterException e) {
			//				logger.warn("{} : cannot add brand for {}",e.getMessage(), this);
			//			}
			break;
		case "GTIN":
			if (NumberUtils.isDigits(val)) {
				getReferentielAttributes().put(ReferentielKey.GTIN,val);
			}else {
				
				// Searching for finishing by ".00", in a few providers
				
				int pos = val.indexOf(".");
				if (pos != - 1) {
					val = val.substring(0,pos);
				}
				
				if (NumberUtils.isDigits(val)) {
					getReferentielAttributes().put(ReferentielKey.GTIN,val);
				} else {					
					logger.info("Cannot add non numeric GTIN, for {} at {}",val, this);
				}
				
			}
			break;
		default:
			logger.error("Cannot add because of unknown referentiel key {} at {}",val, this);
			break;



		}

	}


	/**
	 * Add an attribute
	 *
	 * @return
	 */
	public void addAttribute(final String name, final String value, final String language,
			final Boolean ignoreCariageReturns,final Set<String> multivalueSeparators ) {
		if (null == value || value.isBlank()) {
			logger.debug("Cannot add null or empty values for attribute " + name);
			return ;
		}

		final Attribute attr = new Attribute();

		// Attributye name normalisation
		String sanitizedName =name;
		sanitizedName= StringUtils.normalizeSpace(sanitizedName).toUpperCase();
		// TODO(gof) : From conf, could have the =, ..
		if (sanitizedName.endsWith(":")) {
			sanitizedName = sanitizedName.substring(0, sanitizedName.length() -1).trim();
		}

		attr.setName(sanitizedName);

		if (ignoreCariageReturns.booleanValue()) {
			attr.setRawValue(value.trim().replaceAll("[\\r\\n]+", " "));
		} else {
			attr.setRawValue(value);
		}

		attr.setLanguage(language);



		addAttribute(attr, multivalueSeparators);
	}

	/**
	 * Add an attribute
	 *
	 * @param attr
	 * @return
	 */
	//	public void addAttribute(final Attribute attr) {
	//
	//		return addAttribute(attr, null);
	//	}

	/**
	 * Add attribute and specify separator fields to be used to parse values. If
	 * null, default split values will be used.
	 *
	 * @param attr
	 * @param map
	 * @return
	 */
	private void addAttribute(final Attribute attr, final Set<String> multivalueSeparator) {

		attr.setName(attr.getName().trim().toUpperCase());


		try {
			attr.validate();
		} catch (final ValidationException e) {

			logger.info("Attribute validation failed : {}, {}",attr.getName(), e.getMessage());
		}

		// Parse the values from rawValue

		if (!attributes.add(attr)) {
			logger.info("Attribute conflict for : {}");

			if (null == hashedAttributes) {
				hashedAttributes = new HashMap<>();
			}
			// Adding in the hashed map for performances
			hashedAttributes.put(attr.getName(), attr);
		}

		// Forcing String normalisation
		attr.normalize();

	}

	public void addRating(final Rating rr) throws ValidationException {
		rr.validate();
		ratings.add(rr);
	}

	public void addExpertRating(final Rating rr) throws ValidationException {
		if (null == rr) {
			throw new ValidationException("Trying to add a null expert rating !");
		}

		rr.addTag(RatingType.TECHNICAL);
		addRating(rr);
	}

	public void addUserRating(final Rating rr) throws ValidationException {
		if (null == rr) {
			throw new ValidationException("Trying to add a null rating : " + this);
		}
		rr.addTag(RatingType.SITES);
		addRating(rr);
	}

	public void addRseRating(final Rating rr) throws ValidationException {
		if (null == rr) {
			throw new ValidationException("Trying to add a null rating : " + this);
		}
		rr.addTag(RatingType.RSE);
		addRating(rr);
	}

	public void addProductTags(final List<String> tags) {

		addProductTag(StringUtils.join(tags, ">").toUpperCase().trim());
	}

	public void addProductTag(final String category) {
		if (StringUtils.isEmpty(category)) {
			return;
		}

		this.category = IdHelper.getCategoryName(category);


	}

	public void addDescription(final String description, final String language) {

		if (StringUtils.isEmpty(description)) {
			return;
		}

		if (null == descriptions) {
			descriptions = new HashSet<>();
		}

		descriptions.add(new Description(description, language));

	}

	/**
	 * Attempts an auto detection
	 *
	 * @param url
	 * @throws ValidationException
	 */

	public void addResource(final String url) throws ValidationException {
		addResource(url,new HashSet<String>());
	}


	/**
	 * Attempts an auto detection
	 *
	 * @param url
	 * @throws ValidationException
	 */

	public void addResource(final String url, final String tag) throws ValidationException {
		addResource(url,Sets.newHashSet(tag));
	}


	public void addResource(String url, final Set<String> tags) throws ValidationException {

		if (StringUtils.isEmpty(url)) {
			return;
		}

		if (null == resources) {
			resources = new HashSet<>();
		}

		if (url.startsWith("//")) {
			url = "https:"+url;
		} else if (!url.startsWith("http") && url.startsWith("/")) {
			url = getBaseUrl() + url;
		}

		final Resource r = new Resource(url);

		// A resource is also a source, but not automatically marked
		r.setTimeStamp(System.currentTimeMillis());

		r.setTags(tags);

		r.validate();

		resources.add(r);
	}

	/**
	 * Return the base url, from a long url
	 *
	 * @return
	 */
	private String getBaseUrl() {

		return url.substring(0, url.indexOf('/', 8));
	}

	public void addResource(final Resource resource) throws ValidationException {

		if (null == resource) {
			return;
		}

		if (null == resources) {
			resources = new HashSet<>();
		}

		resource.validate();

		resources.add(resource);
	}

	public void addSubSeller(final String subSeller) throws ParseException {

		if (StringUtils.isEmpty(subSeller)) {
			return;
		}

		merchantName = subSeller;
	}

	public void addName(final String name) {

		if (StringUtils.isEmpty(name)) {
			return;
		}
		names.add(name);
	}

	public void addCon(final String r, final String language, final String author) throws ValidationException {
		final ProsOrCons p = new ProsOrCons();
		p.setLabel(new Localised(r, language));
		p.setAuthor(author);
		p.validate();
		cons.add(p);

	}

	public void addCon(final String r, final String language) throws ValidationException {
		final ProsOrCons p = new ProsOrCons();
		p.setLabel(new Localised(r, language));

		p.validate();
		cons.add(p);

	}

	public void addPro(final String r, final String language, final String author) throws ValidationException {
		final ProsOrCons p = new ProsOrCons();
		p.setLabel(new Localised(r, language));
		p.setAuthor(author);
		p.validate();
		pros.add(p);
	}

	public void addPro(final String r, final String language) throws ValidationException {
		final ProsOrCons p = new ProsOrCons();
		p.setLabel(new Localised(r, language));
		p.validate();
		pros.add(p);
	}

	/**
	 * Set the currency in the price object
	 *
	 * @param currency
	 * @return
	 * @throws ParseException
	 */
	public void setCurrency(final String currency) throws ParseException {

		if (StringUtils.isEmpty(currency)) {
			return ;
		}
		if (null == price) {
			price = new Price();

		}
		price.setCurrency(currency);
	}

	public void setPrice(final String price, final Locale locale) throws ParseException {
		if (StringUtils.isEmpty(price)) {
			//TODO(design,P3,0.25) : throw exception
			return;
		}

		if (null == this.price) {
			this.price = new Price();
		}
		this.price.setPriceValue(price, locale);

	}


	public void setPriceNumeric(final Double price) {
		final Price p = new Price();
		p.setPrice(price);
		this.price = p;
	}





	/**
	 * Return the affiliated url link for this page if availlable, the legacy link if not.
	 * @return
	 */
	public String affiliatedUrlIfPossible() {
		if (StringUtils.isEmpty(affiliatedUrl)) {
			return url;
		} else {
			return affiliatedUrl;
		}
	}

	/**
	 * Add the price. attempt to set price and eventually the currency in a
	 * bestEffort manner
	 *

	 * @return
	 * @throws ParseException
	 */
	public void setPriceAndCurrency(String price, final Locale locale) throws ParseException {
		if (StringUtils.isEmpty(price)) {
			return;
		}

		price =  StringUtils.normalizeSpace(price.toUpperCase());

		// Get the default locale parser


		//TODO(feature,P3,0.25) : Complete with other currencies


		if (price.startsWith("EUR")) {
			setCurrency("EUR");
			price = price.substring(3);
		}



		Matcher m = endingCurrencyParser.matcher(price);
		if (m.find()) {
			setCurrency(m.group(1));
			// NOTE(gof) : probable a better perf way
			price = price.replace(m.group(1), "");
		} else {
			m = middleCurrencyParser.matcher(price);
			if (m.find()) {
				setCurrency(m.group(1));
				price = price.replace(m.group(1), ".");
			}
		}

		// TODO(perf,P3,0.25) Removing spaces chars
		price = price.replace(" ", "").replace(" ", "");

		// Check only "price"
		if (StringUtils.isNumeric(price))  {
			setPrice(price, locale);
		}else if (noCurrency.matcher(price).matches()) {
			setPrice(price, locale);
		} else {
			throw new ParseException("Unable to parse price : " + price + " at " + url, 0);
		}

	}

	/**
	 * Return a rating by it's tag
	 *
	 * @return
	 * @throws ResourceNotFoundException
	 */
	public Rating rating(final String tag) {
		return ratings.stream().filter(e -> e.getTags().contains(tag)).findFirst().orElse(null);
	}

	public Rating rating(final RatingType tag) {
		return rating(tag.toString());
	}


	public List<Rating> ratings(final String tag) {
		final List<Rating> ret = ratings.stream().filter(e -> e.getTags().contains(tag))
				.collect(Collectors.toList());

		return ret;


	}

	//	public Set<Rating> expertSecondaryRating() {
	//		return ratings.stream().filter(e -> e.getType().equals(RatingType.TECHNICAL))
	//				.collect(Collectors.toSet());
	//	}
	//
	//	public Set<Rating>rseSecondaryRating() {
	//		return ratings.stream().filter(e -> e.getType().equals(RatingType.RSE))
	//				.collect(Collectors.toSet());
	//	}

	/**
	 * Remove an attribute
	 *
	 * @param a
	 */
	public void removeAttribute(final Attribute a) {
		attributes.remove(a);
		hashedAttributes.remove(a.getName());

	}

	/**
	 * Return an attribute by it's name
	 *
	 * @param attributeName
	 * @return
	 */
	public Attribute getAttribute(final String attributeName) {

		if (null == hashedAttributes || hashedAttributes.size() != attributes.size()) {
			hashedAttributes = new HashMap<>();
			for (final Attribute a : attributes) {
				hashedAttributes.put(a.getName(), a);
			}
		}
		return hashedAttributes.get(attributeName);
	}



	/**
	 *
	 * @return a "no dot" version for the stores
	 */
	public String shortStore() {

		final int dot = getDatasourceName().lastIndexOf('.');
		if (dot != -1) {
			return getDatasourceName().substring(0, dot);
		} else {
			return getDatasourceName();
		}
	}



	public String brandUid() {
		return referentielAttributes.get(ReferentielKey.MODEL);
	}

	public String gtin (){
		return referentielAttributes.get(ReferentielKey.GTIN);
	}

	public String brand() {
		return referentielAttributes.get(ReferentielKey.BRAND);
	}


	public boolean containsAttribute(String attributeName) {
		return getAttribute(attributeName) != null;
	}

	/**
	 *
	 * @return The hash for affiliation url if found, of classical one otherwhile
	 */
	public String hashedUrl() {
		return IdHelper.encrypt(affiliatedUrlIfPossible());
	}

	public String hashedStore() {
		return IdHelper.encrypt(datasourceName);
	}

	public boolean affiliated() {
		return !StringUtils.isBlank(affiliatedUrl);
	}



	/////////////////////////////////////////////////////////////////////////////////
	// Getter / Setter
	/////////////////////////////////////////////////////////////////////////////////

	public String getUrl() {
		return url;
	}

	public void setUrl(final String url) {
		this.url = url;
	}


	public Set<String> getNames() {
		return names;
	}

	public void setNames(final Set<String> names) {
		this.names = names;
	}

	public String getDatasourceName() {
		return datasourceName;
	}

	public void setDatasourceName(final String providerId) {
		datasourceName = providerId;
	}

	public Long getLastIndexationDate() {
		return lastIndexationDate;
	}

	public void setLastIndexationDate(final Long date) {
		lastIndexationDate = date;
	}

	public Set<Attribute> getAttributes() {
		return attributes;
	}

	public void setAttributes(final Set<Attribute> attributes) {
		this.attributes = attributes;
	}

	public Set<Resource> getResources() {
		return resources;
	}

	public void setResources(final Set<Resource> resources) {
		this.resources = resources;
	}

	public String getMerchantName() {
		return merchantName;
	}

	public void setMerchantName(final String subSeller) {
		merchantName = subSeller;
	}

	public Set<Description> getDescriptions() {
		return descriptions;
	}

	public void setDescriptions(final Set<Description> descriptions) {
		this.descriptions = descriptions;
	}






	public Map<ReferentielKey, String> getReferentielAttributes() {
		return referentielAttributes;
	}

	public void setReferentielAttributes(Map<ReferentielKey, String> referentielAttributes) {
		this.referentielAttributes = referentielAttributes;
	}

	public Price getPrice() {
		return price;
	}

	public void setPrice(final Price price) {
		this.price = price;
	}





	public Set<ProsOrCons> getPros() {
		return pros;
	}

	public void setPros(final Set<ProsOrCons> pros) {
		this.pros = pros;
	}

	public Set<ProsOrCons> getCons() {
		return cons;
	}

	public void setCons(final Set<ProsOrCons> cons) {
		this.cons = cons;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(final String productTags) {
		category = productTags;
	}


	public Set<Question> getQuestions() {
		return questions;
	}

	public void setComments(final Set<Comment> comments) {
		this.comments = comments;
	}

	public Boolean getReferentielData() {
		return referentielData;
	}

	public void setReferentielData(final Boolean referentielData) {
		this.referentielData = referentielData;
	}

	public Set<Rating> getRatings() {
		return ratings;
	}

	public void setRatings(final Set<Rating> ratings) {
		this.ratings = ratings;
	}

	public Set<Comment> getComments() {
		return comments;
	}

	public void setQuestions(final Set<Question> questions) {
		this.questions = questions;
	}




	public Set<UnindexedKeyValTimestamp> getAlternateIds() {
		return alternateIds;
	}

	public void setAlternateIds(Set<UnindexedKeyValTimestamp> alternateIds) {
		this.alternateIds = alternateIds;
	}

	public InStock getInStock() {
		return inStock;
	}

	public void setInStock(final InStock inStock) {
		this.inStock = inStock;
	}

	//	public ProviderType getProviderType() {
	//		return providerType;
	//	}
	//
	//	public void setProviderType(final ProviderType providerType) {
	//		this.providerType = providerType;
	//	}


	public String getAffiliatedUrl() {
		return affiliatedUrl;
	}


	public void setAffiliatedUrl(final String affiliatedUrl) {
		this.affiliatedUrl = affiliatedUrl;
	}

	public Integer getQuantityInStock() {
		return quantityInStock;
	}

	public void setQuantityInStock(final Integer quantityInStock) {
		this.quantityInStock = quantityInStock;
	}

	public Integer getWarranty() {
		return warranty;
	}

	public void setWarranty(final Integer warranty) {
		this.warranty = warranty;
	}


	public Integer getShippingTime() {
		return shippingTime;
	}

	public void setShippingTime(final Integer shippingTime) {
		this.shippingTime = shippingTime;
	}

	public Double getShippingCost() {
		return shippingCost;
	}

	public void setShippingCost(final Double shippingCost) {
		this.shippingCost = shippingCost;
	}

	public ProductCondition getProductState() {
		return productState;
	}

	public void setProductState(final ProductCondition productState) {
		this.productState = productState;
	}


	public ProviderSupportType getProviderSupportType() {
		return providerSupportType;
	}

	public void setProviderSupportType(final ProviderSupportType providerSupportType) {
		this.providerSupportType = providerSupportType;
	}

	public String getDatasourceConfigName() {
		return datasourceConfigName;
	}

	public void setDatasourceConfigName(final String datasourceConfigName) {
		this.datasourceConfigName = datasourceConfigName;
	}


	public Long getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Long creationDate) {
		this.creationDate = creationDate;
	}



}
