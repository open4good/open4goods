package org.open4goods.api.model;

import org.open4goods.commons.model.data.Price;

public class SearchResult {
	private String brand;
	private String brandUid;
	private Long dateIndexed;
	private Price price;
	private String url;
	public String getBrand() {
		return brand;
	}
	public void setBrand(final String brand) {
		this.brand = brand;
	}
	public String getBrandUid() {
		return brandUid;
	}
	public void setBrandUid(final String brandUid) {
		this.brandUid = brandUid;
	}

	public Long getDateIndexed() {
		return dateIndexed;
	}
	public void setDateIndexed(final Long dateIndexed) {
		this.dateIndexed = dateIndexed;
	}
	public Price getPrice() {
		return price;
	}
	public void setPrice(final Price price) {
		this.price = price;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(final String url) {
		this.url = url;
	}



}
