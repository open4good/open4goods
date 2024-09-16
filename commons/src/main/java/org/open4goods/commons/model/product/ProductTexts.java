package org.open4goods.commons.model.product;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import org.open4goods.commons.model.Localisable;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

public class ProductTexts {

	private String url;
	
	private String h1Title;
	
	private String metaDescription;
	
	private String productMetaOpenGraphTitle;
	
	private String productMetaOpenGraphDescription;
	
	private String productMetaTwitterTitle;
	
	private String productMetaTwitterDescription;

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getH1Title() {
		return h1Title;
	}

	public void setH1Title(String h1Title) {
		this.h1Title = h1Title;
	}

	public String getMetaDescription() {
		return metaDescription;
	}

	public void setMetaDescription(String metaDescription) {
		this.metaDescription = metaDescription;
	}

	public String getProductMetaOpenGraphTitle() {
		return productMetaOpenGraphTitle;
	}

	public void setProductMetaOpenGraphTitle(String productMetaOpenGraphTitle) {
		this.productMetaOpenGraphTitle = productMetaOpenGraphTitle;
	}

	public String getProductMetaOpenGraphDescription() {
		return productMetaOpenGraphDescription;
	}

	public void setProductMetaOpenGraphDescription(String productMetaOpenGraphDescription) {
		this.productMetaOpenGraphDescription = productMetaOpenGraphDescription;
	}

	public String getProductMetaTwitterTitle() {
		return productMetaTwitterTitle;
	}

	public void setProductMetaTwitterTitle(String productMetaTwitterTitle) {
		this.productMetaTwitterTitle = productMetaTwitterTitle;
	}

	public String getProductMetaTwitterDescription() {
		return productMetaTwitterDescription;
	}

	public void setProductMetaTwitterDescription(String productMetaTwitterDescription) {
		this.productMetaTwitterDescription = productMetaTwitterDescription;
	}
	
	

	
}
