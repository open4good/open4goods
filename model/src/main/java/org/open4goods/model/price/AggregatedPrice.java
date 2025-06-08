
package org.open4goods.model.price;

import java.text.DecimalFormat;
import java.util.Locale;

import org.joda.time.DurationFieldType;
import org.joda.time.Period;
import org.joda.time.PeriodType;
import org.joda.time.format.PeriodFormat;
import org.joda.time.format.PeriodFormatter;
import org.open4goods.model.datafragment.DataFragment;
import org.open4goods.model.product.ProductCondition;
import org.springframework.data.annotation.Transient;

public class AggregatedPrice extends Price {

	//TODO : shared, ugly
	public static final DecimalFormat numberFormater = new DecimalFormat("0.#");

	private String datasourceName;

	private String offerName;
	private String url;
	private Double compensation;
	/**
	 * The state of the product (new, occasion, ...)
	 */
	//TODO : Rename to productCondition
	private ProductCondition productState;

	/**
	 * The encoded form of the affiliation token
	 */
	@Transient
	private String affiliationToken;

	/**
	 *
	 * @param price Price to initialize from
	 * @param df    DataFragment to initialize from
	 */
	public AggregatedPrice(DataFragment df) {
		datasourceName = df.getDatasourceName();
		url = df.affiliatedUrlIfPossible();
		offerName = df.longestName();
		setCurrency(df.getPrice().getCurrency());
		setPrice(df.getPrice().getPrice());
		setTimeStamp(df.getLastIndexationDate());
		setProductState(df.getProductState());
	}

	/**
	 *
	 * @return the datasource name without tld
	 */
	public String shortDataSourceName() {

		if (null == datasourceName) {
			return "ERROR_NOT_SET";
		} else {
			int i = datasourceName.indexOf(".");
			if (i == -1) {
				return datasourceName;
			} else {
				return datasourceName.substring(0,i);
			}
		}

	}

	/**
	 * Only used for the "average" price representation
	 * @param price
	 * @param dcurrency
	 */
	public AggregatedPrice(double price, Currency currency) {
		setCurrency(currency);
		setPrice(price);

	}

	/**
	 * TODO : merge with the one on price()
	 * @return a localised formated duration of when the product was last indexed
	 */
	public String ago(Locale locale) {

		long duration = System.currentTimeMillis() - getTimeStamp();



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

	public String formatedDuration() {
		// TODO : LOCALIZE
		return ago(Locale.FRANCE);
	}


	/**
	 * A human readable price (2 decimals max, skipped if int)
	 * @return
	 */
	public String shortPrice() {

		Double p = super.getPrice();
		boolean isInt = p == Math.rint(p);

		if (isInt) {
			return String.valueOf(p.intValue());
		} else {
			return numberFormater.format(p);
		}

	}


	public AggregatedPrice() {
		super();
	}

	public String getDatasourceName() {
		return datasourceName;
	}

	public void setDatasourceName(String datasourceName) {
		this.datasourceName = datasourceName;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getOfferName() {
		return offerName;
	}

	public void setOfferName(String offerName) {
		this.offerName = offerName;
	}

	public ProductCondition getProductState() {
		return productState;
	}

	public void setProductState(ProductCondition productState) {
		this.productState = productState;
	}

	public String getAffiliationToken() {
		return affiliationToken;
	}

	public void setAffiliationToken(String affiliationToken) {
		this.affiliationToken = affiliationToken;
	}

	public Double getCompensation() {
		return compensation;
	}

	public void setCompensation(Double compensation) {
		this.compensation = compensation;
	}




}
