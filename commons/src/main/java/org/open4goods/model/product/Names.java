package org.open4goods.model.product;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

public class Names {

	@Field(index = true, store = false, type = FieldType.Text)
	private String name;


	@Field(index = true, store = false, type = FieldType.Text)
	private String manualName;


	@Field(index = true, store = false, type = FieldType.Text, analyzer = "french")
	//	@Field(index = true, store = false, type = FieldType.Text)
	private Set<String> offerNames = new HashSet<>();


	public String longestOfferName() {
		return offerNames.stream().max (Comparator.comparingInt(String::length)).get();
	}

	public String shortestOfferName() {
		return offerNames.stream().min (Comparator.comparingInt(String::length)).orElse(null);
	}

	public Set<String> getOfferNames() {
		return offerNames;
	}

	public void setOfferNames(final Set<String> offerNames) {
		this.offerNames = offerNames;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getManualName() {
		return manualName;
	}

	public void setManualName(String manualName) {
		this.manualName = manualName;
	}



}
