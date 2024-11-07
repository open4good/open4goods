package org.open4goods.commons.model.dto;

public class PriceBucket {

	private String name;
	private Long count;

	public PriceBucket() {
	}
	
	public PriceBucket(String keyAsString, long docCount) {
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
