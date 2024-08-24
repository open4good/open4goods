package org.open4goods.commons.config.yml.datasource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.open4goods.commons.model.data.RatingType;

/**
 * @author goulven
 */
public class ExtractorConfig {

	/**
	 * The class used by this extractor
	 */
	private String className;


	///////////////////////////////
	// For categories
	///////////////////////////////
	/**
	 * The xpath exp where to find breadcrumb (fil d'ariane)
	 */
	private String xpathBreadCrumb;
	/**
	 * Char sequence to breadCrumbSpliter the breadcrumb on
	 */
	private String breadCrumbSpliter;


	/** Split urls on it's "path" part, then allow to extract product tags from that **/
	private Boolean urlExtractTags = false;
	private Integer urlExtractTagsFrom = 0;
	private Integer urlExtractTagsTo;


	//////////////////////
	// For deep extractors
	//////////////////////
	/**
	 * Only used by DeepExtractor, to aggregate data from "deep nested" url's
	 */
	private List<ExtractorConfig> extractors;

	/**
	 * The parameter that must be incremented in a page manner
	 */
	private String parameterPage;

	/**
	 * XpathExpression or fixed value that will provides the url to be deep extracted
	 */
	private String url;

	/**
	 * If set, those tokens will be replaced in url (for deep extraction)
	 */
	private Map<String,String> urlReplacement;


	/**
	 * For deep extraction. If set, the numeric between pathIncrementVariablePrefix and pathIncrementVariableSuffix will be incremented, and then replaced in the original url.
	 * eg: handle https://www.darty.com/nav/achat/hifi_video/televiseurs-led/grand_ecran_led/avis_1__samsung_ue28j4100.htmlxxxxxxxxxxxxxxxxxxxxxxx
	 */
	private String pathIncrementVariablePrefix;

	/**
	 * For deep extraction. If set, the numeric between pathIncrementVariablePrefix and pathIncrementVariableSuffix will be incremented, and then replaced in the original url.
	 * eg: handle https://www.darty.com/nav/achat/hifi_video/televiseurs-led/grand_ecran_led/avis_1__samsung_ue28j4100.htmlxxxxxxxxxxxxxxxxxxxxxxx
	 */
	private String pathIncrementVariableSuffix;


	/**
	 * The number the pagination must starts
	 */
	private Integer startPage=0;

	/**
	 * The max number of pages that will be sub fetched
	 */
	private Integer pageLimit = 200;

	/**
	 * -If true, will reverse order the extracted breadcrumb.
	 */
	private boolean reverseOrder = false;

	//////////////////////
	// For jsonld extractor
	///////////////////////
	/**
	 * xpath to the jsonld structure
	 */
	private String xpathJsonLd;

	/**
	 * If set, will collect the multiple json+ld descriptions and use the one that contains the following string
	 */
	private String xpathJsonLdMustContains;


	/**
	 *
	 */
	private Set<String> attributeSeparators;

	private Boolean ignoreCariageReturns = false;

	//////////////////////////////////////////////////
	// For attributes table extractor : keys/values
	// xpath exp
	//////////////////////////////////////////////////
	private String xpathKeys;
	private String xpathSplitChars;

	private String xpathValues;

	private String xpathTable;

	private String gtin13;
	private String subseller;
	//////////////////////////////////////////////////
	// Attributes expressions (xpath, jsonp,..)
	//////////////////////////////////////////////////

	private String brandUid;
	private String category;
	// If defined, the indice to start category breadcrumbs aggregation
	private Integer  categoryFrom;
	private Integer  categoryTo;

	private String description;
	private String price;
	private String currency;
	private String image;
	private String name;
	private String brand;
	/** In case where the brand suffixes the id **/
	private String brandAndId;
	/** Apply only if brandAndId is set. Allows to remove part of text.  **/
	private String brandAndIdRemoval;

	/** In case where the brand suffixes the id, the separator char **/
	private String brandAndIdSeparator = " ";

	private RatingConfig expertRating;
	private RatingConfig userRating;
	private RatingConfig rseRating;
	private CommentsProperties commentsProperties;

