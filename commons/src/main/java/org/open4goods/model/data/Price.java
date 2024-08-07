package org.open4goods.model.data;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.exceptions.ValidationException;
import org.open4goods.model.Standardisable;
import org.open4goods.model.Validable;
import org.open4goods.model.constants.Currency;
import org.open4goods.services.StandardiserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import com.google.common.collect.Sets;

import jakarta.validation.constraints.NotNull;

public class Price implements Validable, Standardisable, Comparable<Double> {


	// TODO(conf): from conf : price valid 3 monthes in microdata
	public static final long MICRODATA_VALID_UNTIL_DURATION = 1000 * 3600 *24 * 90l;

	private final static Logger logger = LoggerFactory.getLogger(Price.class);

	@NotNull
	@Field(index = true, store = false, type = FieldType.Double)
	private Double price;

	@NotNull
	@Field(index = false, store = false, type = FieldType.Keyword)
	private Currency currency;

	@NotNull
	@Field(index = true, store = false, type = FieldType.Date, format = DateFormat.epoch_millis)
	private Long timeStamp;

	//	private static final SimpleDateFormat iso8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

	public Price() {
		super();
	}

	public Price(@NotNull final Double price, @NotNull final Currency currency) {
		super();
		this.price = price;
		this.currency = currency;
	}

	///////////////////////////////////////
	// toString / HashCode / Equals / ...
	///////////////////////////////////////

	@Override
	public int compareTo(Double o) {
		return price.compareTo(o);
	}

	@Override
	public boolean equals(final Object obj) {

		if (this == obj) {
			return true;
		}

		if (obj instanceof Price p) {
			return Objects.equals(getCurrency(), p.getCurrency()) && Objects.equals(getPrice(), p.getPrice());

		}
		return false;
	}

	@Override
	public String toString() {
		return price + " " + currency;
	}

	/////////////////////////////////////
	// Contract methods
	/////////////////////////////////////
	@Override
	public void validate() throws ValidationException {
		if (null == currency || null == price) {
			throw new ValidationException("Invalid price");
		}
	}

	@Override
	public Set<Standardisable> standardisableChildren() {
		return Sets.newHashSet(this);
	}

	@Override
	public void standardize(final StandardiserService standardiser, final Currency c) {
		standardiser.standarise(this, c);

	}

	///////////////////////////////////
	// Utility methods
	///////////////////////////////////
	public boolean greaterThan(final Price p) {

		if (null == p) {
			logger.error("Cannot apply greaterThan on a null price");
			return false;
		}

		if (!p.getCurrency().equals(getCurrency())) {
			logger.error("greaterThan is not so good on different currencies ! Data coherence will be bad....");
		}

		return price > p.getPrice();

	}

	public boolean lowerThan(final Price p) {
		if (null == p) {
			logger.error("Cannot apply lowerThan on a null price");
			return false;
		}
		if (!p.getCurrency().equals(getCurrency())) {
			logger.error("lowerThan is not so good on different currencies ! Data coherence will be bad....");

		}

		return price < p.getPrice();
	}

	public void setCurrency(String currency) throws ParseException {

		if (StringUtils.isEmpty(currency)) {
			return;
		}
		currency = currency.trim().toUpperCase();

		final Currency cur = Currency.convert(currency);

		this.currency = cur;


	}

	public void setPriceValue(final String price, final Locale locale) throws ParseException {


		double p;
		try {
			p = Double.parseDouble(price);
		} catch (final NumberFormatException e) {

			final NumberFormat nf = NumberFormat.getInstance(locale);

			p = nf.parse(price).doubleValue();

		}

		if (null != this.price && !this.price.equals(p)) {
			logger.info("Price conflict : {} <> {}. Will erase",p,this.price);
		}

		this.price = p;

	}



	/**
	 * Return the decimal part with the ".", or empty text if none
	 *
	 * @return
	 */
	public String priceRoundingPart() {
		String str;
		try {
			str = String.valueOf(price);
			str = str.substring(str.indexOf("."));
			if (".0".equals(str)) {
				str = "";
			}

		} catch (final Exception e) {
			str = "";
		}

		return str;
	}

	/**
	 * Return the currency symbol
	 *
	 * @return
	 */
	public String currencySymbol() {
		return java.util.Currency.getInstance(currency.toString()).getSymbol();
	}

	/**
	 * Compute the valid until (used by UI microdatas)
	 * @return
	 */
	public String validUntil () {

		Date date = new Date(timeStamp + MICRODATA_VALID_UNTIL_DURATION);
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		return new StringBuilder()
				.append(calendar.get(Calendar.YEAR))
				.append("-")
				.append(calendar.get(Calendar.MONTH)+1)
				.append("-").append(calendar.get(Calendar.DAY_OF_MONTH))
				.toString();
	}
	/**
	 *
	 * @return the entire part as an integer
	 */
	public Integer priceInteger() {
		return price.intValue();
	}
	/////////////////////////////////////////
	// Getters / Setters
	/////////////////////////////////////////

	public Double getPrice() {
		return price;
	}

	public void setPrice(final Double price) {
		this.price = price;
	}

	public Currency getCurrency() {
		return currency;
	}

	public void setCurrency(final Currency currency) {
		this.currency = currency;
	}

	public Long getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(Long timeStamp) {
		this.timeStamp = timeStamp;
	}

}
