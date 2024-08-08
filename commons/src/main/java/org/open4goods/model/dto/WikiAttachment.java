package org.open4goods.model.dto;

/**
 * A xwiki attachment
 */
public class WikiAttachment {

	
	private String id;

//	<id>xwiki:Blog.BlogIntroduction@blog-post-thumbnail.jpg</id>
//	<name>blog-post-thumbnail.jpg</name>
//	<size>126296</size>
//	<longSize>126296</longSize>
//	<version>1.1</version>
//	<pageId>xwiki:Blog.BlogIntroduction</pageId>
//	<pageVersion>2.1</pageVersion>
//	<mimeType>image/jpeg</mimeType>
//	<author>XWiki.o4g</author>
	
	private String url;
	private String name;
	private String size;
	private String version;
	private String pageId;
	private String pageVersion;
	private String mimeType;
	private String author;
	private String date;
	
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getSize() {
		return size;
	}
	public void setSize(String size) {
		this.size = size;
	}

	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	public String getPageId() {
		return pageId;
	}
	public void setPageId(String pageId) {
		this.pageId = pageId;
	}
	public String getPageVersion() {
		return pageVersion;
	}
	public void setPageVersion(String pageVersion) {
		this.pageVersion = pageVersion;
	}
	public String getMimeType() {
		return mimeType;
	}
	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}
	public String getAuthor() {
		return author;
	}
	public void setAuthor(String author) {
		this.author = author;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getDate() {
		return date;
	}
	public void setDate(String date) {
		this.date = date;
	}
		
	
	
	
}