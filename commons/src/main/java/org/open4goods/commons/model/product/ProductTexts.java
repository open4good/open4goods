package org.open4goods.commons.model.product;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import org.open4goods.commons.model.Localisable;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

public class ProductTexts {

	private Localisable<String,String> url = new Localisable<>();
	
	private Localisable<String,String> h1Title = new Localisable<>();
	
	private Localisable<String,String> metaDescription = new Localisable<>();
	
	private Localisable<String,String> productMetaOpenGraphTitle = new Localisable<>();
	
	private Localisable<String,String> productMetaOpenGraphDescription = new Localisable<>();
	
	private Localisable<String,String> productMetaTwitterTitle = new Localisable<>();
	
	private Localisable<String,String> productMetaTwitterDescription = new Localisable<>();
	
	
	@Field(index = true, store = false, type = FieldType.Text, analyzer = "french")
	//	@Field(index = true, store = false, type = FieldType.Text)
	private Set<String> offerNames = new HashSet<>();


	
//	
//	
//	/**
//	 * 	Adds a keyed name for a language
//	 * @param lang
//	 * @param key
//	 * @param value
//	 */
//	public void addOther(String lang, String key, String value) {
//	
//		if (!others.containsKey(lang)) {
//			others.put(lang, new  Localisable());
//		}
//		
//		others.get(lang).put(key, value);
//	}
//	
//	
//	

	
	
	
	
	
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

	public Localisable<String, String> getUrl() {
		return url;
	}

	public void setUrl(Localisable<String, String> url) {
		this.url = url;
	}

	public Localisable<String, String> getH1Title() {
		return h1Title;
	}

	public void setH1Title(Localisable<String, String> h1Title) {
		this.h1Title = h1Title;
	}

	public Localisable<String, String> getMetaDescription() {
		return metaDescription;
	}

	public void setMetaDescription(Localisable<String, String> metaDescription) {
		this.metaDescription = metaDescription;
	}

	public Localisable<String, String> getProductMetaOpenGraphTitle() {
		return productMetaOpenGraphTitle;
	}

	public void setProductMetaOpenGraphTitle(Localisable<String, String> productMetaOpenGraphTitle) {
		this.productMetaOpenGraphTitle = productMetaOpenGraphTitle;
	}

	public Localisable<String, String> getProductMetaOpenGraphDescription() {
		return productMetaOpenGraphDescription;
	}

	public void setProductMetaOpenGraphDescription(Localisable<String, String> productMetaOpenGraphDescription) {
		this.productMetaOpenGraphDescription = productMetaOpenGraphDescription;
	}

	public Localisable<String, String> getProductMetaTwitterTitle() {
		return productMetaTwitterTitle;
	}

	public void setProductMetaTwitterTitle(Localisable<String, String> productMetaTwitterTitle) {
		this.productMetaTwitterTitle = productMetaTwitterTitle;
	}

	public Localisable<String, String> getProductMetaTwitterDescription() {
		return productMetaTwitterDescription;
	}

	public void setProductMetaTwitterDescription(Localisable<String, String> productMetaTwitterDescription) {
		this.productMetaTwitterDescription = productMetaTwitterDescription;
	}

	
}
