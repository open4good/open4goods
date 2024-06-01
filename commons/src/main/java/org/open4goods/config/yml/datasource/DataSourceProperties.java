
package org.open4goods.config.yml.datasource;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.open4goods.model.constants.Currency;
import org.open4goods.model.constants.ProductCondition;
import org.open4goods.model.constants.ReferentielKey;
import org.open4goods.model.constants.TimeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;

import ch.qos.logback.classic.Level;
import jakarta.validation.constraints.NotBlank;

/**
 * Config for a provider website
 *
 * @author goulven
 *
 */
/**
 * @author goulven
 *
 */
@Validated
public class DataSourceProperties {

	/**
	 * TODO(gof) :; should be from a global conf The mapping of attributes that must
	 * be indexed as referentiel attributes
	 */
	private static Map<String, ReferentielKey> defaultReferentielAttributes = new HashMap<>();

	static {
		// Load the default referentiel attributes

		defaultReferentielAttributes.put("MODEL", ReferentielKey.MODEL);
		defaultReferentielAttributes.put("MODELE", ReferentielKey.MODEL);
		defaultReferentielAttributes.put("MODÈLE", ReferentielKey.MODEL);
		defaultReferentielAttributes.put("MODEL", ReferentielKey.MODEL);
		defaultReferentielAttributes.put("BRAND", ReferentielKey.BRAND);
		defaultReferentielAttributes.put("MARQUE", ReferentielKey.BRAND);
		defaultReferentielAttributes.put("GTIN", ReferentielKey.GTIN);
		defaultReferentielAttributes.put("GTIN13", ReferentielKey.GTIN);
		defaultReferentielAttributes.put("EAN", ReferentielKey.GTIN);
		defaultReferentielAttributes.put("EAN13", ReferentielKey.GTIN);
		defaultReferentielAttributes.put("GENCOD EAN", ReferentielKey.GTIN);
	}

	protected static final Logger logger = LoggerFactory.getLogger(DataSourceProperties.class);

	@NotBlank
	private String name;

	/**
	 * The eventual feedKey, if a match from catalog (feedservice)
	 */
	private String feedKey;
	
	/**
	 * Hot time populated
	 */
	private String datasourceConfigName;
	
	@NotBlank
	private String favico;

	private String logo;


	private String portalUrl;


	/**
	 * If true, will be computed as a brand score
	 */
	private boolean brandScore = false;

	/**
	 * If set, the score will be inverted (invertedScaleBase - score).
	 * (used to "reverse" a score. eg. a score presenting a risk, we want to get the associated performance)
	 */
	private Double invertScaleBase;
	
	
	
	/**
	 * The percent of reversement for the provider
	 */
	private Double reversement = 3.0;

	/**
	 * Scheduling of this datasource Can be TimeConstants.CRON_DAY
	 * |TimeConstants.CRON_WEEK | TimeConstants.CRON_MONTH or a spring cronexp
	 *
	 *
	 */

	//	 "*/10 * * * * *" = every ten seconds.
	//	 "0 0 8-10 * * *" = 8, 9 and 10 o'clock of every day.
	//	 "0 0 8,10 * * *" = 8 and 10 o'clock of every day.
	//	* "0 0/30 8-10 * * *" = 8:00, 8:30, 9:00, 9:30 and 10 o'clock every day.
	//	* "0 0 9-17 * * MON-FRI" = on the hour nine-to-five weekdays
	//	* "0 0 0 25 12 ?" = every Christmas Day at midnight
	//     *
	private String cronRefresh;

	// A local instance variable, that cache the computed cron
	private String internalCron;

	//	/**
	//	 * If true, the "debugging" will be outputed as WARN in the dedicatedLogger.
	//	 * Only apply if activeProfile = dev
	//	 */
	//	private Boolean devMode = false;

	/**
	 * If true, a best effort attempt to extract brandUid from offersNames will be
	 * processed.
	 */
	private Boolean extractBrandUidFromName = false;

	@NotBlank
	/**
	 * The datasource language. It is a default value, it is in most case
	 * autodetected
	 */
	private String language = "FR";

	/**
	 * The default currency to use. Default value used if currency is not set from
	 * extraction process
	 */
	private Currency defaultCurrency;

	/**
	 * If set, the validation will occurs on those fields (and not the classical
	 * name/price/currency) Used for example by referentiels items
	 */
	private Set<String> validationFields;

	/**
	 * If true, this is a referentiel item
	 */
	private Boolean referentiel = false;

	/**
	 * The log level for specific logging file. If empty, no specific logging
	 */
	private String dedicatedLogLevel = "WARN";

	/**
	 * In case where MaxRating is not defined, allow to define the site specific
	 * rating max
	 */
	private Double ratingMax;

	/**
	 * In case where MaxRating is not defined, allow to define the site specific
	 * rating min
	 */

	private Integer ratingMin;

	/**
	 * The default condition of the item
	 */
	private ProductCondition defaultItemCondition = ProductCondition.NEW;

	/**
	 * The date format
	 * (https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html),
	 * used by this provider
	 */
	private String dateFormat;

