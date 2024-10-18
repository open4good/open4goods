package org.open4goods.commons.model.product;

import org.open4goods.commons.model.Localisable;

public class ProductTexts {

	private Localisable<String,String> url = new Localisable<>();
	
	private Localisable<String,String> h1Title = new Localisable<>();
	
	private Localisable<String,String> metaDescription = new Localisable<>();
	
	private Localisable<String,String> productMetaOpenGraphTitle = new Localisable<>();
	
	private Localisable<String,String> productMetaOpenGraphDescription = new Localisable<>();
	
	private Localisable<String,String> productMetaTwitterTitle = new Localisable<>();
	
	private Localisable<String,String> productMetaTwitterDescription = new Localisable<>();
	

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
