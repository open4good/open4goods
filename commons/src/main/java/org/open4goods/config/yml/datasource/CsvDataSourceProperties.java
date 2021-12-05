package org.open4goods.config.yml.datasource;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.config.yml.test.TestCsvLine;
import org.open4goods.model.constants.ReferentielKey;
import org.springframework.validation.annotation.Validated;

@Validated
//TODO(design) : should / could rename config elems
public class CsvDataSourceProperties {

	/**
	 * For datasourceregression tests, allow to specify CSV header rows
	 */
	private String testHeaders;

	@NotNull
	@NotEmpty
	/**
	 * For datasourceregression tests, the conditions to satisfy
	 */
	private List<TestCsvLine> testDatas;

	/**
	 * csvSeparator
	 */
	private char csvSeparator = ';';

	/**
	 * csvQuoteChar
	 */
	private Character csvQuoteChar;

	
	/**
	 * csvQuoteChar
	 */
	private Character csvEscapeChar='"';

	/**
	 * Encoding of the CSV file
	 */	
	private String csvEncoding="UTF8";
	
	
	/**
	 * If true, will use libreoffice headless to sanitize the CSV file
	 */	
	private Boolean csvSanitisation=false;
	
	
		
	
	/**
	 * If defined, only columns (keys) having one of the values will be handled
	 */
	private Map<String, Set<String>> columnsFilter = new HashMap<>();

	/**
	 * The URL's where CSV(s) can be retrieved
	 */
	protected Set<String> datasourceUrls = new HashSet<>();

	/**
	 * If true, means the data is in ZIP format TODO(feature) : auto detect
	 * downloaded format
	 */
	private Boolean ziped = false;

	/**
	 * Column name containing the url of the product (can be null if affiliatedUrl
	 * is empty)
	 */
	protected String url;

	/**
	 * Column name containing the affiliated url
	 */
	protected String affiliatedUrl;


	/**
	 * If set, only the cells having the corresponding values will be included
	 */
	protected Map<String, String> include = new HashMap<>();
	
	/**
	 * If set, only the cells NOT having the corresponding values will be included
	 */
	protected Map<String, String> exclude = new HashMap<>();

	
	/**
	 * If set, those tokens will be replaced in affiliation url's
	 */
	protected Map<String, String> affiliatedUrlReplacementTokens;

	/**
	 * If set, the definitive url will be the extraction of this parameter name in
	 * the url attribute.
	 */
	private String extractUrlFromParam;

	/**
	 * If true, the url will be trimed of its parameters
	 */
	private Boolean trimUrlParameters = false;

	/**
	 * Column name indicating the productCategory
	 */
	protected List<String> productTags;

	/**
	 * If true, all columns of this datasource will ba added as attributes
	 */
	protected Boolean importAllAttributes = false;

	/**
	 * Column name containing the price
	 */
	protected String price;

	/**
	 * The map of column names by referentiel attributes
	 */
	protected Map<ReferentielKey, String> referentiel = new HashMap<>();

	/**
	 * A mapping of CSV cells (by colname) / attribute
	 */
	protected Map<String, String> attributes = new HashMap<>();

	/**
	 * for attributes parsing, wether to ignore (or not) cariage returns in
	 * attributes values
	 */
	private Boolean attributesIgnoreCariageReturns = true;

	/**
	 * for attributes parsing, the split separator to use for values
	 */
	private Set<String> attributesSplitSeparators;

	/**
	 * Column name(s) containing images
	 */
	protected Set<String> image = new HashSet<>();

	/**
	 * The currency, as static expression
	 */
	protected org.open4goods.model.constants.Currency currency;

	/**
	 * Column name containing the name of the product
	 */
	protected String name;

	/**
	 * Column name containing the availlability
	 */
	protected String inStock;

	/**
	 * Column name containing the quantity in stock
	 */
	protected String quantityInStock;

	/**
	 * Column name containing the shipping cost
	 */
	protected String shippingCost;

	/**
	 * Column name containing the shipping time
	 */
	protected String shippingTime;

