package org.open4goods.commons.model.data;

public class AiDescription {
	
	private long ts;
	
	private String content;
	
	public AiDescription() {
		super();
	}

	public AiDescription(String content) {
		this.ts = System.currentTimeMillis();
		this.content = content;
	}

	public long getTs() {
		return ts;
	}

	public void setTs(long ts) {
		this.ts = ts;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	


}
