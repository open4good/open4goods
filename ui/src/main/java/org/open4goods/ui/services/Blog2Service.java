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
import org.open4goods.model.blog.BlogPost2;
import org.open4goods.model.dto.WikiPage2;
import org.open4goods.services.Xwiki2Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

import io.micrometer.common.util.StringUtils;

public class Blog2Service {

private static final Logger logger = LoggerFactory.getLogger(Blog2Service.class);

	
	private BlogConfiguration config;
	private Xwiki2Service xwikiService;

	private Map<String, BlogPost2> postsByUrl = new HashMap<>();


	private Localisable baseUrl;
	
	
	public Blog2Service(Xwiki2Service wikiService,  BlogConfiguration config, Localisable localisable) {
		this.config = config;
		this.xwikiService = wikiService;
		this.baseUrl = localisable;
	}
	
	// TODO : Cacheable
	public Map<String, BlogPost2> getBlogPosts() {
	
		logger.info("Getting blog posts");
		Map<String, BlogPost2> postsByUrl = new HashMap<>();
		
		List<WikiPage2> wikiPages = xwikiService.getWikiPages("Blog");
		
		for (WikiPage2 wikiPage : wikiPages) {
			//WikiPage2 fullPage = xwikiService.get;
			BlogPost2 post = new BlogPost2();
			
			post.setUrl(IdHelper.azCharAndDigits(wikiPage.getPage().getTitle().toLowerCase().replace(" ", "-")));
			post.setTitle(wikiPage.getPage().getTitle());
			
			// Substring(7) : Remove "XWiki." from author
			post.setAuthor(WordUtils.capitalizeFully(wikiPage.getPage().getAuthor().substring(6)));			
			try {
				// TODO : Update when xwiki update to jakarta
//				post.setSummary(xwikiService.renderXWiki20SyntaxAsXHTML(fullPage.getProps().get("summary")));			
//				post.setBody(xwikiService.renderXWiki20SyntaxAsXHTML(fullPage.getProps().get("content")));
								
				post.setSummary(wikiPage.getProperties().get("summary"));
				post.setBody(wikiPage.getProperties().get("content"));
							
			} catch (Exception e) {
				logger.error("Error while rendering XWiki content", e);
			}
			
			post.setHidden("1".equals(wikiPage.getProperties().get("hidden")));
			// Skipping if hidden
			if (post.getHidden()) {
				continue;
			}
			
			// TODO : Evict a ghost page
			if (post.getTitle().equals("Blog")) {
				continue;
			}
			
			post.setAttachments(wikiPage.getAttachmentsList());
			post.setCreated(wikiPage.getPage().getCreated().getTimeInMillis());
			post.setModified(wikiPage.getPage().getModified().getTimeInMillis());

			String cats = wikiPage.getProperties().get("category");
			
			if (null != cats) {
				post.setCategory(Arrays.asList(cats.replace("Blog.","").split("\\|")));				
			}
			
			//post.setImage(getProxyUrl(fullPage.getSpace(), fullPage.getName(), (String)fullPage.getAdditionalProperties().get("image")));
			if( StringUtils.isNotEmpty(wikiPage.getProperties().get("image")) ) {
				//post.setImage(wikiPage.getAttachmentUrl(wikiPage.getProperties().get("image")));
				post.setImage(getProxyUrl(wikiPage.getPage().getSpace(), wikiPage.getPage().getName(), wikiPage.getProperties().get("image")));

			}
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
		for (BlogPost2 post : getBlogPosts().values()) {
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
