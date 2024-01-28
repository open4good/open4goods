package org.open4goods.model.blog;

import java.util.ArrayList;
import java.util.List;

import org.open4goods.model.dto.WikiAttachment;

public class BlogPost {
	
	private String url;
	private String title;
	private String author;
	private String summary;
	
	private String body;
	private long created;
	private long modified;
	
	
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

	
	public long getCreated() {
		return created;
	}
	public void setCreated(long created) {
		this.created = created;
	}
	public long getModified() {
		return modified;
	}
	public void setModified(long modified) {
		this.modified = modified;
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


	
	
}
