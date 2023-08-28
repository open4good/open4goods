
package org.open4goods.model.product;

import org.open4goods.model.constants.Currency;
import org.open4goods.model.constants.ProductState;
import org.open4goods.model.data.DataFragment;
import org.open4goods.model.data.Price;
import org.springframework.data.annotation.Transient;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

public class AggregatedPrice extends Price {

	@Field(index = false, store = false, type = FieldType.Keyword)
	private String datasourceName;
	@Field(index = false, store = false, type = FieldType.Keyword)
	private String datasourceConfigName;
	@Field(index = false, store = false, type = FieldType.Keyword)
	private String offerName;
	@Field(index = false, store = false, type = FieldType.Keyword)
	private String url;
	@Field(index = false, store = false, type = FieldType.Boolean)
	private boolean affiliated;
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

		datasourceConfigName = df.getDatasourceConfigName();
		datasourceName = df.getDatasourceName();
		url = df.affiliatedUrlIfPossible();
		affiliated = df.affiliated();
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



	public AggregatedPrice() {
		super();
	}

	public String getDatasourceName() {
		return datasourceName;
	}

	public void setDatasourceName(String datasourceName) {
		this.datasourceName = datasourceName;
	}

	public String getDatasourceConfigName() {
		return datasourceConfigName;
	}

	public void setDatasourceConfigName(String datasourceConfigName) {
		this.datasourceConfigName = datasourceConfigName;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public boolean isAffiliated() {
		return affiliated;
	}

	public void setAffiliated(boolean affiliated) {
		this.affiliated = affiliated;
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
