package org.open4goods.model.product;

import org.open4goods.model.attribute.Attribute;
import org.open4goods.model.data.DataFragment;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

public class SourcedAttribute extends Attribute{
	
	@Field(index = false, store = false, type = FieldType.Keyword)	
	private String datasourceName;
	
	public SourcedAttribute() {
		super();
	}

	/**
	 * Constructor
	 * @param translated
	 * @param df
	 */
	public SourcedAttribute(IAttribute source, DataFragment df) {

		this.datasourceName = df.getDatasourceName();
		
		setRawValue(source.getValue());
		setLanguage(source.getLanguage());
		setName(source.getName());

	}
	
	
	public SourcedAttribute(IAttribute source, AggregatedData df) {
		
		setRawValue(source.getValue());
		setLanguage(source.getLanguage());
		setName(source.getName());
//		setReferentiel(source.getReferentiel());

	}
	
	
	////////////////////////////////
	// Getters / setters
	////////////////////////////////
	


	public String getDatasourceName() {
		return datasourceName;
	}

	public void setDatasourceName(String datasourceName) {
		this.datasourceName = datasourceName;
	}


	

}
