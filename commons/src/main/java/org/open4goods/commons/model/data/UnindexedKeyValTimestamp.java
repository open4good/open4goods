package org.open4goods.commons.model.data;

// TODO(design, p2) : This is a legacy due to elastic misconception, should be replaced my maps / sets
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
