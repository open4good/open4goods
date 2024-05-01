package org.open4goods.ui.services;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.text.WordUtils;
import org.open4goods.config.yml.BlogConfiguration;
import org.open4goods.helper.IdHelper;
import org.open4goods.model.Localisable;
import org.open4goods.model.blog.BlogPost;
import org.open4goods.model.dto.WikiPage;
import org.open4goods.xwiki.services.XWikiReadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.rest.model.jaxb.Page;
import org.xwiki.rest.model.jaxb.PageSummary;
import org.xwiki.rest.model.jaxb.Pages;

import com.rometools.rome.feed.synd.SyndCategory;
import com.rometools.rome.feed.synd.SyndCategoryImpl;
import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndContentImpl;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndEntryImpl;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.feed.synd.SyndFeedImpl;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedOutput;

/**
 * This service handles XWiki content bridges :
 * > Authentication, through the registered users/password on the wiki
 * > RBAC, through roles
 * > Content retrieving, through the REST API
 * TODO : This class is a mess
 * @author Goulven.Furet
 */
public class BlogService {

	private static final Logger logger = LoggerFactory.getLogger(BlogService.class);

	
	private BlogConfiguration config;
	private XWikiReadService xwikiReadService;

	private Map<String, BlogPost> postsByUrl = new HashMap<>();


	private Localisable baseUrl;
	
	
	public BlogService(XWikiReadService wikiService,  BlogConfiguration config, Localisable localisable) {
		this.config = config;
		this.xwikiReadService = wikiService;
		this.baseUrl = localisable;
	}
	
	// TODO : Cacheable
	public Map<String, BlogPost> getBlogPosts() {
	
		logger.info("Getting blog posts");
		Map<String, BlogPost> postsByUrl = new HashMap<>();
		
<<<<<<< Upstream, based on origin/main

=======
		List<WikiPage> pages = xwikiReadService.getPages("Blog");
>>>>>>> f9c909d Ending first round
		
<<<<<<< Upstream, based on origin/main
		
		
		Pages pages = xwikiReadService.getPages("Blog");
		
		for (PageSummary page : pages.getPageSummaries()) {
			Page fullPage = xwikiReadService.getPage(page.getSpace(), page.getName());
=======
		for (WikiPage page : pages) {
			WikiPage fullPage = xwikiReadService.getPage(page.getSpace(), page.getName());
>>>>>>> f9c909d Ending first round
			BlogPost post = new BlogPost();
			
			post.setUrl(IdHelper.azCharAndDigits(fullPage.getTitle().toLowerCase().replace(" ", "-")));
			post.setTitle(fullPage.getTitle());
			
			// Substring(7) : Remove "XWiki." from author
			post.setAuthor(WordUtils.capitalizeFully(fullPage.getAuthor().substring(6)));			
			try {
				// TODO : Update when xwiki update to jakarta
//				post.setSummary(xwikiService.renderXWiki20SyntaxAsXHTML(fullPage.getProps().get("summary")));			
//				post.setBody(xwikiService.renderXWiki20SyntaxAsXHTML(fullPage.getProps().get("content")));
				
				
//				post.setSummary(fullPage.getProps().get("summary"));
				System.out.println(fullPage.getContent());
				post.setBody(fullPage.getContent());
							
			} catch (Exception e) {
				logger.error("Error while rendering XWiki content", e);
			}
			
			post.setHidden(fullPage.isHidden());
			// Skipping if hidden
			if (post.getHidden()) {
				continue;
			}
			
			// TODO : Evict a ghost page
			if (post.getTitle().equals("Blog")) {
				continue;
			}
			
			
			post.setCreated(fullPage.getCreated().getTimeInMillis()  );
			post.setModified(fullPage.getModified().getTimeInMillis());

			// TODO : put back
//			post.setAttachments(fullPage.getAttachments());
//			String cats = fullPage.getProps().get("category");
			
//			if (null != cats) {
//				post.setCategory(Arrays.asList(cats.replace("Blog.","").split("\\|")));				
//			}
			
//			post.setImage(getProxyUrl(fullPage.getSpace(), fullPage.getName(), fullPage.getProps().get("image")));
						
			postsByUrl.put(post.getUrl(), post);
			
			// If we have no summary, truncate the first 100 chars
			// TODO : Truncation from config
			if (null != post.getBody() &&  null == post.getSummary() && post.getBody().length() > 100) {
				post.setSummary(post.getBody().substring(0, 100)+" ...");
			}
			
		}
		

		
		return postsByUrl;
	}


	/**
	 * Generate the RSS feed
	 * @param lang
	 * @return
	 * @throws FeedException
	 */
	public String rss (String lang) throws FeedException {
		logger.info("Generating RSS feed");
		SyndFeed feed = new SyndFeedImpl();
		feed.setEncoding("UTF-8");
		feed.setFeedType(config.getFeedType());
		feed.setTitle(config.getFeedTitle().i18n(lang));
		feed.setDescription(config.getFeedDescription().i18n(lang));
		
		feed.setLink(baseUrl.i18n(lang) +config.getFeedUrl());
		
//		feed.setCopyright(lang)
//		feed.setAuthors(null)
//		feed.setCategories(null)
				
		// TODO : i18n filtering
		feed.setEntries(new ArrayList<>());
		for (BlogPost post : getBlogPosts().values()) {
			SyndEntry entry = new SyndEntryImpl();
			entry.setTitle(post.getTitle());
			entry.setLink(baseUrl.i18n(lang) + config.getBlogUrl() + post.getUrl());

			SyndContent description = new SyndContentImpl();
			description.setType("text/html");
			description.setValue(post.getSummary());

			entry.setDescription(description);

			List<SyndCategory> categories = new ArrayList<>();
			for (String cat : post.getCategory()) {
				SyndCategory category = new SyndCategoryImpl();
				category.setName(cat);
				categories.add(category);
			}
			entry.setCategories(categories);
			feed.getEntries().add(entry);
		}
		
		SyndFeedOutput syndFeedOutput = new SyndFeedOutput();
		return syndFeedOutput.outputString(feed,true);
	}
	
	
	private String getProxyUrl(String space, String name, String file) {		
		return "/attachments/" + space + "/" + name + "/" + file;
	}
}