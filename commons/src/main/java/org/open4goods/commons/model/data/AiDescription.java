package org.open4goods.commons.model.data;


import org.springframework.data.elasticsearch.annotations.Field;
/**
 * Representation of a generated AI Text
 */
import org.springframework.data.elasticsearch.annotations.FieldType;

public class AiDescription {
	
	@Field(index = false, store = false, type = FieldType.Date)
	private long ts;
	
	@Field(index = false, store = false, type = FieldType.Date)
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
