package org.open4goods.model.dto;

public class WikiResult {


	private String pageName;
	private String pageTitle;

	private String html;

	private String author;


	private String layout;

	private String width;


	private String metaTitle;
	private String metaDescription;
	private String metaKeyword;

	private String viewLink;

	private String editLink;



	public String getHtml() {
		return html;
	}

	public void setHtml(String html) {
		this.html = html;
	}

	public String getPageName() {
		return pageName;
	}

	public void setPageName(String title) {
		pageName = title;
	}


	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public String getViewLink() {
		return viewLink;
	}

	public void setViewLink(String viewLink) {
		this.viewLink = viewLink;
	}

	public String getEditLink() {
		return editLink;
	}

	public void setEditLink(String editLink) {
		this.editLink = editLink;
	}

	public String getLayout() {
		return layout;
	}

	public void setLayout(String layout) {
		this.layout = layout;
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

	public String getMetaKeyword() {
		return metaKeyword;
	}

	public void setMetaKeyword(String metaKeyword) {
		this.metaKeyword = metaKeyword;
	}

	public String getWidth() {
		return width;
	}

	public void setWidth(String width) {
		this.width = width;
	}

	public String getPageTitle() {
		return pageTitle;
	}

	public void setPageTitle(String pageTitle) {
		this.pageTitle = pageTitle;
	}





}