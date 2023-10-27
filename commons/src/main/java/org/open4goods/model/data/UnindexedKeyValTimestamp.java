package org.open4goods.model.data;

import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

public class UnindexedKeyValTimestamp extends UnindexedKeyVal{

	
	@Field(index = false, store = false, type = FieldType.Date)
	private long ts;
	
	public UnindexedKeyValTimestamp(String key, String value) {
		super(key, value);
		this.ts=System.currentTimeMillis();
	}

	public long getTs() {
		return ts;
	}

	public void setTs(long ts) {
		this.ts = ts;
	}

	
	

	
}