	/**
	 * Column name containing the warranty
	 */
	protected String warranty;

	/**
	 * Column name that contains product state
	 **/
	protected String productState;

	/**
	 * Column name(s) containing the descriptions
	 */
	protected Set<String> description = new HashSet<>();

	/**
	 * Field where attributes are stored
	 */
	protected String attrs;

	/**
	 * the char used to split multiple attribute values inside
	 */
	protected String attributesSplitChar;

	/**
	 * the char used to split attribute keys and values
	 */

	protected String attributesKeyValSplitChar;

	/**
	 * If set, the key of the attribute will only consiste of the part after attributesKeyKeepAfter, if exists
	 */
	protected String attributesKeyKeepAfter;
	
	/** Describe a rating in CSV format **/
	private CsvRatingConfig rating;

	/**
	 * If set, the CSV extracted DataFragments will be crawled according to this web
	 * crawling configuration
	 */
	private HtmlDataSourceProperties webDatasource;

	protected void addColumn(final String productTag, final Set<String> ret) {
		if (!StringUtils.isEmpty(productTag)) {
			ret.add(productTag);
		}
	}

	public char getCsvSeparator() {
		return csvSeparator;
	}

	public void setCsvSeparator(final char csvSeparator) {
		this.csvSeparator = csvSeparator;
	}



	public Character getCsvQuoteChar() {
		return csvQuoteChar;
	}

	public void setCsvQuoteChar(Character csvQuoteChar) {
		this.csvQuoteChar = csvQuoteChar;
	}

	public Set<String> getDatasourceUrls() {
		return datasourceUrls;
	}

	public void setDatasourceUrls(final Set<String> datasourceUrls) {
		this.datasourceUrls = datasourceUrls;
	}

	public Map<ReferentielKey, String> getReferentiel() {
		return referentiel;
	}

