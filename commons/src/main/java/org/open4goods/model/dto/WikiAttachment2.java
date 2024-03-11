package org.open4goods.model.dto;

/**
 * A xwiki attachment broker
 * 
 * 
 * @author Thierry.Ledan
 */
public class WikiAttachment2 {

	
	private String id;
	private String url;
	private String name;
	private int size;
	private String version;
	private String pageId;
	private String pageVersion;
	private String mimeType;
	private String author;
	private long date;
	
	public WikiAttachment2(){
		
	}
	
//	public WikiAttachment2 (Attachment attachment){
//		this.url = attachment.getXwikiAbsoluteUrl();
//		private String name;
//		private String size;
//		private String version;
//		private String pageId;
//		private String pageVersion;
//		private String mimeType;
//		private String author;
//		private String date;
//	}
	
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
	public int getSize() {
		return size;
	}
	public void setSize(int size) {
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
	public long getDate() {
		return date;
	}
	public void setDate(long date) {
		this.date = date;
	}
		
	
	
	
}