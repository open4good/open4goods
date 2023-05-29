package org.open4goods.model.product;

import java.util.HashSet;
import java.util.Set;

public class ConflictedAttribute {

	/**
	 * The value for this conflict occurence
	 */
	private String value;

	/**
	 * The attributes agreeing to this value
	 */
	private Set<SourcedAttribute> sources = new HashSet<>();



	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public Set<SourcedAttribute> getSources() {
		return sources;
	}

	public void setSources(Set<SourcedAttribute> sources) {
		this.sources = sources;
	}







}
