package org.open4goods.commons.model.dto;

public class NumericBucket {

	private String name;
	private Long count;

	public NumericBucket() {
	}
	
	public NumericBucket(String keyAsString, long docCount) {
		this.name = keyAsString;
		this.count = docCount;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Long getCount() {
		return count;
	}
	public void setCount(Long count) {
		this.count = count;
	}
	
	
}
