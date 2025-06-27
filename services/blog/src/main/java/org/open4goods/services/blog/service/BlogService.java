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
import org.open4goods.services.blog.model.BlogPost;
import org.open4goods.xwiki.model.FullPage;
import org.open4goods.xwiki.services.XwikiFacadeService;
import org.open4goods.xwiki.XWikiServiceConfiguration;
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

import jakarta.annotation.PostConstruct;

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

    // Index of posts keyed by slug -> title. Only slugs are kept to avoid
    // fetching full pages during the scheduled refresh.
    private volatile Map<String, String> postIndex = Collections.emptyMap();

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
     * Refreshes the blog posts by updating from XWiki.
     * <p>
     * This method is scheduled to run every 2 hours and is also invoked on application startup.
     * TODO (p3, conf): Schedule from configuration
     * </p>
     */
    @PostConstruct
    @Scheduled(initialDelay = 0, fixedDelay = 1000 * 3600 * 2)
    public void refreshPosts() {
        try {
            updatePostIndex();
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
        return postIndex.keySet().stream()
                .map(this::getPost)
                .filter(post -> post != null && (StringUtils.isEmpty(tag)
                        || (post.getCategory() != null && post.getCategory().contains(tag))))
                .toList();
    }

    /**
     * Retrieve a single blog post using its slug.
     *
     * @param slug the XWiki page name
     * @return the BlogPost or {@code null} if retrieval fails
     */
    @org.springframework.cache.annotation.Cacheable(cacheNames = XWikiServiceConfiguration.ONE_HOUR_LOCAL_CACHE_NAME,
            key = "'blog:' + #slug")
    public BlogPost getPost(String slug) {
        try {
            FullPage fullPage = xwikiFacadeService.getFullPage("Blog", slug);
            if (fullPage == null) {
                return null;
            }

            // Evict the default blog page with title "Blog"
            if ("Blog".equals(fullPage.getProperties().get("title"))) {
                return null;
            }

            // Skip hidden pages
            if (fullPage.getWikiPage().isHidden()) {
                return null;
            }

            BlogPost post = new BlogPost();
            post.setWikiPage(fullPage);

            String image = fullPage.getProperties().get("image");
            if (StringUtils.isNotEmpty(image)) {
                String fullImage = getBlogImageUrl(
                        URLEncoder.encode(slug, StandardCharsets.UTF_8),
                        URLEncoder.encode(image, StandardCharsets.UTF_8));
                post.setImage(fullImage);
            }

            String author = fullPage.getWikiPage().getAuthor();
            if (author != null && author.length() > 6) {
                post.setAuthor(WordUtils.capitalizeFully(author.substring(6)));
            } else {
                post.setAuthor(author);
            }

            String extract = fullPage.getProperties().get("extract");
            post.setSummary(extract);

            String category = fullPage.getProperties().get("category");
            if (category != null) {
                List<String> categories = Arrays.stream(category.replace("Blog.", "").split("\\|")).filter(StringUtils::isNotEmpty).toList();
                post.setCategory(categories);
            } else {
                post.setCategory(Collections.emptyList());
            }

            String publishDate = fullPage.getProperties().get("publishDate");
            if (publishDate != null) {
                ZonedDateTime zonedDateTime = ZonedDateTime.parse(publishDate, blogDateFormatter);
                post.setCreated(new Date(zonedDateTime.toInstant().toEpochMilli()));
            }

            if (fullPage.getWikiPage().getModified() != null) {
                post.setModified(new Date(fullPage.getWikiPage().getModified().getTimeInMillis()));
            }

            String title = fullPage.getProperties().get("title");
            post.setTitle(title);
            post.setUrl(slug);

            String[] pageIdParts = fullPage.getWikiPage().getId().replace("xwiki:", "").split("\\.");
            post.setEditLink(xwikiFacadeService.getPathHelper().getEditpath(pageIdParts));

            String html = fullPage.getHtmlContent();
            int startPos = html.indexOf(XWIKI_BLOGPOST_START_MARKUP);
            if (startPos != -1) {
                html = html.substring(startPos);
            }
            int stopPos = html.indexOf(XWIKI_BLOGPOST_STOP_MARKUP);
            if (stopPos != -1) {
                html = html.substring(0, stopPos);
            }
            html = Jsoup.clean(html, Safelist.basicWithImages());
            String basePath = '/' + config.getBlogUrl();
            if (basePath.endsWith("/")) {
                basePath = basePath.substring(0, basePath.length() - 1);
            }
            html = html.replace("\"/bin/download/Blog", "\"" + basePath);
            post.setBody(html);

            if (post.getBody() != null && post.getSummary() == null && post.getBody().length() > 100) {
                post.setSummary(post.getBody().substring(0, 100) + " ...");
            }

            return post;
        } catch (Exception e) {
            logger.error("Error while retrieving blog post {}", slug, e);
            return null;
        }
    }

    /**
     * Updates the blog post index without fetching full pages.
     */
    public void updatePostIndex() {
        if (!loading.compareAndSet(false, true)) {
            logger.warn("Blog posts update is already running");
            return;
        }

        try {
            logger.info("Updating blog post index from XWiki");
            Pages pages = xwikiFacadeService.getPages("Blog");
            if (pages == null) {
                logger.error("Received null pages from XWiki service.");
                throw new IllegalStateException("Pages from XWiki are null");
            }

            Map<String, String> newIndex = new LinkedHashMap<>();
            int localExpectedCount = 0;

            for (PageSummary page : pages.getPageSummaries()) {
                if (page.getFullName().endsWith(".WebHome")) {
                    continue;
                }
                newIndex.put(page.getName(), page.getTitle());
                localExpectedCount++;
            }

            this.postIndex = Collections.unmodifiableMap(newIndex);
            this.expectedBlogPagesCount = localExpectedCount;
            this.exceptionsCount = 0;

            logger.info("Blog index updated successfully. Total posts: {}", newIndex.size());
        } catch (Exception e) {
            logger.error("Error while updating blog index", e);
        } finally {
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
        for (String slug : postIndex.keySet()) {
            BlogPost post = getPost(slug);
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
        Builder healthBuilder;
        if (loading.get()) {
            healthBuilder = Health.up().withDetail("loading", true);
        } else {
            healthBuilder = Health.up()
                    .withDetail("posts_count", posts.size())
                    .withDetail("exceptions_count", exceptionsCount);
        }

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
    // Getters
    ///////////////////////////////

    /**
     * Current post index mapping slug to title.
     */
    public Map<String, String> getPostIndex() {
        return postIndex;
    }

    /**
     * Compute tags counts lazily from loaded posts.
     */
    public Map<String, Integer> getTags() {
        Map<String, Integer> counts = new HashMap<>();
        for (String slug : postIndex.keySet()) {
            BlogPost post = getPost(slug);
            if (post.getCategory() != null) {
                for (String cat : post.getCategory()) {
                    counts.merge(cat, 1, Integer::sum);
                }
            }
        }
        return counts;
    }
}
