package org.open4goods.model.blog;

import java.util.ArrayList;
import java.util.List;

import org.open4goods.model.dto.WikiAttachment;

public class BlogPost {
	
	private String url;
	private String title;
	private String author;
	private String summary;
	private String editLink;
	
	private String body;
	private String created;
	private Long createdMs;
	
	private String modified;
	
	
	private List<String> category = new ArrayList<>();
	private Boolean hidden;
	
	private String image;
	
	// Some more attachments, drom wiki page, should not be usefull
	private List<WikiAttachment> attachments = new ArrayList<>();
	
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

	
	public void setCreated(String created) {
		this.created = created;
	}
	public List<WikiAttachment> getAttachments() {
		return attachments;
	}
	public void setAttachments(List<WikiAttachment> attachments) {
		this.attachments = attachments;
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
	public String getModified() {
		return modified;
	}
	public void setModified(String modified) {
		this.modified = modified;
	}
	public String getCreated() {
		return created;
	}
	public Long getCreatedMs() {
		return createdMs;
	}
	public void setCreatedMs(Long createdMs) {
		this.createdMs = createdMs;
	}
	public String getEditLink() {
		return editLink;
	}
	public void setEditLink(String editLink) {
		this.editLink = editLink;
	}


	
	
}
