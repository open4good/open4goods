package org.open4goods.ui.services;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.io.FileUtils;
import org.open4goods.config.yml.ui.ProductI18nElements;
import org.open4goods.config.yml.ui.VerticalConfig;
import org.open4goods.dao.ProductRepository;
import org.open4goods.helper.GenericFileLogger;
import org.open4goods.model.blog.BlogPost;
import org.open4goods.services.VerticalsConfigService;
import org.open4goods.ui.config.yml.UiConfig;
import org.open4goods.xwiki.model.FullPage;
import org.open4goods.xwiki.services.XwikiFacadeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import com.redfin.sitemapgenerator.ChangeFreq;
import com.redfin.sitemapgenerator.WebSitemapGenerator;
import com.redfin.sitemapgenerator.WebSitemapUrl;
import com.redfin.sitemapgenerator.WebSitemapUrl.Options;

import ch.qos.logback.classic.Level;

/**
 * Service in charge of generating the localised sitemaps
 *
 * @author Goulven.Furet
 *
 */

public class SitemapGenerationService {

	private static final Logger LOGGER = LoggerFactory.getLogger(SitemapGenerationService.class);

	private ProductRepository aggregatedDataRepository;
	private UiConfig uiConfig;

	// The flag that indicates wether opendata export is running or not
	private AtomicBoolean exportRunning = new AtomicBoolean(false);

	private final Map<String, WebSitemapGenerator> siteGens = new HashMap<>();

	private Logger statsLogger;

	private VerticalsConfigService verticalsConfigService;
	private BlogService blogService;
	// TODO : To allow last mod date and check existence
	private final XwikiFacadeService xwikiService;

	public SitemapGenerationService(ProductRepository aggregatedDataRepository, UiConfig uiConfig, VerticalsConfigService verticalConfigService,  BlogService blogService, XwikiFacadeService xwikiService) {
		this.aggregatedDataRepository = aggregatedDataRepository;
		this.uiConfig = uiConfig;
		this.verticalsConfigService = verticalConfigService;
		this.blogService = blogService;
		this.statsLogger = GenericFileLogger.initLogger("stats-sitemap", Level.INFO, uiConfig.logsFolder(), false);
		this.xwikiService = xwikiService;
	}

	/**
	 * IBuild the sitemap
	 *
	 * TODO : Schedule from conf
	 */
	@Scheduled(initialDelay = 1000L * 3600, fixedDelay = 1000L * 3600 * 24 * 7)
	public void generate() {

		if (exportRunning.get()) {
			LOGGER.error("Sitemap generation is already running");
		} else {
			exportRunning.set(true);
		}

		LOGGER.info("Starting sitemap generation");

		statsLogger.info("Starting sitemap generation");

		// Sitemap files initialisation 
		initSitemapsFiles();
		
		///////////////////////////////////
		// Adding pages, per languages
		///////////////////////////////////
		siteGens.entrySet().stream().forEach(e-> {
			
			String lang = e.getKey();
			WebSitemapGenerator sitemap = siteGens.get(lang);
						
			// Adding home page
			String baseUrl = uiConfig.getNamings().getBaseUrls().get(lang) ;
			addUrl(sitemap, baseUrl, ChangeFreq.MONTHLY, 1.0);
			
			// Adding contoller based pages
			// TODO
			
			// Adding blog posts
			addBlogPost(blogService.getPosts(), sitemap, baseUrl );
			
			// Adding wiki pages
			
			
			
			uiConfig.getWikiPagesMapping().entrySet().stream().forEach(entry -> {
				// 
				addWikiPage(entry.getKey(),entry.getValue().i18n(entry.getKey()), sitemap, baseUrl);
				
				
				
			});
			
			// Adding vertical relativ material
			verticalsConfigService.getConfigsWithoutDefault().forEach(v -> {
				addVerticalPages(v, v.i18n(lang),sitemap, baseUrl);
			});
			
			// Adding products
			// TODO
			
		});
		
		AtomicLong totalItems= new AtomicLong(0);
//		// Processing each data
//		aggregatedDataRepository.exportAllHavingPrices()

//		.filter(e -> e.getOffersCount() > 1)
//		.forEach(e -> {
//			onAggregatedData(e);
//			totalItems.incrementAndGet();
//		});

		// Closing / flushing files
		terminate();

		statsLogger.info("Sitemap generated with {} items", totalItems.get());

		exportRunning.set(false);

	}


	/**
	 * Init the simaps files
	 */
	public void initSitemapsFiles() {

		LOGGER.info("Starting site maps generation");

		// For each internationalized website
		for (final Entry<String, String> urls : uiConfig.getNamings().getServerNames().entrySet()) {
			File folder = uiConfig.siteMapFolder();
			folder = new File(folder.getAbsolutePath() + "/" + urls.getKey());
			LOGGER.info("Creating folder {}", folder.getAbsolutePath());
			try {
				FileUtils.deleteDirectory(folder);
			} catch (final IOException e1) {
				LOGGER.error("Error while deleting old sitemap");
			}

			folder.mkdirs();

			try {
				siteGens.put(urls.getKey(), new WebSitemapGenerator(uiConfig.getBaseUrl(urls.getKey()) + "sitemap/" + urls.getKey() + "/", folder));
			} catch (final MalformedURLException e) {
				LOGGER.warn("Invalid url in sitemap : {}", e.getMessage());
			}

		}

	}

