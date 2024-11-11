package org.open4goods.commons.model.product;

import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

public class PriceHistory {

	private Long timestamp;

	private Double price;


	
	@Override
	public String toString() {
		return timestamp+":"+price;
	}
	public PriceHistory() {
	}

	
	/**
	 * Get the number of the day this timestamp refers to
	 * @return
	 */
	public Long getDay() {
		return getTimestamp() / (1000 * 60 * 60 * 24); 
	}

	
	public PriceHistory(AggregatedPrice minPrice) {
		timestamp = minPrice.getTimeStamp();
		price = minPrice.getPrice();
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


}
