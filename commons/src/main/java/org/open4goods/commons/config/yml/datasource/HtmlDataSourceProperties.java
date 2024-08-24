package org.open4goods.commons.config.yml.datasource;

import java.util.List;
import java.util.Set;

import org.open4goods.commons.config.yml.test.TestUrl;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * Specific configuration for a Web (crawlable) DataSource
 * @author Goulven.Furet
 *
 */
@Validated
public class HtmlDataSourceProperties {

	@NotBlank
	private List<ExtractorConfig> extractors;

	@NotBlank
	// The baseUrl of the site. WITHOUT ENDING "/"
	private String baseUrl;

	@NotNull
	@NotEmpty
	private Set<TestUrl> testUrls;

	//	/**
	//	 * If set, this datasource will only crawl the following url's
	//	 */
	//	private List<TestUrl> fixedUrls = new ArrayList<TestUrl>();

	/**
	 * If set, only url containing one of this string will be folllowed
	 */
	private List<String> urlContainsFilter;

	/**
	 * If set,  url containing one of this string will not be followed
	 */
	private List<String> urlExclusionsFilter;


	/**
	 * If set, only url containing one of this string will be processed with extrcators
	 */
	private List<String> urlContainsExtractionFilter;

	/**
	 * If set,  url containing one of this string will not be processed with extrcators
	 */
	private List<String> urlExclusionsExtractionFilter;


	/**
	 * If true, this is a brand oriented crawl
	 */
	private boolean brand2csv = false;
	

	/**
	 * If set, the url's will be provided through a custom provider class, that must implement CustomUrlProvider onterface
	 */
	private String customUrlProviderClass;
	

	
	
	/**
	 * If set, those url's will be added to the baseurl as initial crawl url's
	 */
	@NotNull
	@NotEmpty
	private List<String> initialUrls;



	private CapuleRobotstxtProperties robotsConfig;

	private CrawlProperties crawlConfig = new CrawlProperties() ;

	//	private XpathExtractorConfig xpathExtractorConfig = new XpathExtractorConfig();

	// private MockConfig mockConfig = new MockConfig();


	/**
	 * If true, XpathExtractor (that ALWAYS extracts the productTags) will break the
	 * execution of following extractors if no productTags has been found. It allows
	 * to optimze by bypassing some extractors
	 */
	// TODO : in conf in each datasource
	private Boolean evictIfNoXpathCategory = true;

	/**
	 * Return true if provided url match a test url
	 * @param url
	 * @return
	 */
	public boolean containsTestUrl(final String url) {
		for (final TestUrl tu : testUrls) {
			if (tu.getUrl().equals(url)) {
				return true;
			}
		}
		return false;
	}

	public List<ExtractorConfig> getExtractors() {
		return extractors;
	}

	public void setExtractors(final List<ExtractorConfig> extractors) {
		this.extractors = extractors;
	}

	public String getBaseUrl() {
		return baseUrl;
	}

	public void setBaseUrl(final String baseUrl) {
		this.baseUrl = baseUrl;
	}


	//	public List<TestUrl> getFixedUrls() {
	//		return fixedUrls;
	//	}
	//
	//	public void setFixedUrls(List<TestUrl> fixedUrls) {
	//		this.fixedUrls = fixedUrls;
	//	}

	public List<String> getUrlContainsFilter() {
		return urlContainsFilter;
	}

	public void setUrlContainsFilter(final List<String> urlContainsFilter) {
		this.urlContainsFilter = urlContainsFilter;
	}

	public List<String> getInitialUrls() {
		return initialUrls;
	}

	public void setInitialUrls(final List<String> initialUrls) {
		this.initialUrls = initialUrls;
	}



	public CapuleRobotstxtProperties getRobotsConfig() {
		return robotsConfig;
	}

	public void setRobotsConfig(final CapuleRobotstxtProperties robotsConfig) {
		this.robotsConfig = robotsConfig;
	}

	public CrawlProperties getCrawlConfig() {
		return crawlConfig;
	}

	public void setCrawlConfig(final CrawlProperties crawlConfig) {
		this.crawlConfig = crawlConfig;
	}

	//	public XpathExtractorConfig getXpathExtractorConfig() {
	//		return xpathExtractorConfig;
	//	}
	//
	//	public void setXpathExtractorConfig(final XpathExtractorConfig xpathExtractorConfig) {
	//		this.xpathExtractorConfig = xpathExtractorConfig;
	//	}


	public Boolean getEvictIfNoXpathCategory() {
		return evictIfNoXpathCategory;
	}

	public void setEvictIfNoXpathCategory(final Boolean evictIfNoXpathCategory) {
		this.evictIfNoXpathCategory = evictIfNoXpathCategory;
	}


	public Set<TestUrl> getTestUrls() {
		return testUrls;
	}

	public void setTestUrls(final Set<TestUrl> testUrls) {
		this.testUrls = testUrls;
	}

	public List<String> getUrlExclusionsFilter() {
		return urlExclusionsFilter;
	}

	public void setUrlExclusionsFilter(final List<String> urlExclusionsFilter) {
		this.urlExclusionsFilter = urlExclusionsFilter;
	}

	public List<String> getUrlContainsExtractionFilter() {
		return urlContainsExtractionFilter;
	}

	public void setUrlContainsExtractionFilter(final List<String> urlContainsExtractionFilter) {
		this.urlContainsExtractionFilter = urlContainsExtractionFilter;
	}

	public List<String> getUrlExclusionsExtractionFilter() {
		return urlExclusionsExtractionFilter;
	}

	public void setUrlExclusionsExtractionFilter(final List<String> urlExclusionsExtractionFilter) {
		this.urlExclusionsExtractionFilter = urlExclusionsExtractionFilter;
	}

	public boolean isBrand2csv() {
		return brand2csv;
	}

	public void setBrand2csv(boolean brand2csv) {
		this.brand2csv = brand2csv;
	}

	public String getCustomUrlProviderClass() {
		return customUrlProviderClass;
	}

	public void setCustomUrlProviderClass(String customUrlProviderClass) {
		this.customUrlProviderClass = customUrlProviderClass;
	}






}
