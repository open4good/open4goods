package org.open4goods.services.blog.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;
import org.open4goods.services.blog.config.BlogConfiguration;
import org.open4goods.model.Localisable;
import org.open4goods.model.helper.IdHelper;
import org.open4goods.services.blog.model.BlogPost;
import org.open4goods.xwiki.model.FullPage;
import org.open4goods.xwiki.services.XwikiFacadeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Health.Builder;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
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
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

import java.util.concurrent.CompletableFuture;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

/**
 * Service class for handling blog functionalities built over the XWiki blog application.
 * <p>
 * This service is responsible for refreshing blog posts from XWiki, generating an RSS feed,
 * and exposing blog-related data for the UI. It uses a scheduled task to update posts and
 * maintains internal health check data.
 * </p>
 *
 * @author Goulven.Furet
 */
@Service
public class BlogService implements HealthIndicator {

    private static final String XWIKI_BLOGPOST_START_MARKUP = "<div class=\"entry-content\">";
    private static final String XWIKI_BLOGPOST_STOP_MARKUP = "<div class=\"entry-footer\">";
    private static final Logger logger = LoggerFactory.getLogger(BlogService.class);

    private final BlogConfiguration config;
    private final XwikiFacadeService xwikiFacadeService;
    private final Localisable<String, String> baseUrl;

    // Volatile fields to ensure visibility in concurrent access.
    private volatile List<BlogPost> posts = Collections.emptyList();
    private volatile Map<String, BlogPost> postsByUrl = Collections.emptyMap();
    private volatile Map<String, Integer> tags = Collections.emptyMap();

