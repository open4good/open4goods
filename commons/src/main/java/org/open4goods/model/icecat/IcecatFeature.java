package org.open4goods.model.icecat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class IcecatFeature {
	@JacksonXmlProperty(isAttribute = true)
	private String Class;

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
	
	public String getClassAttribute() { // `getClass` is reserved in Java, so use another name
		return Class;
	}

	public void setClassAttribute(String Class) {
		this.Class = Class;
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
	
	
}