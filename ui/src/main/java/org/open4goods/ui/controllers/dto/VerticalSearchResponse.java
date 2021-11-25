package org.open4goods.ui.controllers.dto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.open4goods.model.product.AggregatedData;

/**
 * Results of a search inside a specific vertical
 * @author goulven
 *
 */
public class VerticalSearchResponse {
	
	private String verticalName;
	
	private List<AggregatedData> data;

	private Map<String, Integer> brands = new HashMap<>();
	
	private Double minPrice = null;
	
	private Double maxPrice = null;
	
	/**
	 * Compute the brands, minPrice / maxPrice
	 */
	public void compute() {
		
		data.forEach(e -> {
			
			// Brands
			Integer val = brands.get(e.brand());
			if (null == val) {
				brands.put(e.brand(), 1);
			} else {
				brands.put(e.brand(), val+1);
			}
			
			// minPrice
			if (null == minPrice) {
				minPrice = e.bestPrice().getPrice();
			} else {
				if (e.bestPrice().getPrice() < minPrice.doubleValue()) {
					minPrice = e.bestPrice().getPrice();					
				}
			}
			
			// maxPrice
			if (null == maxPrice) {
				maxPrice = e.bestPrice().getPrice();
			} else {
				if (e.bestPrice().getPrice() > maxPrice.doubleValue()) {
					maxPrice = e.bestPrice().getPrice();					
				}
			}			
		});
		
		
	}
	
	
	
	
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




	public Map<String, Integer> getBrands() {
		return brands;
	}




	public void setBrands(Map<String, Integer> brands) {
		this.brands = brands;
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
	
}