	/**
	 * Those prefixes will be removed from dates before parsing
	 */
	private Set<String> datesPrefixesToRemove = new HashSet<>();

	/**
	 * If set all chars after this string in dates will be trimed
	 */
	private String datesCutAt;

	/**
	 * If set, the affiliation link will be the DataFragmentUrl appened to this
	 * affiliationLinkPrefix. The DataFragmentURl will then be urlencoded.
	 */
	private String affiliationLinkPrefix;

	/**
	 * If set and if a affiliationLinkPrefix is also set, the affiliation link will
	 * be the DataFragmentUrl suffixed with this affiliationLinkSuffix.
	 */
	private String affiliationLinkSuffix;

	/**
	 * For regression check, indicates the minimum number of items that should have
	 * been indexed
	 */

	private Long minimumIndexedItems = Long.MAX_VALUE;

	/**
	 * If set, this tokens will be removed from descriptions
	 */
	private Set<String> descriptionRemoveToken;

	/**
	 * If set, means this is a CSV datasource.
	 */
	private CsvDataSourceProperties csvDatasource;

	private HtmlDataSourceProperties webDatasource;

	private ApiDataSourceProperties apiDatasource;

	/**
	 *
	 * @return the webdatasource, from csv configuration or from webdatasource
	 */
	public HtmlDataSourceProperties webDataSource() {
		if (null != csvDatasource && null != csvDatasource.getWebDatasource()) {
			return csvDatasource.getWebDatasource();
		}

		return webDatasource;
	}

	/**
	 * Return a well formatted cron expression from cron OR from a DAILY / WEEKLY /
	 * MONTHLY
	 *
	 * @return
	 */
	public String cron() {

		if (internalCron != null) {
			return internalCron;
		}

		String c = null;
		if (org.apache.commons.lang3.StringUtils.isEmpty(cronRefresh)) {

			if (null != webDataSource()) {
				// If it is a web datasource (csv handled or natural, then schedule weekly
				logger.info("Undefined cronReferesh for webdatasource {}. Defaulting to every week", name);
				cronRefresh = TimeConstants.CRON_WEEK;

			} else if (null != apiDatasource) {
				// If it is a web datasource (csv handled or natural, then schedule weekly
				logger.info("Undefined cronReferesh for apidatasource {}. Defaulting to every day", name);
				cronRefresh = TimeConstants.CRON_DAY;

			} else {
				logger.info("Undefined cronReferesh for csvdatasource {}. Defaulting to every day", name);
				cronRefresh = TimeConstants.CRON_DAY;

			}

		}

        c = switch (cronRefresh.toUpperCase()) {
            case TimeConstants.CRON_DAY -> "0 " + getRandomMinute() + " " + getRandomHour() + " ? * *";
            case TimeConstants.CRON_WEEK ->
                    "0 " + getRandomMinute() + " " + getRandomHour() + " ? * " + getRandomDayOfWeek();
            case TimeConstants.CRON_MONTH ->
                    "0 " + getRandomMinute() + " " + getRandomHour() + " " + getRandomDayOfMonth() + " * ?";
            default -> cronRefresh;
        };

		internalCron = c;
		return internalCron;
	}
	
	

	
	///////////////////////////////
	// Random generation
	//////////////////////////////
	private static int getRandomNumberInRange(final int min, final int max) {

		final Random r = new Random();
		return r.ints(min, max + 1).limit(1).findFirst().getAsInt();

	}

	private int getRandomMinute() {
		return getRandomNumberInRange(0, 59);
	}

	private int getRandomHour() {
		return getRandomNumberInRange(0, 23);
	}

	private int getRandomDayOfMonth() {
		return getRandomNumberInRange(1, 29);
	}

	private int getRandomDayOfWeek() {
		return getRandomNumberInRange(1, 7);
	}

	@Override
	public String toString() {
		return name;
	}

	public Level getLogLevel() {
		return Level.toLevel(dedicatedLogLevel);
	}

	public String getName() {
		return name;
	}
	//
	//	public ProviderType getType() {
	//		return type;
	//	}
	//
	//	public void setType(final ProviderType type) {
	//		this.type = type;
	//	}

	public String getFavico() {
		return favico;
	}

	public void setFavico(final String favico) {
		this.favico = favico;
	}

	public String getCronRefresh() {
		return cronRefresh;
	}

	public void setCronRefresh(final String cronRefresh) {
		this.cronRefresh = cronRefresh;
	}

	//	public Boolean getDevMode() {
	//		return devMode;
	//	}
	//
	//	public void setDevMode(Boolean devMode) {
	//		this.devMode = devMode;
	//	}

	public Boolean getExtractBrandUidFromName() {
		return extractBrandUidFromName;
	}

