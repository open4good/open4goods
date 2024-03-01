package org.open4goods.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.open4goods.config.yml.XwikiConfiguration;
import org.open4goods.model.dto.WikiAttachment2;
import org.open4goods.model.dto.WikiPage2;
import org.open4goods.xwiki.services.XWikiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.xwiki.rest.model.jaxb.Attachment;
import org.xwiki.rest.model.jaxb.Page;


/**
 * This service handles XWiki content bridges :
 * @TODO: > Authentication, through the registered users/password on the wiki
 * @TODO: > RBAC, through roles
 * > Content retrieving, through the REST API
 * @TODO: caching
 * 
 * @author Thierry.Ledan
 */
public class Xwiki2Service {

	private static final Logger logger = LoggerFactory.getLogger(Xwiki2Service.class);

	//private final Map<String,WikiResult> contentCache = new ConcurrentHashMap<>();

	private final XwikiConfiguration config;
	
	@Autowired
	XWikiService xwikiServices;

	
	//Define the formatter for the given date pattern
    //DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssX");

	public Xwiki2Service(XwikiConfiguration config) {
		this.config = config;
	}
	

	/**
	 * Set a list a WikiPage object from list of retrieved Page object.
	 * Then add attachments and properties to each WikiPage
	 * 
	 * @param space
	 * @return list of WikiPage 
	 */
	public List<WikiPage2> getWikiPages (String space) {
		
		List<WikiPage2> wikiPageList = new ArrayList<WikiPage2>();
		// get all Pages
		List<Page> pages = null;
		try {
			pages = xwikiServices.getPagesList(space);
		} catch (Exception e) {
			logger.error("Exception catched from xwiki starter:{}", e.getMessage());
		}
		
		// then get attachments and properties for each page
		WikiPage2 wikiPage;
		for(Page page: pages) {
			try {
				wikiPage = new WikiPage2(page);
				// manage attachments (with right scheme for url)
				List<Attachment> attachments = xwikiServices.getAttachmentList(page);
				if( attachments != null && attachments.size() > 0 ) {
					List<WikiAttachment2> wikiAttachments = new ArrayList<WikiAttachment2>();
					WikiAttachment2 wikiAttachment = null;
					for(Attachment attachment: attachments) {
						wikiAttachment = new WikiAttachment2();
						wikiAttachment.setUrl(xwikiServices.getAttachmentUrl(attachment));
						wikiAttachment.setId(attachment.getId());
						wikiAttachment.setName(attachment.getName());
						wikiAttachment.setSize(attachment.getSize());
						wikiAttachment.setVersion(attachment.getVersion());
						wikiAttachment.setPageId(attachment.getPageId());
						wikiAttachment.setPageVersion(attachment.getPageVersion());
						wikiAttachment.setMimeType(attachment.getMimeType());
						wikiAttachment.setAuthor(attachment.getAuthor());
						wikiAttachment.setDate(attachment.getDate().getTimeInMillis());
						// add new WikiAttachment to list
						wikiAttachments.add(wikiAttachment);
					}
					// set wikipage attachments
					wikiPage.setAttachmentsList(wikiAttachments);
				}
				wikiPage.setProperties(xwikiServices.getProperties(page));
				wikiPageList.add(wikiPage);
			} catch(Exception e) {
				logger.warn("Exception catched :{}", e.getMessage());
			}
		}
		return wikiPageList;
	}
	

	/**
	 * 
	 * 
	 * @param space
	 * @param pageName
	 * @return
	 */
	public Page getPage (String space, String pageName) {
		
		Page page = null;
		try {
			page = xwikiServices.getPage(space, pageName);
				
		} catch (Exception e) {
			logger.error("Exception catched from xwiki starter:{}", e.getMessage());
		}
		return page;
	}
	
	
	/**
	 * Get the URL of an image, given its name and space
	 * @param space
	 * @param name
	 * @param string
	 * @return
	 */
	public String getAttachmentUrl(String space, String name, String attachmentName) {
		
		return config.getBaseUrl()+"/bin/download/"+space+"/"+name+"/"+attachmentName;
	}
	/**
	 * 
	 * @param date
	 * @return
	 */
//	public long parseWikiDate (String date) {
//        // Parse the date string into a LocalDateTime object
//        LocalDateTime dateTime = LocalDateTime.parse(date, formatter);
//        // Convert LocalDateTime to epoch seconds (Unix timestamp)
//       return dateTime.toEpochSecond(ZoneOffset.UTC);
//	}
	
		
//	public void invalidate(String doc) {
//		logger.warn("Invalidating wiki cache : {}",doc);
//		contentCache.remove(doc.replace(".", "/"));
//	}
//
//	public void invalidateAll() {
//		contentCache.clear();
//	}
	
	
	

}
