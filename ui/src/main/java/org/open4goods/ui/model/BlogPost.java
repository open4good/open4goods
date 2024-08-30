package org.open4goods.ui.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.open4goods.xwiki.model.FullPage;

/**
 * Representation of a xwiki blog post entry
 */
public class BlogPost {
	
	private String url;
	private String title;
	private String author;
	private String summary;
	private String editLink;
	
	private String body;
	private Date created;
	
	private Date modified;
	
	private List<String> category = new ArrayList<>();
	
	private Boolean hidden;
	
	private String image;
	
	private FullPage wikiPage;
	
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getAuthor() {
		return author;
	}
	public void setAuthor(String author) {
		this.author = author;
	}
	public String getSummary() {
		return summary;
	}
	public void setSummary(String summary) {
		this.summary = summary;
	}
	public String getBody() {
		return body;
	}
	public void setBody(String body) {
		this.body = body;
	}

	public List<String> getCategory() {
		return category;
	}
	public void setCategory(List<String> category) {
		this.category = category;
	}
	public Boolean getHidden() {
		return hidden;
	}
	public void setHidden(Boolean hidden) {
		this.hidden = hidden;
	}
	public String getImage() {
		return image;
	}
	public void setImage(String image) {
		this.image = image;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getEditLink() {
		return editLink;
	}
	public void setEditLink(String editLink) {
		this.editLink = editLink;
	}
	public Date getCreated() {
		return created;
	}
	public void setCreated(Date created) {
		this.created = created;
	}
	public Date getModified() {
		return modified;
	}
	public void setModified(Date modified) {
		this.modified = modified;
	}
	public FullPage getWikiPage() {
		return wikiPage;
	}
	public void setWikiPage(FullPage wikiPage) {
		this.wikiPage = wikiPage;
	}

	
	
	
}
