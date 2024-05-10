package org.open4goods.model.data;

import org.open4goods.model.Localised;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

public class AiDescription extends Description {
	
	@Field(index = false, store = false, type = FieldType.Date)
	private long ts;
	
	public AiDescription() {
		super();

	}

	public AiDescription(Localised content) {
		super(content);	
		this.ts = System.currentTimeMillis();
	}

	public AiDescription(String description, String language) {
		super(description, language);
		this.ts = System.currentTimeMillis();		
	}

	public long getTs() {
		return ts;
	}

	public void setTs(long ts) {
		this.ts = ts;
	}


}
