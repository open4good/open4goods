package org.open4goods.model.product;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import org.open4goods.model.Localisable;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

public class ProductTexts {

	private Localisable url = new Localisable();
	
	private Localisable h1Title = new Localisable();
	
	private Localisable metaDescription = new Localisable();
	
	private Localisable productMetaOpenGraphTitle = new Localisable();
	
	private Localisable productMetaOpenGraphDescription = new Localisable();
	
	private Localisable productMetaTwitterTitle = new Localisable();
	
	private Localisable productMetaTwitterDescription = new Localisable();
	
	
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


	public Localisable getMetaDescription() {
		return metaDescription;
	}

	public void setMetaDescription(Localisable metaDescription) {
		this.metaDescription = metaDescription;
	}

	public Localisable getProductMetaOpenGraphTitle() {
		return productMetaOpenGraphTitle;
	}

	public void setproductMetaOpenGraphTitle(Localisable productMetaOpenGraphTitle) {
		this.productMetaOpenGraphTitle = productMetaOpenGraphTitle;
	}

	public Localisable getproductMetaOpenGraphDescription() {
		return productMetaOpenGraphDescription;
	}

	public void setproductMetaOpenGraphDescription(Localisable productMetaOpenGraphDescription) {
		this.productMetaOpenGraphDescription = productMetaOpenGraphDescription;
	}

	public Localisable getproductMetaTwitterTitle() {
		return productMetaTwitterTitle;
	}

	public void setproductMetaTwitterTitle(Localisable productMetaTwitterTitle) {
		this.productMetaTwitterTitle = productMetaTwitterTitle;
	}

	public Localisable getproductMetaTwitterDescription() {
		return productMetaTwitterDescription;
	}
	public void setproductMetaTwitterDescription(Localisable productMetaTwitterDescription) {
		this.productMetaTwitterDescription = productMetaTwitterDescription;
	}

	
}
