package org.open4goods.model.vertical;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonMerge;

public class ProductI18nElements {
	@JsonMerge
	private PrefixedAttrText url = new PrefixedAttrText();
	@JsonMerge
	private PrefixedAttrText h1Title = new PrefixedAttrText();



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
	 * The custom pages names and associated templates for this vertical
	 */
	@JsonMerge
	private List<WikiPageConfig> wikiPages = new ArrayList<>();

	/**
	 * Configuration for ai generation tool
	 */
	@JsonMerge
	private AiPromptsConfig aiConfigs = new AiPromptsConfig();


	/**
	 * Return the truncated home description
	 * @param max
	 * @return
	 */
	public String truncatedHomeDescription(int max) {
		return verticalHomeDescription.length() > max ? verticalHomeDescription.substring(0, max)+"..." : verticalHomeDescription;
	}


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