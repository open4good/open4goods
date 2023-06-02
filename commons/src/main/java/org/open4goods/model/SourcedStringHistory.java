package org.open4goods.model;

import java.util.List;

public class SourcedStringHistory {

	
	private String value;
	private Long timestamp;
	
	
	
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	public Long getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(Long timestamp) {
		this.timestamp = timestamp;
	}
	public SourcedStringHistory(String value, Long timestamp) {
		super();
		this.value = value;
		this.timestamp = timestamp;
	}
		
	
	
	
}
