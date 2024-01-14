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

	private Localisable url = new Localisable();
	
	private Localisable h1Title = new Localisable();
	
	private Localisable metaTitle = new Localisable();
	
	private Localisable metaDescription = new Localisable();
	
	private Localisable opengraphTitle = new Localisable();
	
	private Localisable openGraphDescription = new Localisable();
	
	private Localisable twitterTitle = new Localisable();
	
	private Localisable twitterDescription = new Localisable();
	
	@Field(index = false, store = false, type = FieldType.Object)
	private Map<String,Localisable> others = new HashMap<>();
//	

	
	@Field(index = true, store = false, type = FieldType.Text, analyzer = "french")
	//	@Field(index = true, store = false, type = FieldType.Text)
	private Set<String> offerNames = new HashSet<>();


	
	
	
	/**
	 * 	Adds a keyed name for a language
	 * @param lang
	 * @param key
	 * @param value
	 */
	public void addOther(String lang, String key, String value) {
	
		if (!others.containsKey(lang)) {
			others.put(lang, new  Localisable());
		}
		
		others.get(lang).put(key, value);
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

	
	public Localisable getUrl() {
		return url;
	}

	public void setUrl(Localisable url) {
		this.url = url;
	}

	public Localisable getH1Title() {
		return h1Title;
	}

	public void setH1Title(Localisable h1Title) {
		this.h1Title = h1Title;
	}

	public Localisable getMetaTitle() {
		return metaTitle;
	}

	public void setMetaTitle(Localisable metaTitle) {
		this.metaTitle = metaTitle;
	}

	public Localisable getMetaDescription() {
		return metaDescription;
	}

	public void setMetaDescription(Localisable metaDescription) {
		this.metaDescription = metaDescription;
	}

	public Localisable getOpengraphTitle() {
		return opengraphTitle;
	}

	public void setOpengraphTitle(Localisable opengraphTitle) {
		this.opengraphTitle = opengraphTitle;
	}

	public Localisable getOpenGraphDescription() {
		return openGraphDescription;
	}

	public void setOpenGraphDescription(Localisable openGraphDescription) {
		this.openGraphDescription = openGraphDescription;
	}

	public Localisable getTwitterTitle() {
		return twitterTitle;
	}

	public void setTwitterTitle(Localisable twitterTitle) {
		this.twitterTitle = twitterTitle;
	}

	public Localisable getTwitterDescription() {
		return twitterDescription;
	}
	public void setTwitterDescription(Localisable twitterDescription) {
		this.twitterDescription = twitterDescription;
	}

	public Map<String, Localisable> getOthers() {
		return others;
	}

	public void setOthers(Map<String, Localisable> others) {
		this.others = others;
	}
}
