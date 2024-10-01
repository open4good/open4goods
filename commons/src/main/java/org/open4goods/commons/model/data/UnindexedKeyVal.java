package org.open4goods.commons.model.data;

import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
//TODO(design, p2) : This is a legacy due to elastic misconception, should be replaced my maps / sets
public class UnindexedKeyVal {

	
	private String key;
	private String value;
	
	public UnindexedKeyVal() {
		super();
	}

	public UnindexedKeyVal(String key, String val) {
		this.key = key;
		this.value = val;
	}

	
	@Override
	public int hashCode() {
		return key.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
	
		if (obj instanceof UnindexedKeyVal || obj instanceof UnindexedKeyValTimestamp) {
			return ((UnindexedKeyVal)obj).key.equals(key);
		}
		return false;
	}

	
	@Override
	public String toString() {
		return value;
	}
	
	public void setKey(String key) {
		this.key = key;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}

	public String getKey() {
		return key;
	}
	
	
}
