package org.open4goods.commons.model.dto;

public class NumericRangeFilter {

	private String key;
	private Double minValue;
	private Double maxValue;
	private boolean allowEmptyValues = true;



	public NumericRangeFilter() {
		super();
	}
	public NumericRangeFilter(String attribute, Double minValue, Double maxValue, Boolean includeUndefined) {
		super();
		this.key = attribute;
		this.minValue = minValue;
		this.maxValue = maxValue;
		this.allowEmptyValues = includeUndefined.booleanValue();
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

	


}
