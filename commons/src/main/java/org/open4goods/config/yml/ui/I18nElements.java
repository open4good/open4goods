package org.open4goods.config.yml.ui;

public class I18nElements {

	private PrefixedAttrText url = new PrefixedAttrText();

	private PrefixedAttrText h1Title = new PrefixedAttrText();
	private String metaTitle;
	private String metaDescription;
	private String opengraphTitle;
	private String openGraphDescription;
	private String twitterTitle;
	private String twitterDescription;

	// Getters and setters

	public PrefixedAttrText getUrl() {
		return url;
	}

	public void setUrl(PrefixedAttrText url) {
		this.url = url;
	}

	public String getOpengraphTitle() {
		return opengraphTitle;
	}

	public void setOpengraphTitle(String opengraphTitle) {
		this.opengraphTitle = opengraphTitle;
	}

	public String getOpenGraphDescription() {
		return openGraphDescription;
	}

	public void setOpenGraphDescription(String openGraphDescription) {
		this.openGraphDescription = openGraphDescription;
	}

	public String getTwitterTitle() {
		return twitterTitle;
	}

	public void setTwitterTitle(String twitterTitle) {
		this.twitterTitle = twitterTitle;
	}

	public String getTwitterDescription() {
		return twitterDescription;
	}

	public void setTwitterDescription(String twitterDescription) {
		this.twitterDescription = twitterDescription;
	}

	public String getMetaTitle() {
		return metaTitle;
	}

	public void setMetaTitle(String metaTitle) {
		this.metaTitle = metaTitle;
	}

	public String getMetaDescription() {
		return metaDescription;
	}

	public void setMetaDescription(String metaDescription) {
		this.metaDescription = metaDescription;
	}

	public PrefixedAttrText getH1Title() {
		return h1Title;
	}

	public void setH1Title(PrefixedAttrText h1Title) {
		this.h1Title = h1Title;
	}
	
	

}