	public void setExtractBrandUidFromName(final Boolean extractBrandUidFromName) {
		this.extractBrandUidFromName = extractBrandUidFromName;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(final String language) {
		this.language = language;
	}

	public Currency getDefaultCurrency() {
		return defaultCurrency;
	}

	public void setDefaultCurrency(final Currency defaultCurrency) {
		this.defaultCurrency = defaultCurrency;
	}

	public Set<String> getValidationFields() {
		return validationFields;
	}

	public void setValidationFields(final Set<String> validationFields) {
		this.validationFields = validationFields;
	}

	public Boolean getReferentiel() {
		return referentiel;
	}

	public void setReferentiel(final Boolean referentiel) {
		this.referentiel = referentiel;
	}

	public String getDedicatedLogLevel() {
		return dedicatedLogLevel;
	}

	public void setDedicatedLogLevel(final String dedicatedLogLevel) {
		this.dedicatedLogLevel = dedicatedLogLevel;
	}

	public Double getRatingMax() {
		return ratingMax;
	}

	public void setRatingMax(final Double ratingMax) {
		this.ratingMax = ratingMax;
	}

	public Integer getRatingMin() {
		return ratingMin;
	}

	public void setRatingMin(final Integer ratingMin) {
		this.ratingMin = ratingMin;
	}

	public String getDateFormat() {
		return dateFormat;
	}

	public void setDateFormat(final String dateFormat) {
		this.dateFormat = dateFormat;
	}

	public Set<String> getDatesPrefixesToRemove() {
		return datesPrefixesToRemove;
	}

	public void setDatesPrefixesToRemove(final Set<String> datePrefixesToRemove) {
		datesPrefixesToRemove = datePrefixesToRemove;
	}

	public CsvDataSourceProperties getCsvDatasource() {
		return csvDatasource;
	}

	public void setCsvDatasource(final CsvDataSourceProperties csvDatasource) {
		this.csvDatasource = csvDatasource;
	}

	public HtmlDataSourceProperties getWebDatasource() {
		return webDatasource;
	}

	public void setWebDatasource(final HtmlDataSourceProperties webDatasourceConfig) {
		webDatasource = webDatasourceConfig;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public static Map<String, ReferentielKey> getDefaultReferentielAttributes() {
		return defaultReferentielAttributes;
	}

	public String getDatesCutAt() {
		return datesCutAt;
	}

	public void setDatesCutAt(final String datesCutAfter) {
		datesCutAt = datesCutAfter;
	}

	public String getAffiliationLinkPrefix() {
		return affiliationLinkPrefix;
	}

	public void setAffiliationLinkPrefix(final String affiliationLinkPrefix) {
		this.affiliationLinkPrefix = affiliationLinkPrefix;
	}

	public Long getMinimumIndexedItems() {
		return minimumIndexedItems;
	}

	public void setMinimumIndexedItems(final Long minimumIndexedItems) {
		this.minimumIndexedItems = minimumIndexedItems;
	}

	public ProductCondition getDefaultItemCondition() {
		return defaultItemCondition;
	}

	public void setDefaultItemCondition(final ProductCondition defaultProductState) {
		defaultItemCondition = defaultProductState;
	}

	public String getAffiliationLinkSuffix() {
		return affiliationLinkSuffix;
	}

	public void setAffiliationLinkSuffix(final String affiliationLinkSuffix) {
		this.affiliationLinkSuffix = affiliationLinkSuffix;
	}

	public String getInternalCron() {
		return internalCron;
	}

	public void setInternalCron(final String internalCron) {
		this.internalCron = internalCron;
	}

	public ApiDataSourceProperties getApiDatasource() {
		return apiDatasource;
	}

	public void setApiDatasource(final ApiDataSourceProperties apiDatasource) {
		this.apiDatasource = apiDatasource;
	}

	public static void setDefaultReferentielAttributes(final Map<String, ReferentielKey> defaultReferentielAttributes) {
		DataSourceProperties.defaultReferentielAttributes = defaultReferentielAttributes;
	}

	public Double getReversement() {
		return reversement;
	}

	public void setReversement(Double reversement) {
		this.reversement = reversement;
	}

	public String getLogo() {
		return logo;
	}

	public void setLogo(String logo) {
		this.logo = logo;
	}

	public String getPortalUrl() {
		return portalUrl;
	}

	public void setPortalUrl(String portalUrl) {
		this.portalUrl = portalUrl;
	}

	public Set<String> getDescriptionRemoveToken() {
		return descriptionRemoveToken;
	}

	public void setDescriptionRemoveToken(Set<String> descriptionRemoveToken) {
		this.descriptionRemoveToken = descriptionRemoveToken;
	}

	public String getFeedKey() {
		return feedKey;
	}

	public void setFeedKey(String feedKey) {
		this.feedKey = feedKey;
	}

	public String getDatasourceConfigName() {
		return datasourceConfigName;
	}

	public void setDatasourceConfigName(String datasourceConfigName) {
		this.datasourceConfigName = datasourceConfigName;
	}

	public boolean isBrandScore() {
		return brandScore;
	}

	public void setBrandScore(boolean brandScore) {
		this.brandScore = brandScore;
	}

	public Double getInvertScaleBase() {
		return invertScaleBase;
	}

	public void setInvertScaleBase(Double invertScaleBase) {
		this.invertScaleBase = invertScaleBase;
	}





}