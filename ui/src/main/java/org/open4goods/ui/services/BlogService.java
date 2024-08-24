package org.open4goods.ui.services;


import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;
import org.open4goods.commons.config.yml.BlogConfiguration;
import org.open4goods.commons.helper.IdHelper;
import org.open4goods.commons.model.Localisable;
import org.open4goods.ui.model.BlogPost;
import org.open4goods.xwiki.model.FullPage;
import org.open4goods.xwiki.services.XwikiFacadeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
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
 * TODO : Localisation
 * @author Goulven.Furet
 */
public class BlogService {

	private static final String XWIKI_BLOGPOST_START_MARKUP = "<div class=\"entry-content\">";
	private static final String XWIKI_BLOGPOST_STOP_MARKUP = "<div class=\"entry-footer\">";
	
	private static final Logger logger = LoggerFactory.getLogger(BlogService.class);

	
	private BlogConfiguration config;
	private XwikiFacadeService xwikiFacadeService;
	private Map<String, BlogPost> postsByUrl = new ConcurrentHashMap<>();
	private List<BlogPost> posts = new ArrayList<>();
	private Localisable<String,String> baseUrl;

	
	
	public BlogService(XwikiFacadeService xwikiFacadeService,  BlogConfiguration config, Localisable<String,String> localisable) {
		this.config = config;
		this.xwikiFacadeService = xwikiFacadeService;
		this.baseUrl = localisable;
	}

	// TODO : From conf
	@Scheduled(initialDelay = 2000, fixedDelay = 1000 * 3600*2)
	public void refreshPosts() {
		updateBlogPosts();
	}
	

	/**
	 * Return blog posts for a given tag
	 * @param vertical
	 * @return
	 */
	public List<BlogPost> getPosts(String vertical) {
		return posts.stream().filter(e->e.getCategory().contains(vertical)).toList();
	}
	
	
	// TODO : Cacheable, or better @scheduled
	public void updateBlogPosts() {
		logger.info("Getting blog posts");
		Pages pages = xwikiFacadeService.getPages("Blog");
		List<BlogPost> posts = new ArrayList<>();
		for (PageSummary page : pages.getPageSummaries()) {		
			if (page.getFullName().endsWith(".WebHome")) {
				continue;
			}
			FullPage fullPage = xwikiFacadeService.getFullPage(page.getSpace(), page.getName());
			BlogPost post = new BlogPost();
			post.setWikiPage(fullPage);
			String image = fullPage.getProperties().get("image");
			
						
			String extract = fullPage.getProperties().get("extract");
//			int hidden = Integer.valueOf(fullPage.getProperties().get("hidden"));
			String  publishDate = fullPage.getProperties().get("publishDate");
			// TODO :  DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss zzz yyyy");

			
			
//			int published =   Integer.valueOf(fullPage.getProperties().get("published"));
			String category =  fullPage.getProperties().get("category");
			String title = fullPage.getProperties().get("title");
//			String content = fullPage.getProperties().get("content");
			
//			List<String> categories = Arrays.asList(category.replace("Blog.", "").split("|"));
			
			post.setUrl(getPostUrl(title));
			posts.add(post);
			// Maintain the inversed map
			postsByUrl.put(post.getUrl(), post);
						
			post.setTitle(title);
			post.setEditLink(xwikiFacadeService.getPathHelper().getEditpath(page.getId().replace("xwiki:", "").split("\\.")));
			
			if (!StringUtils.isEmpty(image)) {
				String fullImage = getBlogImageUrl( URLEncoder.encode(page.getName(), Charset.defaultCharset()), URLEncoder.encode(image, Charset.defaultCharset()));
				post.setImage(fullImage);				
			}
			
			
			// Substring(7) : Remove "XWiki." from author
			post.setAuthor(WordUtils.capitalizeFully(fullPage.getWikiPage().getAuthor().substring(6)));			
			try {
				post.setSummary(extract);
				
				
				String html = fullPage.getHtmlContent();
				// Remove the leading xwiki edit markup pages
				int pos = html.indexOf(XWIKI_BLOGPOST_START_MARKUP);
				if (-1 != pos) {
					html = html.substring(pos);
				}
				
				pos = html.indexOf(XWIKI_BLOGPOST_STOP_MARKUP);
				if (-1 != pos) {
					html = html.substring(0,pos);
				}
				
				
				//TODO : Markup should be mutualized with BlogController / downloadAttachment mapping
				html = html.replace("\"/bin/download/Blog","\"/blog");
				
				
				post.setBody(html);
							
				// Remove the trailing markup
				
				
				
			} catch (Exception e) {
				logger.error("Error while rendering XWiki content", e);
			}
			
			post.setHidden(fullPage.getWikiPage().isHidden());
			// Skipping if hidden
			if (post.getHidden()) {
				continue;
			}
			
			// TODO : Evict a ghost page
			if (post.getTitle().equals("Blog")) {
				continue;
			}
			
//			String date = fullPage.getWikiPage().getCreated().getTime().toLocaleString();
			// TODO : Proper i18n, 
//			date = date.substring(0,date.indexOf(','));			
			post.setCreated(publishDate);
			post.setCreatedMs(fullPage.getWikiPage().getCreated().getTimeInMillis());
			post.setModified(fullPage.getWikiPage().getModified().toString());

			// TODO : Handle attachment
//			post.setAttachments(fullPage.getAttachments());
			
			if (null != category) {
				post.setCategory(Arrays.asList(category.replace("Blog.","").split("\\|")));				
			}
			// TODO : handle image
//			post.setImage(getProxyUrl(fullPage.getSpace(), fullPage.getName(), fullPage.getProps().get("image")));
						
			// If we have no summary, truncate the first 100 chars
			// TODO : Truncation from config
			if (null != post.getBody() &&  null == post.getSummary() && post.getBody().length() > 100) {
				post.setSummary(post.getBody().substring(0, 100)+" ...");
			}
			
		}
		this.posts.clear();
		Collections.sort(posts, (o1, o2) -> Long.compare(o2.getCreatedMs(), o1.getCreatedMs()));
		this.posts.addAll(posts);
		
	}

	private String getBlogImageUrl( String name, String image) {
		return "/blog/"+name+"/"+image;
	}

	private String getPostUrl(String title) {
		return StringUtils.strip(IdHelper.azCharAndDigits(StringUtils.normalizeSpace(title).toLowerCase(),"-"),"-");
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
		for (BlogPost post : postsByUrl.values()) {
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

	public Map<String, BlogPost> getPostsByUrl() {
		return postsByUrl;
	}

	public void setPostsByUrl(Map<String, BlogPost> postsByUrl) {
		this.postsByUrl = postsByUrl;
	}

	public List<BlogPost> getPosts() {
		return posts;
	}

	public void setPosts(List<BlogPost> posts) {
		this.posts = posts;
	}



	

}