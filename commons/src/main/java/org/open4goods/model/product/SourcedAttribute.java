package org.open4goods.model.product;

import org.open4goods.model.attribute.Attribute;
import org.open4goods.model.data.DataFragment;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

public class SourcedAttribute extends Attribute{
	
	@Field(index = false, store = false, type = FieldType.Keyword)	
	private String datasourceName;
	
	@Field(index = false, store = false, type = FieldType.Keyword)	
	private String url;
	
	@Field(index = false, store = false, type = FieldType.Date, format = DateFormat.epoch_millis)	
	private Long date;


	public SourcedAttribute() {
		super();
	}

	/**
	 * Constructor
	 * @param translated
	 * @param df
	 */
	public SourcedAttribute(Attribute source, DataFragment df) {

		this.date = df.getLastIndexationDate();
		this.datasourceName = df.getDatasourceName();
		this.url=df.affiliatedUrlIfPossible();
		
		setRawValue(source.getRawValue());
		setLanguage(source.getLanguage());
		setName(source.getName());
		setReferentiel(source.getReferentiel());
		
		
		
	}
	
	////////////////////////////////
	// Getters / setters
	////////////////////////////////
	


	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getDatasourceName() {
		return datasourceName;
	}

	public void setDatasourceName(String datasourceName) {
		this.datasourceName = datasourceName;
	}

	public Long getDate() {
		return date;
	}

	public void setDate(Long date) {
		this.date = date;
	}


	

}
