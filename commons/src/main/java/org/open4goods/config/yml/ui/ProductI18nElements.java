package org.open4goods.config.yml.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.open4goods.config.yml.WikiPageConfig;
import org.open4goods.config.yml.attributes.PromptConfig;
import org.open4goods.config.yml.attributes.AiPromptsConfig;
import org.open4goods.model.dto.WikiPage;

import com.fasterxml.jackson.annotation.JsonMerge;

public class ProductI18nElements {
	@JsonMerge
	private PrefixedAttrText url = new PrefixedAttrText();
	@JsonMerge
	private PrefixedAttrText h1Title = new PrefixedAttrText();
	@JsonMerge
	private String productMetaTitle;
	@JsonMerge
	private String productMetaDescription;
	@JsonMerge
	private String productMetaOpenGraphTitle;
	@JsonMerge
	private String productMetaOpenGraphDescription;
	@JsonMerge
	private String productMetaTwitterTitle;
	@JsonMerge
	private String productMetaTwitterDescription;
	

	/**
	 * The url of the vertical home page
	 */
	@JsonMerge
	private String verticalHomeUrl ;

	
	@JsonMerge
	private String verticalMetaTitle;
	@JsonMerge
	private String verticalMetaDescription;
	@JsonMerge
	private String verticalMetaOpenGraphTitle;
	@JsonMerge
	private String verticalMetaOpenGraphDescription;
	@JsonMerge
	private String verticalMetaTwitterTitle;
	@JsonMerge
	private String verticalMetaTwitterDescription;
	/**
	 * The title on the vertical home page
	 */
	@JsonMerge
	private String verticalHomeTitle ;

	/**
	 * The description on the category section, on the home page
	 */
	@JsonMerge
	private String verticalHomeDescription ;

	/**
	 * The image logo on the vertical home page
	 */
	@JsonMerge
	private String verticalHomeLogo ;
	/**
	 * The custom pages names and associated templates for this vertical
	 */
	@JsonMerge
	private List<WikiPageConfig> wikiPages = new ArrayList<>();
	
	/**
	 * Configuration for ai generation tool
	 */
	@JsonMerge
	private AiPromptsConfig aiConfigs = new AiPromptsConfig();

	public PrefixedAttrText getUrl() {
		return url;
	}

	public void setUrl(PrefixedAttrText url) {
		this.url = url;
	}

	public PrefixedAttrText getH1Title() {
		return h1Title;
	}

	public void setH1Title(PrefixedAttrText h1Title) {
		this.h1Title = h1Title;
	}

	public String getProductMetaTitle() {
		return productMetaTitle;
	}

	public void setProductMetaTitle(String productMetaTitle) {
		this.productMetaTitle = productMetaTitle;
	}

	public String getProductMetaDescription() {
		return productMetaDescription;
	}

	public void setProductMetaDescription(String productMetaDescription) {
		this.productMetaDescription = productMetaDescription;
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

	public String getVerticalHomeUrl() {
		return verticalHomeUrl;
	}

	public void setVerticalHomeUrl(String verticalHomeUrl) {
		this.verticalHomeUrl = verticalHomeUrl;
	}

	public String getVerticalMetaTitle() {
		return verticalMetaTitle;
	}

	public void setVerticalMetaTitle(String verticalMetaTitle) {
		this.verticalMetaTitle = verticalMetaTitle;
	}

	public String getVerticalMetaDescription() {
		return verticalMetaDescription;
	}

	public void setVerticalMetaDescription(String verticalMetaDescription) {
		this.verticalMetaDescription = verticalMetaDescription;
	}

	public String getVerticalMetaOpenGraphTitle() {
		return verticalMetaOpenGraphTitle;
	}

	public void setVerticalMetaOpenGraphTitle(String verticalMetaOpenGraphTitle) {
		this.verticalMetaOpenGraphTitle = verticalMetaOpenGraphTitle;
	}

	public String getVerticalMetaOpenGraphDescription() {
		return verticalMetaOpenGraphDescription;
	}

	public void setVerticalMetaOpenGraphDescription(String verticalMetaOpenGraphDescription) {
		this.verticalMetaOpenGraphDescription = verticalMetaOpenGraphDescription;
	}

	public String getVerticalMetaTwitterTitle() {
		return verticalMetaTwitterTitle;
	}

	public void setVerticalMetaTwitterTitle(String verticalMetaTwitterTitle) {
		this.verticalMetaTwitterTitle = verticalMetaTwitterTitle;
	}

	public String getVerticalMetaTwitterDescription() {
		return verticalMetaTwitterDescription;
	}

	public void setVerticalMetaTwitterDescription(String verticalMetaTwitterDescription) {
		this.verticalMetaTwitterDescription = verticalMetaTwitterDescription;
	}

	public String getVerticalHomeTitle() {
		return verticalHomeTitle;
	}

	public void setVerticalHomeTitle(String verticalHomeTitle) {
		this.verticalHomeTitle = verticalHomeTitle;
	}

	public String getVerticalHomeDescription() {
		return verticalHomeDescription;
	}

	public void setVerticalHomeDescription(String verticalHomeDescription) {
		this.verticalHomeDescription = verticalHomeDescription;
	}

	public String getVerticalHomeLogo() {
		return verticalHomeLogo;
	}

	public void setVerticalHomeLogo(String verticalHomeLogo) {
		this.verticalHomeLogo = verticalHomeLogo;
	}


	public List<WikiPageConfig> getWikiPages() {
		return wikiPages;
	}

	public void setWikiPages(List<WikiPageConfig> wikiPages) {
		this.wikiPages = wikiPages;
	}

	public AiPromptsConfig getAiConfigs() {
		return aiConfigs;
	}

	public void setAiConfigs(AiPromptsConfig aiConfigs) {
		this.aiConfigs = aiConfigs;
	}


	
	
	

}