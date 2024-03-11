package org.open4goods.model.dto;

import java.util.List;
import java.util.Map;

//import org.xwiki.rest.model.jaxb.Attachment;
import org.xwiki.rest.model.jaxb.Page;

public class WikiPage2 {

	List<WikiAttachment2> wikiAttachmentsList = null;
	Map<String,String> properties = null;
	Page page = null;
	
	public Page getPage() {
		return page;
	}

	public void setPage(Page page) {
		this.page = page;
	}

	public WikiPage2(Page page) {
		this.page = page;
	}

	public List<WikiAttachment2> getAttachmentsList() {
		return wikiAttachmentsList;
	}

	public void setAttachmentsList(List<WikiAttachment2> wikiAttachmentsList) {
		this.wikiAttachmentsList = wikiAttachmentsList;
	}
	
	/**
	 * get the download file url
	 * @param fileName
	 * @return
	 */
	public String getAttachmentUrl(String fileName) {
		String url = null;
		for(WikiAttachment2 attach: this.wikiAttachmentsList) {
			if( attach.getName().equals(fileName) ) {
				url = attach.getUrl();
				break;
			}
		}
		return url;
	}
	
	public Map<String, String> getProperties() {
		return properties;
	}

	public void setProperties(Map<String, String> properties) {
		this.properties = properties;
	}

}
