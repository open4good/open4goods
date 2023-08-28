package org.open4goods.model.data;

import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

public class UnindexedKeyVal {

	
	@Field(index = false, store = false, type = FieldType.Keyword)
	private String key;
	@Field(index = false, store = false, type = FieldType.Keyword)
	private String value;
	
	
	
	
	public UnindexedKeyVal() {
		super();
	}

	public UnindexedKeyVal(String key, String val) {
		this.key = key;
		this.value = val;
	}

	public String getKey() {
		return key;
	}
	
	@Override
	public int hashCode() {
		return key.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
	
		if (obj instanceof UnindexedKeyVal) {
			return ((UnindexedKeyVal)obj).key.equals(key);
		}
		return false;
		
		
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
	
	
	
}
