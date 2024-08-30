package org.open4goods.ui.services;


import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;
import org.open4goods.commons.config.yml.BlogConfiguration;
import org.open4goods.commons.helper.IdHelper;
import org.open4goods.commons.model.Localisable;
import org.open4goods.ui.controllers.ui.pages.BlogController;
import org.open4goods.ui.model.BlogPost;
import org.open4goods.xwiki.model.FullPage;
import org.open4goods.xwiki.services.XwikiFacadeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Health.Builder;
import org.springframework.boot.actuate.health.HealthIndicator;
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

import jakarta.annotation.PostConstruct;

/**
 * This service handles blog functionalities, built over the Xwiki blog application
 * @author Goulven.Furet
 */
public class BlogService implements HealthIndicator{

	private static final String XWIKI_BLOGPOST_START_MARKUP = "<div class=\"entry-content\">";
	private static final String XWIKI_BLOGPOST_STOP_MARKUP = "<div class=\"entry-footer\">";
	
	private static final Logger logger = LoggerFactory.getLogger(BlogService.class);

			
	private BlogConfiguration config;
	private XwikiFacadeService xwikiFacadeService;
	private Map<String, BlogPost> postsByUrl = new ConcurrentHashMap<>();
	private List<BlogPost> posts = new ArrayList<>();
	private Localisable<String,String> baseUrl;

	private DateTimeFormatter blogDateformatter = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH);
	
	// States variables for healthcheck
	int expectectedBlogPagesCount = 0;
	int exceptionsCount = 0;
	
	public BlogService(XwikiFacadeService xwikiFacadeService,  BlogConfiguration config, Localisable<String,String> baseUrls) {
		this.config = config;
		this.xwikiFacadeService = xwikiFacadeService;
		this.baseUrl = baseUrls;
	}

	// TODO(p3, conf) : Schedule from conf
	@Scheduled(fixedDelay = 1000 * 3600*2)
	@PostConstruct
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
	
	
	/**
	 * Retrieves the blog posts from the Xwiki 
	 */
	public void updateBlogPosts() {
		logger.info("Getting blog posts");
		Pages pages = xwikiFacadeService.getPages("Blog");
		
		// Setting expected number of blog articles
		expectectedBlogPagesCount = pages.getPageSummaries().size();
		
		List<BlogPost> posts = new ArrayList<>();
		for (PageSummary page : pages.getPageSummaries()) {	
			
			try {
				BlogPost post = new BlogPost();

				// Reading the blog post
				FullPage fullPage = xwikiFacadeService.getFullPage(page.getSpace(), page.getName());
				post.setWikiPage(fullPage);

				// To discard internal xwiki page, that is not a blog post
				if (page.getFullName().endsWith(".WebHome")) {
					expectectedBlogPagesCount--;
					continue;
				}

				// Evict the xwiki blog default page
				if ("Blog".equals(fullPage.getProperties().get("title"))) {
					expectectedBlogPagesCount--;
					continue;
				}
				
				// Skipping if hidden
				if (fullPage.getWikiPage().isHidden()) {
					expectectedBlogPagesCount--;
					continue;
				}
				
				////////////////////////
				// Setting blogpostInfos
				////////////////////////


				// The image
				String image = fullPage.getProperties().get("image");						
				if (!StringUtils.isEmpty(image)) {
					String fullImage = getBlogImageUrl( URLEncoder.encode(page.getName(), Charset.defaultCharset()), URLEncoder.encode(image, Charset.defaultCharset()));
					post.setImage(fullImage);				
				}
				
				// Set the Author
				// Substring(6) : Remove "XWiki." from author
				post.setAuthor(WordUtils.capitalizeFully(fullPage.getWikiPage().getAuthor().substring(6)));		
				
				
				// post summary
				String extract = fullPage.getProperties().get("extract");
				post.setSummary(extract);
				
				// Setting the post category
				String category =  fullPage.getProperties().get("category");
				if (null != category) {
					post.setCategory(Arrays.asList(category.replace("Blog.","").split("\\|")));				
				}
				
				// Get blog publish date as an epoch
				String  publishDate = fullPage.getProperties().get("publishDate");
				ZonedDateTime zonedDateTime = ZonedDateTime.parse(publishDate, blogDateformatter);
				long publishedDateMs = zonedDateTime.toInstant().toEpochMilli();
				post.setCreated( new Date(publishedDateMs));
				
				
				
				// Get last modification as epoch 
				post.setModified(new Date(fullPage.getWikiPage().getModified().getTimeInMillis()));
				
				// Setting the post title
				String title = fullPage.getProperties().get("title");
				post.setTitle(title);

				// Derivating the open4goods blog url from the post title
				post.setUrl(getPostUrl(title));

				// Setting the edit link
				post.setEditLink(xwikiFacadeService.getPathHelper().getEditpath(page.getId().replace("xwiki:", "").split("\\.")));

				
				// Setting the post content, from xwiki content html markup parsing
				
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
				
				// Replace blog attachments links with proxied equivalent nudger links
				html = html.replace("\"/bin/download/Blog","\"" + BlogController.DEFAULT_PATH);
				
				post.setBody(html);
					

				
				// If we have no summary, truncate the first chars from the full post
				// TODO(p3,conf) : Truncation from config
				if (null != post.getBody() &&  null == post.getSummary() && post.getBody().length() > 100) {
					post.setSummary(post.getBody().substring(0, 100)+" ...");
				}
				
				
				// Save the post in instance variable
				posts.add(post);
			} catch (Exception e) {
				logger.error("Error while setting blog post from XWiki content", e);
				exceptionsCount++;

			}
			
		}

		// Replacing existing posts by order sorted ones
		this.posts.clear();
		Collections.sort(posts, (o1, o2) -> o2.getCreated().compareTo(o1.getCreated()));
		this.posts.addAll(posts);
		
		// Update the inversed map
		postsByUrl.clear();
		posts.stream().forEach(post -> {
			postsByUrl.put(post.getUrl(), post);
			
		});
		
		
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
				
		// TODO(p3,i18) : i18n filtering
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
	
	/**
	 * Rename the image in order to be proxified by xwiki component
	 * @see BlogController.attachment
	 * @param name
	 * @param image
	 * @return
	 */
	private String getBlogImageUrl( String name, String image) {
		return BlogController.DEFAULT_PATH+ "/"+name+"/"+image;
	}

	/**
	 * Return a proper name for a post URL
	 * @param title
	 * @return
	 */
	private String getPostUrl(String title) {
		return StringUtils.strip(IdHelper.azCharAndDigits(StringUtils.normalizeSpace(title).toLowerCase(),"-"),"-");
	}


	/**
	 * Custom healthcheck, 
	 */
	@Override
	public Health health() {
		
		Builder health = Health.up()
				.withDetail("posts_count", posts.size())
				.withDetail("exceptions_count", exceptionsCount)
				;
		
		if (posts.size()==0) {
			health = health.down();
		}

		if (exceptionsCount>0) {
			health = health.down();
		}

		if (expectectedBlogPagesCount != posts.size()) {
			health = health.down().withDetail("invalid_expected_posts_count", "Was expecting " + expectectedBlogPagesCount + " get " + posts.size());
		}
		
		return health.build();
	}
	
	///////////////////////////////
	// Getters and setters
	//////////////////////////////

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