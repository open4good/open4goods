package org.open4goods.commons.model.data;

public class UnindexedKeyValTimestamp extends UnindexedKeyVal{

	
	private long ts;
	
	
	public UnindexedKeyValTimestamp() {
		super();
	}

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
