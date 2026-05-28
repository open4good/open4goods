package org.open4goods.model.product;

import org.open4goods.model.Localisable;

public class ProductTexts {

	private Localisable<String,String> url = new Localisable<>();

	private Localisable<String,String> displayName = new Localisable<>();

	private Localisable<String,String> cardName = new Localisable<>();

	private Localisable<String,String> pageTitle = new Localisable<>();

	private Localisable<String,String> seoName = new Localisable<>();
	
	private Localisable<String,String> metaDescription = new Localisable<>();
	
	private Localisable<String,String> productMetaOpenGraphTitle = new Localisable<>();
	
	private Localisable<String,String> productMetaOpenGraphDescription = new Localisable<>();
	

	public Localisable<String, String> getUrl() {
		return url;
	}

	public void setUrl(Localisable<String, String> url) {
		this.url = url;
	}

	public Localisable<String, String> getDisplayName() {
		return displayName;
	}

	public void setDisplayName(Localisable<String, String> displayName) {
		this.displayName = displayName;
	}

	public Localisable<String, String> getCardName() {
		return cardName;
	}

	public void setCardName(Localisable<String, String> cardName) {
		this.cardName = cardName;
	}

	public Localisable<String, String> getPageTitle() {
		return pageTitle;
	}

	public void setPageTitle(Localisable<String, String> pageTitle) {
		this.pageTitle = pageTitle;
	}

	public Localisable<String, String> getSeoName() {
		return seoName;
	}

	public void setSeoName(Localisable<String, String> seoName) {
		this.seoName = seoName;
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

	
}
