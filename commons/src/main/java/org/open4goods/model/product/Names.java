package org.open4goods.model.product;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.open4goods.model.Localisable;
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


	// language, key, value
	@Field(index = true, store = false, type = FieldType.Object)
	private Map<String,Localisable> names = new HashMap<>();
	
	
	
	/**
	 * 	Adds a keyed name for a language
	 * @param lang
	 * @param key
	 * @param value
	 */
	public void addName(String lang, String key, String value) {
	
		if (!names.containsKey(lang)) {
			names.put(lang, new  Localisable());
		}
		
		names.get(lang).put(key, value);
	}
	
	
	

	
	
	
	
	
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



	public Map<String, Localisable> getNames() {
		return names;
	}

	public void setNames(Map<String, Localisable> names) {
		this.names = names;
	}



}
