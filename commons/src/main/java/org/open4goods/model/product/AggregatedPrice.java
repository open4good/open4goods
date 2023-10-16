
package org.open4goods.model.product;

import java.text.DecimalFormat;

import org.open4goods.model.constants.Currency;
import org.open4goods.model.constants.ProductState;
import org.open4goods.model.data.DataFragment;
import org.open4goods.model.data.Price;
import org.springframework.data.annotation.Transient;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

public class AggregatedPrice extends Price {
	
	private static final DecimalFormat df = new DecimalFormat("0.00");
	
	@Field(index = false, store = false, type = FieldType.Keyword)
	private String datasourceName;
	@Field(index = false, store = false, type = FieldType.Keyword)
	private String offerName;
	@Field(index = false, store = false, type = FieldType.Keyword)
	private String url;
	@Field(index = false, store = false, type = FieldType.Double)
	private Double compensation;
	/**
	 * The state of the product (new, occasion, ...)
	 */
	@Field(index = true, store = false, type = FieldType.Keyword)
	private ProductState productState;

	/**
	 * The encoded form of the affiliation token
	 */
	@Transient
	@Field(index = false, store = false, type = FieldType.Keyword)
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

		int i = datasourceName.indexOf(".");
		if (i == -1) {
			return datasourceName;
		} else {
			return datasourceName.substring(0,i);
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
	 * A human readable price (2 decimals max, skipped if int)
	 * @return
	 */
	public String shortPrice() {
		
		Double p = super.getPrice();		
		boolean isInt = p == Math.rint(p);
		
		if (isInt) {
			return String.valueOf(p.intValue());
		} else {
			return String.valueOf(df.format(p));
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

	public ProductState getProductState() {
		return productState;
	}

	public void setProductState(ProductState productState) {
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
