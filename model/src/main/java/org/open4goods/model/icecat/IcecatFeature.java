package org.open4goods.model.icecat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class IcecatFeature {
	@JacksonXmlProperty(isAttribute = true, localName = "Class")
	private String Clazz;

	@JacksonXmlProperty(isAttribute = true)
	private String DefaultDisplayUnit;

	@JacksonXmlProperty(isAttribute = true)
	private String ID;

	@JacksonXmlProperty(isAttribute = true)
	private String Type;

	@JacksonXmlProperty(isAttribute = true)
	private String Updated;

	@JacksonXmlProperty(localName = "Descriptions")
	private IcecatDescriptions descriptions;

	@JacksonXmlProperty(localName = "Measure")
	private IcecatMeasure measure;

	@JacksonXmlProperty(localName = "Names")
	private IcecatNames names;
	
	
	
	   @JacksonXmlProperty(isAttribute = true)
       private int CategoryFeatureGroup_ID;

       @JacksonXmlProperty(isAttribute = true)
       private int CategoryFeature_ID;


       @JacksonXmlProperty(isAttribute = true)
       private int LimitDirection;

       @JacksonXmlProperty(isAttribute = true)
       private int Mandatory;

       @JacksonXmlProperty(isAttribute = true)
       private String No;

       @JacksonXmlProperty(isAttribute = true)
       private int Searchable;

   
       @JacksonXmlProperty(isAttribute = true)
       private String Use_Dropdown_Input;

       @JacksonXmlProperty(isAttribute = true)
       private int ValueSorting;

       
       
       

	public String getClazz() {
		return Clazz;
	}

	public void setClazz(String clazz) {
		Clazz = clazz;
	}

	public String getDefaultDisplayUnit() {
		return DefaultDisplayUnit;
	}

	public void setDefaultDisplayUnit(String DefaultDisplayUnit) {
		this.DefaultDisplayUnit = DefaultDisplayUnit;
	}

	public String getID() {
		return ID;
	}

	public void setID(String ID) {
		this.ID = ID;
	}

	public String getType() {
		return Type;
	}

	public void setType(String Type) {
		this.Type = Type;
	}

	public String getUpdated() {
		return Updated;
	}

	public void setUpdated(String Updated) {
		this.Updated = Updated;
	}

	public IcecatDescriptions getDescriptions() {
		return descriptions;
	}

	public void setDescriptions(IcecatDescriptions descriptions) {
		this.descriptions = descriptions;
	}

	public IcecatMeasure getMeasure() {
		return measure;
	}

	public void setMeasure(IcecatMeasure measure) {
		this.measure = measure;
	}

	public IcecatNames getNames() {
		return names;
	}

	public void setNames(IcecatNames names) {
		this.names = names;
	}

	

	public int getCategoryFeatureGroup_ID() {
		return CategoryFeatureGroup_ID;
	}

	public void setCategoryFeatureGroup_ID(int categoryFeatureGroup_ID) {
		CategoryFeatureGroup_ID = categoryFeatureGroup_ID;
	}

	public int getCategoryFeature_ID() {
		return CategoryFeature_ID;
	}

	public void setCategoryFeature_ID(int categoryFeature_ID) {
		CategoryFeature_ID = categoryFeature_ID;
	}

	public int getLimitDirection() {
		return LimitDirection;
	}

	public void setLimitDirection(int limitDirection) {
		LimitDirection = limitDirection;
	}

	public int getMandatory() {
		return Mandatory;
	}

	public void setMandatory(int mandatory) {
		Mandatory = mandatory;
	}


	public String getNo() {
		return No;
	}

	public void setNo(String no) {
		No = no;
	}

	public int getSearchable() {
		return Searchable;
	}

	public void setSearchable(int searchable) {
		Searchable = searchable;
	}

	public String getUse_Dropdown_Input() {
		return Use_Dropdown_Input;
	}

	public void setUse_Dropdown_Input(String use_Dropdown_Input) {
		Use_Dropdown_Input = use_Dropdown_Input;
	}

	public int getValueSorting() {
		return ValueSorting;
	}

	public void setValueSorting(int valueSorting) {
		ValueSorting = valueSorting;
	}
	
	
}