	public void setReferentiel(final Map<ReferentielKey, String> referentiel) {
		this.referentiel = referentiel;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(final String url) {
		this.url = url;
	}

	public String getExtractUrlFromParam() {
		return extractUrlFromParam;
	}

	public void setExtractUrlFromParam(final String extractUrlFromParam) {
		this.extractUrlFromParam = extractUrlFromParam;
	}

	public Boolean getTrimUrlParameters() {
		return trimUrlParameters;
	}

	public void setTrimUrlParameters(final Boolean trimUrlParameters) {
		this.trimUrlParameters = trimUrlParameters;
	}

	public List<String> getProductTags() {
		return productTags;
	}

	public void setProductTags(List<String> productTags) {
		this.productTags = productTags;
	}

	public String getPrice() {
		return price;
	}

	public void setPrice(final String price) {
		this.price = price;
	}

	public org.open4goods.model.constants.Currency getCurrency() {
		return currency;
	}

	public void setCurrency(final org.open4goods.model.constants.Currency currency) {
		this.currency = currency;
	}

	public String getInStock() {
		return inStock;
	}

	public void setInStock(final String inStock) {
		this.inStock = inStock;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
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

	public Set<String> getDescription() {
		return description;
	}

	public void setDescription(final Set<String> description) {
		this.description = description;
	}

	public String getAttrs() {
		return attrs;
	}

	public void setAttrs(final String attrs) {
		this.attrs = attrs;
	}

	public String getAttributesKeyValSplitChar() {
		return attributesKeyValSplitChar;
	}

	public void setAttributesKeyValSplitChar(final String attributesKeyValSplitChar) {
		this.attributesKeyValSplitChar = attributesKeyValSplitChar;
	}

	public HtmlDataSourceProperties getWebDatasource() {
		return webDatasource;
	}

	public void setWebDatasource(final HtmlDataSourceProperties webDatasource) {
		this.webDatasource = webDatasource;
	}

	public String getAttributesSplitChar() {
		return attributesSplitChar;
	}

	public void setAttributesSplitChar(final String attributesSplitChar) {
		this.attributesSplitChar = attributesSplitChar;
	}

	public Boolean getZiped() {
		return ziped;
	}

	public void setZiped(final Boolean ziped) {
		this.ziped = ziped;
	}

	public Set<String> getImage() {
		return image;
	}

	public void setImage(final Set<String> image) {
		this.image = image;
	}

	public String getAffiliatedUrl() {
		return affiliatedUrl;
	}

	public void setAffiliatedUrl(final String affiliatedUrl) {
		this.affiliatedUrl = affiliatedUrl;
	}

	public Map<String, Set<String>> getColumnsFilter() {
		return columnsFilter;
	}

	public void setColumnsFilter(final Map<String, Set<String>> columnsFilter) {
		this.columnsFilter = columnsFilter;
	}

	public String getTestHeaders() {
		return testHeaders;
	}

	public void setTestHeaders(final String testHeaders) {
		this.testHeaders = testHeaders;
	}

	public List<TestCsvLine> getTestDatas() {
		return testDatas;
	}

	public void setTestDatas(final List<TestCsvLine> testDatas) {
		this.testDatas = testDatas;
	}

	public Map<String, String> getAttributes() {
		return attributes;
	}

	public void setAttributes(final Map<String, String> attributes) {
		this.attributes = attributes;
	}

	public Boolean getAttributesIgnoreCariageReturns() {
		return attributesIgnoreCariageReturns;
	}

	public void setAttributesIgnoreCariageReturns(final Boolean attributesIgnoreCariageReturns) {
		this.attributesIgnoreCariageReturns = attributesIgnoreCariageReturns;
	}

	public Set<String> getAttributesSplitSeparators() {
		return attributesSplitSeparators;
	}

	public void setAttributesSplitSeparators(final Set<String> attributesSplitSeparators) {
		this.attributesSplitSeparators = attributesSplitSeparators;
	}

	public String getQuantityInStock() {
		return quantityInStock;
	}

	public void setQuantityInStock(final String quantityInStock) {
		this.quantityInStock = quantityInStock;
	}

	public String getWarranty() {
		return warranty;
	}

	public void setWarranty(final String warranty) {
		this.warranty = warranty;
	}

	public String getProductState() {
		return productState;
	}

	public void setProductState(final String productState) {
		this.productState = productState;
	}

	public CsvRatingConfig getRating() {
		return rating;
	}

	public void setRating(final CsvRatingConfig rating) {
		this.rating = rating;
	}

	public Boolean getImportAllAttributes() {
		return importAllAttributes;
	}

	public void setImportAllAttributes(Boolean importAllAttributes) {
		this.importAllAttributes = importAllAttributes;
	}

	public Map<String, String> getAffiliatedUrlReplacementTokens() {
		return affiliatedUrlReplacementTokens;
	}

	public void setAffiliatedUrlReplacementTokens(Map<String, String> affiliatedUrlReplacementTokens) {
		this.affiliatedUrlReplacementTokens = affiliatedUrlReplacementTokens;
	}

	public String getAttributesKeyKeepAfter() {
		return attributesKeyKeepAfter;
	}

	public void setAttributesKeyKeepAfter(String attributesKeyKeepAfter) {
		this.attributesKeyKeepAfter = attributesKeyKeepAfter;
	}

	public Character getCsvEscapeChar() {
		return csvEscapeChar;
	}

	public void setCsvEscapeChar(Character csvEscapeChar) {
		this.csvEscapeChar = csvEscapeChar;
	}

	public String getCsvEncoding() {
		return csvEncoding;
	}

	public void setCsvEncoding(String csvEncoding) {
		this.csvEncoding = csvEncoding;
	}

	public Boolean getCsvSanitisation() {
		return csvSanitisation;
	}

	public void setCsvSanitisation(Boolean csvSanitisation) {
		this.csvSanitisation = csvSanitisation;
	}

	public Map<String, String> getInclude() {
		return include;
	}

	public void setInclude(Map<String, String> include) {
		this.include = include;
	}

	public Map<String, String> getExclude() {
		return exclude;
	}

	public void setExclude(Map<String, String> exclude) {
		this.exclude = exclude;
	}
	
	

}
