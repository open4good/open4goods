package org.open4goods.commons.model.dto;

import java.util.ArrayList;
import java.util.List;

public class NumericRangeFilter {

	private String key;
	private Double intervalSize = 50.0;
	private Double minValue;
	private Double maxValue;
	private boolean allowEmptyValues = true;
	private List<PriceBucket> priceBuckets = new ArrayList<PriceBucket>();



	public NumericRangeFilter() {
		super();
	}
	public NumericRangeFilter(String attribute, Double minValue, Double maxValue, Double interval, Boolean includeUndefined) {
		super();
		this.key = attribute;
		this.minValue = minValue;
		this.maxValue = maxValue;
		this.allowEmptyValues = includeUndefined.booleanValue();
		this.intervalSize = interval;
	}
	
	@Override
	public String toString() {
	
		return key+ " : " + minValue +"<>" + maxValue+", allowEmptyValues:"+allowEmptyValues;
	}
	
	@Override
	public int hashCode() {
		return toString().hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof NumericRangeFilter) {
			return this.toString().equals(obj.toString());
		}
		return false;
	}
	
	
	public String getKey() {
		return key;
	}
	public void setKey(String attribute) {
		this.key = attribute;
	}
	public Double getMinValue() {
		return minValue;
	}
	public void setMinValue(Double minValue) {
		this.minValue = minValue;
	}
	public Double getMaxValue() {
		return maxValue;
	}
	public void setMaxValue(Double maxValue) {
		this.maxValue = maxValue;
	}
	public boolean isAllowEmptyValues() {
		return allowEmptyValues;
	}
	public void setAllowEmptyValues(boolean allowEmptyValues) {
		this.allowEmptyValues = allowEmptyValues;
	}
	public List<PriceBucket> getPriceBuckets() {
		return priceBuckets;
	}
	public void setPriceBuckets(List<PriceBucket> priceBuckets) {
		this.priceBuckets = priceBuckets;
	}
	public Double getIntervalSize() {
		return intervalSize;
	}
	public void setIntervalSize(Double intervalSize) {
		this.intervalSize = intervalSize;
	}

	


}
