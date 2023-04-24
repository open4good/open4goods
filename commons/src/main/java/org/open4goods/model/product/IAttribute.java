package org.open4goods.model.product;

import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

public interface IAttribute {

	public String getLanguage();
	
	
	public String getValue() ;
	
	public String getName() ;
	
	public void setName(String name) ;
}
