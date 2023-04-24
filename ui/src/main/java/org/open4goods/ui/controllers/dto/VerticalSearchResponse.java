package org.open4goods.ui.controllers.dto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.open4goods.config.yml.ui.VerticalConfig;
import org.open4goods.model.product.AggregatedData;

/**
 * Results of a search inside a specific vertical
 * @author goulven
 *
 */
public class VerticalSearchResponse {
	
	private String verticalName;

	private VerticalConfig verticalConfig;
	
	private List<AggregatedData> data;

	private Map<String, Long> brands = new HashMap<>();
	
	private Integer maxOffers;

	private Integer minOffers;
	
	private Double minPrice = null;
	
	private Double maxPrice = null;
	
	private Long totalResults;
	
	private Integer from;
	
	private Integer to;

	private long itemNew;
	
	private long itemOccasion;

	private long itemUnknown;
	
	
	
	
	
	
	
	public String getVerticalName() {
		return verticalName;
	}

	public void setVerticalName(String verticalName) {
		this.verticalName = verticalName;
	}

	public List<AggregatedData> getData() {
		return data;
	}

	public void setData(List<AggregatedData> results) {
		this.data = results;
	}







	public Double getMinPrice() {
		return minPrice;
	}




	public void setMinPrice(Double min) {
		this.minPrice = min;
	}




	public Double getMaxPrice() {
		return maxPrice;
	}




	public void setMaxPrice(Double max) {
		this.maxPrice = max;
	}

	public Long getTotalResults() {
		return totalResults;
	}

	public void setTotalResults(Long totalResults) {
		this.totalResults = totalResults;
	}

	public Integer getFrom() {
		return from;
	}

	public void setFrom(Integer from) {
		this.from = from;
	}

	public Integer getTo() {
		return to;
	}

	public void setTo(Integer to) {
		this.to = to;
	}

	public long getItemNew() {
		return itemNew;
	}

	public void setItemNew(long itemNew) {
		this.itemNew = itemNew;
	}

	public long getItemOccasion() {
		return itemOccasion;
	}

	public void setItemOccasion(long itemOccasion) {
		this.itemOccasion = itemOccasion;
	}

	public long getItemUnknown() {
		return itemUnknown;
	}

	public void setItemUnknown(long itemUnknown) {
		this.itemUnknown = itemUnknown;
	}




	public Map<String, Long> getBrands() {
		return brands;
	}

	public void setBrands(Map<String, Long> brands) {
		this.brands = brands;
	}

	public Integer getMaxOffers() {
		return maxOffers;
	}

	public void setMaxOffers(Integer maxOffers) {
		this.maxOffers = maxOffers;
	}

	public Integer getMinOffers() {
		return minOffers;
	}

	public void setMinOffers(Integer minOffers) {
		this.minOffers = minOffers;
	}

	public VerticalConfig getVerticalConfig() {
		return verticalConfig;
	}

	public void setVerticalConfig(VerticalConfig verticalConfig) {
		this.verticalConfig = verticalConfig;
	}
	
	
	
	
	
}