    // Date formatter for blog publish dates (thread-safe)
    private final DateTimeFormatter blogDateFormatter = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH);

    // Health check counters
    private volatile int expectedBlogPagesCount = 0;
    private volatile int exceptionsCount = 0;

    // Indicates if a posts update is currently running.
    private final AtomicBoolean loading = new AtomicBoolean(false);

    /**
     * Constructs a new BlogService instance.
     *
     * @param xwikiFacadeService service to communicate with XWiki
     * @param config             blog configuration
     * @param baseUrl            base URL localisable service
     */
    public BlogService(XwikiFacadeService xwikiFacadeService, BlogConfiguration config, Localisable<String, String> baseUrl) {
        this.config = config;
        this.xwikiFacadeService = xwikiFacadeService;
        this.baseUrl = baseUrl;
    }

    /**
     * Trigger an asynchronous refresh once the application is ready.
     * The actual load is delegated to {@link #refreshPosts()} and
     * executed in a separate thread to avoid blocking startup.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void loadPostsAsync() {
        CompletableFuture.runAsync(this::refreshPosts);
    }

    /**
     * Refreshes the blog posts by updating from XWiki.
     * <p>
     * This method is scheduled to run every 2 hours and may also be triggered at
     * startup by an asynchronous listener.
     * TODO (p3, conf): Schedule from configuration
     * </p>
     */
    @Scheduled(initialDelay = 1000 * 3600 * 2, fixedDelay = 1000 * 3600 * 2)
    public void refreshPosts() {
        try {
            updateBlogPosts();
        } catch (Exception e) {
            logger.error("Critical error during refreshing posts: {}", e.getMessage(), e);
            // Rethrow to fail fast in a critical situation
            throw new RuntimeException("Failed to refresh blog posts", e);
        }
    }

    /**
     * Retrieves a list of blog posts matching the specified tag (vertical).
     *
     * @param tag the tag to filter blog posts by
     * @return a list of blog posts that contain the specified tag in their category list
     */
    public List<BlogPost> getPosts(String tag) {
        if (StringUtils.isEmpty(tag)) {
            return posts; // return all posts if no tag provided
        }
        return posts.stream()
                .filter(post -> post.getCategory() != null && post.getCategory().contains(tag))
                .collect(Collectors.toList());
    }

    /**
     * Updates the blog posts by fetching data from XWiki.
     * <p>
     * For thread safety, new collections are built locally and then the internal references
     * are replaced atomically with unmodifiable collections.
     * </p>
     */
    public void updateBlogPosts() {
        // Prevent concurrent updates
        if (!loading.compareAndSet(false, true)) {
            logger.warn("Blog posts update is already running");
            return;
        }

        try {
            logger.info("Starting update of blog posts from XWiki");
            Pages pages = xwikiFacadeService.getPages("Blog");
            if (pages == null) {
                logger.error("Received null pages from XWiki service.");
                throw new IllegalStateException("Pages from XWiki are null");
            }

            // Initialize local counters and collections
            int localExpectedCount = pages.getPageSummaries().size();
            int localExceptionsCount = 0;

            List<BlogPost> newPosts = new ArrayList<>();
            Map<String, BlogPost> newPostsByUrl = new HashMap<>();
            Map<String, Integer> newTags = new HashMap<>();

            // Process each page summary from XWiki
            for (PageSummary page : pages.getPageSummaries()) {
                try {
                    // Discard internal XWiki pages.
                    if (page.getFullName().endsWith(".WebHome")) {
                        localExpectedCount--;
                        continue;
                    }

                    // TODO : Default internationalisation
                    FullPage fullPage = xwikiFacadeService.getFullPage(page.getSpace()+":"+ page.getName(),"en");

                    // Evict the default blog page with title "Blog"
                    if ("Blog".equals(fullPage.getProperties().get("title"))) {
                        localExpectedCount--;
                        continue;
                    }

                    // Skip hidden pages
                    if (fullPage.getWikiPage().isHidden()) {
                        localExpectedCount--;
                        continue;
                    }

                    BlogPost post = new BlogPost();
                    post.setWikiPage(fullPage);

                    // Process the image if available
                    String image = fullPage.getProperties().get("image");
                    if (StringUtils.isNotEmpty(image)) {
                        // Use StandardCharsets.UTF_8 for encoding for safety
                        String fullImage = getBlogImageUrl(
                                URLEncoder.encode(page.getName(), StandardCharsets.UTF_8),
                                URLEncoder.encode(image, StandardCharsets.UTF_8));


                        // Translation to .webp format
                        // NOTE : it will be autoamticaly cached and resized with the imageresizer interceptor

                        if (fullImage.endsWith(".png") || fullImage.endsWith(".jpg") || fullImage.endsWith(".jpeg")) {
                        	fullImage = fullImage.substring(0, fullImage.lastIndexOf('.'));
                        	// Must be in allowedImagesSizeSuffixes of app rendering resources
                        	fullImage += "-1000.webp";
                        }

                        post.setImage(fullImage);
                    }

                    // Set the author; remove "XWiki." prefix and capitalize fully
                    String author = fullPage.getWikiPage().getAuthor();
                    if (author != null && author.length() > 6) {
                        post.setAuthor(WordUtils.capitalizeFully(author.substring(6)));
                    } else {
                        post.setAuthor(author);
                    }

                    // Set summary if available
                    String extract = fullPage.getProperties().get("extract");
                    post.setSummary(extract);

                    // Process category: remove "Blog." prefix and split by '|'
                    String category = fullPage.getProperties().get("category");
                    // TODO : Ugly fix to discard article tagged webhome. Should wear a list of exclusions patterns in configuration.
                    if (category != null && !category.toLowerCase().contains("webhome")) {
                        List<String> categories = Arrays.stream(category.replace("Blog.", "").split("\\|"))
                                .filter(StringUtils::isNotEmpty)
                                .collect(Collectors.toList());
                        post.setCategory(categories);
                    } else {
                        post.setCategory(Collections.emptyList());
                    }

                    // Parse publish date using blogDateFormatter
                    String publishDate = fullPage.getProperties().get("publishDate");
                    if (publishDate != null) {
                        ZonedDateTime zonedDateTime = ZonedDateTime.parse(publishDate, blogDateFormatter);
                        post.setCreated(new Date(zonedDateTime.toInstant().toEpochMilli()));
                    }

                    // Set last modification date
                    if (fullPage.getWikiPage().getModified() != null) {
                        post.setModified(new Date(fullPage.getWikiPage().getModified().getTimeInMillis()));
                    }

                    // Set post title and derive URL from it
                    String title = fullPage.getProperties().get("title");
                    post.setTitle(title);
                    post.setUrl(IdHelper.normalizeFileName(title));

                    // Set the edit link using xwikiFacadeService's path helper
                    String[] pageIdParts = page.getId().replace("xwiki:", "").split("\\.");
                    post.setEditLink(xwikiFacadeService.getPathHelper().getEditpath(pageIdParts));

                    // Process HTML content to extract the blog post body
                    String html = fullPage.getHtmlContent();
                    int startPos = html.indexOf(XWIKI_BLOGPOST_START_MARKUP);
                    if (startPos != -1) {
                        html = html.substring(startPos);
                    }

                    int stopPos = html.indexOf(XWIKI_BLOGPOST_STOP_MARKUP);
                    if (stopPos != -1) {
                        html = html.substring(0, stopPos);
                    }

                    // Replace blog attachment links with proxied equivalent links
                    String basePath = config.getStaticDomain()+ '/' + config.getBlogUrl();
                    if (basePath.endsWith("/")) {
                        basePath = basePath.substring(0, basePath.length() - 1);
                    }
                    html = html.replace("\"/bin/download/Blog", "\"" + basePath);

                    // Translation to .webp format
                    // NOTE : it will be autoamticaly cached and resized with the imageresizer interceptor
                    html = html.replace(".png", ".webp");
                    html = html.replace(".jpg", ".webp");
                    html = html.replace(".jpeg", ".webp");

                    post.setBody(html);

                    // Generate a summary if not provided and the body is sufficiently long
                    if (post.getBody() != null && post.getSummary() == null && post.getBody().length() > 100) {
                        // TODO (p3,conf): Truncation length should be configurable
                        post.setSummary(post.getBody().substring(0, 100) + " ...");
                    }

                    newPosts.add(post);
                } catch (Exception ex) {
                    localExceptionsCount++;
                    // Log detailed error information for the failing page.
                    logger.error("Error processing blog post for page '{}': {}", page.getFullName(), ex.getMessage(), ex);
                    // Optionally, rethrow severe exceptions if needed. For now, we continue processing.
                }
            }

            // Sort posts by creation date in descending order.
            newPosts.sort((o1, o2) -> o2.getCreated().compareTo(o1.getCreated()));

            // Build postsByUrl map from the new posts.
            for (BlogPost post : newPosts) {
                newPostsByUrl.put(post.getUrl(), post);
            }

            // Build tags map by counting tag occurrences in each post.
            for (BlogPost post : newPosts) {
                List<String> categories = post.getCategory();
                for (String tag : categories) {
                    newTags.merge(tag, 1, Integer::sum);
                }
            }

            // Atomically replace the internal collections with unmodifiable versions for thread safety.
            this.posts = Collections.unmodifiableList(newPosts);
            this.postsByUrl = Collections.unmodifiableMap(newPostsByUrl);
            this.tags = Collections.unmodifiableMap(newTags);
            this.expectedBlogPagesCount = localExpectedCount;
            this.exceptionsCount = localExceptionsCount;

            // If any exceptions were encountered during processing, rethrow an exception to notify the scheduler.
            if (localExceptionsCount > 0) {
                throw new IllegalStateException("Encountered " + localExceptionsCount + " errors during blog posts update");
            }

            logger.info("Blog posts updated successfully. Total posts: {}", newPosts.size());
        } catch (Exception e) {

        	// TODO : Handle healthchecks / metrics status
        	logger.error("Error while updating blog posts !",e);
		}


        finally {
            // Ensure the loading flag is reset even if exceptions occur.
            loading.set(false);
        }
    }

    /**
     * Generates an RSS feed for the blog posts in the specified language.
     *
     * @param lang the language code for i18n fields in the feed
     * @return the RSS feed as a String
     * @throws FeedException if there is an error during feed generation
     */
    public String rss(String lang) throws FeedException {
        logger.info("Generating RSS feed");
        SyndFeed feed = new SyndFeedImpl();
        feed.setEncoding("UTF-8");
        feed.setFeedType(config.getFeedType());
        feed.setTitle(config.getFeedTitle().i18n(lang));
        feed.setDescription(config.getFeedDescription().i18n(lang));
        feed.setLink(baseUrl.i18n(lang));

        List<SyndEntry> entries = new ArrayList<>();
        // Iterate over posts in a thread-safe manner.
        for (BlogPost post : postsByUrl.values()) {
            SyndEntry entry = new SyndEntryImpl();
            entry.setTitle(post.getTitle());
            entry.setLink(baseUrl.i18n(lang) + config.getBlogUrl() + post.getUrl());

            SyndContent content = new SyndContentImpl();
            content.setType("text/html");
            content.setValue(post.getSummary());
            entry.setDescription(content);

            entry.setPublishedDate(post.getCreated());
            entry.setUpdatedDate(post.getModified());
            entry.setAuthor(post.getAuthor());

            List<SyndCategory> categories = post.getCategory().stream().map(cat -> {
                SyndCategory category = new SyndCategoryImpl();
                category.setName(cat);
                return category;
            }).collect(Collectors.toList());
            entry.setCategories(categories);

            entries.add(entry);
        }
        feed.setEntries(entries);

        SyndFeedOutput output = new SyndFeedOutput();
        return output.outputString(feed, true);
    }

    /**
     * Constructs a proxified blog image URL for the given name and image parameters.
     *
     * @param name  the blog post name, URL-encoded
     * @param image the image file name, URL-encoded
     * @return the proxified URL for the blog image
     */
    private String getBlogImageUrl(String name, String image) {
        String basePath = '/' + config.getBlogUrl();
        if (basePath.endsWith("/")) {
            basePath = basePath.substring(0, basePath.length() - 1);
        }

        // Adding domain prefix if set
        if (null != config.getStaticDomain()) {
        	basePath = config.getStaticDomain() + basePath;
        }

        return basePath + '/' + name + '/' + image;
    }

    /**
     * Provides a custom health indicator for the blog service.
     * <p>
     * The health is considered down if posts are currently loading, if no posts are loaded,
     * if exceptions occurred during update, or if the expected number of posts does not match.
     * </p>
     *
     * @return a Health object representing the current health state
     */
    @Override
    public Health health() {
        if (loading.get()) {
            return Health.up().withDetail("loading", true).build();
        }

        Builder healthBuilder = Health.up()
                .withDetail("posts_count", posts.size())
                .withDetail("exceptions_count", exceptionsCount);

        // Set health down if issues are detected.
        if (posts.isEmpty() || exceptionsCount > 0 || expectedBlogPagesCount != posts.size()) {
            healthBuilder = healthBuilder.down();
            if (expectedBlogPagesCount != posts.size()) {
                healthBuilder.withDetail("invalid_expected_posts_count", "Expected " + expectedBlogPagesCount + " but got " + posts.size());
            }
        }
        return healthBuilder.build();
    }

    ///////////////////////////////
    // Getters and Setters
    ///////////////////////////////

    /**
     * Retrieves the map of blog posts by their URL.
     *
     * @return an unmodifiable map of blog posts keyed by URL
     */
    public Map<String, BlogPost> getPostsByUrl() {
        return postsByUrl;
    }

    /**
     * Sets the map of blog posts by URL.
     *
     * @param postsByUrl the new map of blog posts by URL
     */
    public void setPostsByUrl(Map<String, BlogPost> postsByUrl) {
        this.postsByUrl = postsByUrl;
    }

    /**
     * Retrieves the list of blog posts.
     *
     * @return an unmodifiable list of blog posts
     */
    public List<BlogPost> getPosts() {
        return posts;
    }

    /**
     * Sets the list of blog posts.
     *
     * @param posts the new list of blog posts
     */
    public void setPosts(List<BlogPost> posts) {
        this.posts = posts;
    }

    /**
     * Retrieves the tags map with counts of posts per tag.
     *
     * @return an unmodifiable map of tags to post counts
     */
    public Map<String, Integer> getTags() {
        return tags;
    }

    /**
     * Sets the tags map.
     *
     * @param tags the new tags map
     */
    public void setTags(Map<String, Integer> tags) {
        this.tags = tags;
    }
}