	private QuestionsConfig questionsConfig;

	// custom attributes
	private Map<String, String> attributes = new HashMap<>();

	/**
	 * For jsVar extraction, default js equal sign
	 */
	private String equalSign="=";


	//////////////////////////
	// For extracting labeled review
	// from XPath
	//////////////////////////
	private String reviewXpathLabel;
	private String reviewXpathValue;
	private Integer reviewXpathMinValue;
	private Double reviewXpathMaxValue;
	private RatingType reviewXpathType;



	//////////////////////////////
	// Pros and cons (for xpath)
	//////////////////////////////
	private String pros;
	private String cons;

	/** A set of xpath exp pointing to resources url's **/
	private List<String> resources = new ArrayList<>();

	/** A mapping of string replacement in resource names (eg. changing size of images in URL **/
	private Map<String,String> resourceReplacements = new HashMap<>();


	/** For the resource extractor, url must contains **/
	private String resourceUrlMustContains;


	/**
	 * If false, data returned by this extractor will not be sanitized
	 */
	private Boolean sanitize = true;


	/**
	 * The exp for inStock
	 */
	private String inStock;


	/**
	 * The path to the warranty
	 */
	private String warranty;

	/**
	 * The path to the shipping cost
	 */
	private String 	shippingCost;

	/**
	 * The path to the shipping time
	 */
	private String  shippingTime;


	/**
	 * The path to the product state (neuf, reconsitionné, ...)
	 */
	private String  productState;




	//////////////////////////////////////////////////////////////
	// Getters / Setters
	//////////////////////////////////////////////////////////////

	public String getClassName() {
		return className;
	}

	public void setClassName(final String className) {
		this.className = className;
	}


	public String getXpathJsonLd() {
		return xpathJsonLd;
	}

	public void setXpathJsonLd(final String xpathJsonLd) {
		this.xpathJsonLd = xpathJsonLd;
	}

	public String getXpathKeys() {
		return xpathKeys;
	}

	public void setXpathKeys(final String xpathKeys) {
		this.xpathKeys = xpathKeys;
	}

	public String getXpathValues() {
		return xpathValues;
	}

	public void setXpathValues(final String xpathValues) {
		this.xpathValues = xpathValues;
	}

	public String getGtin13() {
		return gtin13;
	}

	public void setGtin13(final String gtin13) {
		this.gtin13 = gtin13;
	}

	public String getSubseller() {
		return subseller;
	}