	/**
	 * Add conf defined wiki pages to sitemap
	 * @param list
	 * @param sitemap
	 * @param baseUrl 
	 */
	private void addWikiPage(String wikiPath, String nudgerPath, WebSitemapGenerator sitemap, String baseUrl) {
				
		FullPage page = null;
		try {
			page = xwikiService.getFullPage(wikiPath);
		} catch (Exception e) {
			LOGGER.error("Error while retrieving wiki page  {} : {}",wikiPath,e.getMessage() ,e);
		}
		if (null == page) {
			return;
		}
		
		String u =  baseUrl+nudgerPath;
		LOGGER.info("Adding wiki page to sitemap : {}",u);

		addUrl(sitemap, u, ChangeFreq.MONTHLY, 0.7, page.getWikiPage().getModified().getTime());			
	}
	
	/**
	 * Add blog posts to a sitemap
	 * @param list
	 * @param sitemap
	 * @param baseUrl 
	 */
	private void addBlogPost(List<BlogPost> posts, WebSitemapGenerator sitemap, String baseUrl) {
		posts.forEach(post -> {			
			// TODO : Blog is immutable (not translated). At last have it in conf / constants
			String url = baseUrl+"blog/"+  post.getUrl();
			LOGGER.info("Adding blog entry to sitemap : {}",url);

			addUrl(sitemap, url , ChangeFreq.YEARLY, 0.6, Date.from(Instant.ofEpochMilli(post.getCreatedMs())));			
		});		
	}
	
	/**
	 * Add specific vertical page
	 * @param v
	 * @param productI18nElements
	 * @param sitemap
	 * @param baseUrl 
	 */
	private void addVerticalPages(VerticalConfig v, ProductI18nElements i18n, WebSitemapGenerator sitemap, String baseUrl) {
		//  >> Vertical home page
		addUrl(sitemap, baseUrl+ i18n.getVerticalHomeUrl(), ChangeFreq.WEEKLY, 0.9);
				
		//  >> Vertical specific pages
		i18n.getWikiPages().forEach(e-> {
			
			FullPage page = null;
			try {
				page = xwikiService.getFullPage(e.getWikiUrl());
				
				String url = baseUrl+ i18n.getVerticalHomeUrl()+"/" + e.getVerticalUrl();
				LOGGER.info("Adding to sitemap : {}",url);
				addUrl(sitemap, url , ChangeFreq.MONTHLY, 0.9  );
			} catch (Exception ex) {
				LOGGER.error("Error while retrieving wiki page  {} : {}",e.getWikiUrl(),ex.getMessage());
			}
			if (null == page) {
				return;
			}
		});
		
		
		//  

		
	}
	

//	/**
//	 * Convert an aggregated data pageSize a sitemap entry
//	 *
//	 * @param data
//	 */
//	public void onAggregatedData(final Product data) {
//
//		for (final Entry<String, String> urls : uiConfig.getNamings().getServerNames().entrySet()) {
//			try {
//
//				//
////				final String u = uiConfig.getBaseUrl(urls.getKey()) + data.getUrls().getUrls().get(urls.getKey());
//				final String u = uiConfig.getBaseUrl(urls.getKey()) + data.getNames().getName();
//
//				// Getting the last modified date
//				Date now = null;
//				if (null != data.getLastChange()) {
//					now = Date.from(Instant.ofEpochMilli(data.getLastChange()));
//				}
//
//				// Adding page
//				addUrl(siteGens.get(urls.getKey()), u, ChangeFreq.WEEKLY, 0.9, now);
//
//			} catch (final Exception e) {
//				LOGGER.warn("Error while adding url in sitemap : {}", e.getMessage());
//			}
//		}
//	}

	/**
	 * Add an url pageSize the sitemap
	 *
	 * @param sitemap
	 * @param url
	 * @param changeFreq
	 * @param priority
	 * @param lastMod
	 */
	private void addUrl(final WebSitemapGenerator sitemap, final String url, final ChangeFreq changeFreq, final double priority, final Date lastMod) {
		try {
			final Options options = new Options(url);
			options.changeFreq(changeFreq);
			if (null != lastMod) {
				options.lastMod(lastMod);
			}
			options.priority(priority);
			sitemap.addUrl(new WebSitemapUrl(options));
		} catch (final Exception e) {
			LOGGER.error("Error while adding {} pageSize sitemap : {}", url, e.getMessage());
		}

	}

	private void addUrl(final WebSitemapGenerator sg, final String string, final ChangeFreq freq, final double d) {
		addUrl(sg, string, freq, d, null);
	}
	
	public void terminate() {

		LOGGER.info("Ending site map generation");
		for (final WebSitemapGenerator w : siteGens.values()) {
			w.write();
			w.writeSitemapsWithIndex();
		}
	}
	

}
