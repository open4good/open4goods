package org.open4goods.model.product;

import org.open4goods.model.constants.Currency;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

public class PriceHistory {
	
	@Field(index = false, store = false, type=FieldType.Date, format = DateFormat.epoch_millis)
	private Long timestamp;
	
	@Field(index = false, store = false, type = FieldType.Double)	
	private Double price;

	@Field(index = false, store = false, type = FieldType.Keyword)	
	private Currency currency;

	public PriceHistory() {
	}

	public PriceHistory(AggregatedPrice minPrice) {
		this.timestamp = minPrice.getTimeStamp();
		this.price = minPrice.getPrice();
		this.currency = minPrice.getCurrency();
	}

	public Long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Long timestamp) {
		this.timestamp = timestamp;
	}

	public Double getPrice() {
		return price;
	}

	public void setPrice(Double price) {
		this.price = price;
	}

	public Currency getCurrency() {
		return currency;
	}

	public void setCurrency(Currency currency) {
		this.currency = currency;
	}
	
	
	
}