	public void setSubseller(final String subseller) {
		this.subseller = subseller;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(final String category) {
		this.category = category;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	public String getPrice() {
		return price;
	}

	public void setPrice(final String price) {
		this.price = price;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(final String currency) {
		this.currency = currency;
	}

	public String getImage() {
		return image;
	}

	public void setImage(final String image) {
		this.image = image;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public Map<String, String> getAttributes() {
		return attributes;
	}

	public void setAttributes(final Map<String, String> attributes) {
		this.attributes = attributes;
	}

	public String getBrandUid() {
		return brandUid;
	}

	public void setBrandUid(final String vendorUid) {
		brandUid = vendorUid;
	}

	public RatingConfig getExpertRating() {
		return expertRating;
	}

	public void setExpertRating(final RatingConfig globalReviewRating) {
		expertRating = globalReviewRating;
	}

	public String getBrand() {
		return brand;
	}

	public void setBrand(final String brand) {
		this.brand = brand;
	}

	public String getXpathBreadCrumb() {
		return xpathBreadCrumb;
	}

	public void setXpathBreadCrumb(final String xpathBreadCrumb) {
		this.xpathBreadCrumb = xpathBreadCrumb;
	}

	public String getBreadCrumbSpliter() {
		return breadCrumbSpliter;
	}

	public void setBreadCrumbSpliter(final String split) {
		breadCrumbSpliter = split;
	}

	public boolean isReverseOrder() {
		return reverseOrder;
	}

	public void setReverseOrder(final boolean reverseOrder) {
		this.reverseOrder = reverseOrder;
	}

	public String getXpathTable() {
		return xpathTable;
	}

	public void setXpathTable(final String xpathClassifier) {
		xpathTable = xpathClassifier;
	}

	public String getBrandAndId() {
		return brandAndId;
	}

	public void setBrandAndId(final String brandAndId) {
		this.brandAndId = brandAndId;
	}

	public String getBrandAndIdSeparator() {
		return brandAndIdSeparator;
	}

	public void setBrandAndIdSeparator(final String brandAndIdSeparator) {
		this.brandAndIdSeparator = brandAndIdSeparator;
	}

	public String getReviewXpathLabel() {
		return reviewXpathLabel;
	}

	public void setReviewXpathLabel(final String reviewXpathLabel) {
		this.reviewXpathLabel = reviewXpathLabel;
	}

	public String getReviewXpathValue() {
		return reviewXpathValue;
	}

	public void setReviewXpathValue(final String reviewXpathValue) {
		this.reviewXpathValue = reviewXpathValue;
	}

	public Integer getReviewXpathMinValue() {
		return reviewXpathMinValue;
	}

	public void setReviewXpathMinValue(final Integer reviewXpathMinValue) {
		this.reviewXpathMinValue = reviewXpathMinValue;
	}

	public Double getReviewXpathMaxValue() {
		return reviewXpathMaxValue;
	}

	public void setReviewXpathMaxValue(final Double reviewXpathMaxValue) {
		this.reviewXpathMaxValue = reviewXpathMaxValue;
	}

	public List<String> getResources() {
		return resources;
	}

	public void setResources(final List<String> resources) {
		this.resources = resources;
	}

	public String getPros() {
		return pros;
	}

	public void setPros(final String pros) {
		this.pros = pros;
	}

	public String getCons() {
		return cons;
	}

	public void setCons(final String cons) {
		this.cons = cons;
	}

	public RatingConfig getUserRating() {
		return userRating;
	}

	public void setUserRating(final RatingConfig userRating) {
		this.userRating = userRating;
	}

	public CommentsProperties getCommentsProperties() {
		return commentsProperties;
	}

	public void setCommentsProperties(final CommentsProperties commentsProperties) {
		this.commentsProperties = commentsProperties;
	}

	public QuestionsConfig getQuestionsConfig() {
		return questionsConfig;
	}

	public void setQuestionsConfig(final QuestionsConfig questionsConfig) {
		this.questionsConfig = questionsConfig;
	}

	public Boolean getUrlExtractTags() {
		return urlExtractTags;
	}

	public void setUrlExtractTags(final Boolean urlExtractTags) {
		this.urlExtractTags = urlExtractTags;
	}

	public Integer getUrlExtractTagsFrom() {
		return urlExtractTagsFrom;
	}

	public void setUrlExtractTagsFrom(final Integer urlExtractTagsFrom) {
		this.urlExtractTagsFrom = urlExtractTagsFrom;
	}

	public Integer getUrlExtractTagsTo() {
		return urlExtractTagsTo;
	}

	public void setUrlExtractTagsTo(final Integer urlExtractTagsTo) {
		this.urlExtractTagsTo = urlExtractTagsTo;
	}

	public String getResourceUrlMustContains() {
		return resourceUrlMustContains;
	}

	public void setResourceUrlMustContains(final String resourceUrlMustContains) {
		this.resourceUrlMustContains = resourceUrlMustContains;
	}

	public Integer getCategoryFrom() {
		return categoryFrom;
	}

	public void setCategoryFrom(final Integer categoryFrom) {
		this.categoryFrom = categoryFrom;
	}

	public Integer getCategoryTo() {
		return categoryTo;
	}

	public void setCategoryTo(final Integer categoryTo) {
		this.categoryTo = categoryTo;
	}

	public String getXpathSplitChars() {
		return xpathSplitChars;
	}

	public void setXpathSplitChars(final String xpathSplitChars) {
		this.xpathSplitChars = xpathSplitChars;
	}

	public RatingType getReviewXpathType() {
		return reviewXpathType;
	}

	public void setReviewXpathType(final RatingType reviewXpathType) {
		this.reviewXpathType = reviewXpathType;
	}

	public String getBrandAndIdRemoval() {
		return brandAndIdRemoval;
	}

	public void setBrandAndIdRemoval(final String brandAndIdRemoval) {
		this.brandAndIdRemoval = brandAndIdRemoval;
	}

	public RatingConfig getRseRating() {
		return rseRating;
	}

	public void setRseRating(final RatingConfig rseRating) {
		this.rseRating = rseRating;
	}

	public String getInStock() {
		return inStock;
	}

	public void setInStock(final String inStock) {
		this.inStock = inStock;
	}

	public List<ExtractorConfig> getExtractors() {
		return extractors;
	}

	public void setExtractors(final List<ExtractorConfig> extractors) {
		this.extractors = extractors;
	}

	public String getParameterPage() {
		return parameterPage;
	}

	public void setParameterPage(final String parameterPage) {
		this.parameterPage = parameterPage;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(final String url) {
		this.url = url;
	}

	public Integer getPageLimit() {
		return pageLimit;
	}

	public void setPageLimit(final Integer pageLimit) {
		this.pageLimit = pageLimit;
	}

	public Boolean getSanitize() {
		return sanitize;
	}

	public void setSanitize(final Boolean sanitize) {
		this.sanitize = sanitize;
	}

	public Boolean getIgnoreCariageReturns() {
		return ignoreCariageReturns;
	}

	public void setIgnoreCariageReturns(final Boolean ignoreCariageReturns) {
		this.ignoreCariageReturns = ignoreCariageReturns;
	}

	public Set<String> getAttributeSeparators() {
		return attributeSeparators;
	}

	public void setAttributeSeparators(final Set<String> attributeSeparators) {
		this.attributeSeparators = attributeSeparators;
	}

	public Map<String, String> getResourceReplacements() {
		return resourceReplacements;
	}

	public void setResourceReplacements(final Map<String, String> resourceReplacements) {
		this.resourceReplacements = resourceReplacements;
	}

	public String getWarranty() {
		return warranty;
	}

	public void setWarranty(final String warranty) {
		this.warranty = warranty;
	}

	public String getShippingCost() {
		return shippingCost;
	}

	public void setShippingCost(final String shippingCost) {
		this.shippingCost = shippingCost;
	}

	public String getShippingTime() {
		return shippingTime;
	}

	public void setShippingTime(final String shippingTime) {
		this.shippingTime = shippingTime;
	}

	public String getProductState() {
		return productState;
	}

	public void setProductState(final String productState) {
		this.productState = productState;
	}

	public String getXpathJsonLdMustContains() {
		return xpathJsonLdMustContains;
	}

	public void setXpathJsonLdMustContains(final String xpathJsonLdMustContains) {
		this.xpathJsonLdMustContains = xpathJsonLdMustContains;
	}

	public Map<String, String> getUrlReplacement() {
		return urlReplacement;
	}

	public void setUrlReplacement(final Map<String, String> urlReplacement) {
		this.urlReplacement = urlReplacement;
	}


	public String getPathIncrementVariablePrefix() {
		return pathIncrementVariablePrefix;
	}

	public void setPathIncrementVariablePrefix(final String pathIncrementVariablePrefix) {
		this.pathIncrementVariablePrefix = pathIncrementVariablePrefix;
	}

	public String getPathIncrementVariableSuffix() {
		return pathIncrementVariableSuffix;
	}

	public void setPathIncrementVariableSuffix(final String pathIncrementVariableSuffix) {
		this.pathIncrementVariableSuffix = pathIncrementVariableSuffix;
	}

	public Integer getStartPage() {
		return startPage;
	}

	public void setStartPage(final Integer startPage) {
		this.startPage = startPage;
	}

	public String getEqualSign() {
		return equalSign;
	}

	public void setEqualSign(final String equalSign) {
		this.equalSign = equalSign;
	}


}
