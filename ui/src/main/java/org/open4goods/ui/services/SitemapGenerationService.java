package org.open4goods.ui.services;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

import org.open4goods.config.yml.WikiPageConfig;
import org.open4goods.config.yml.ui.ProductI18nElements;
import org.open4goods.config.yml.ui.VerticalConfig;
import org.open4goods.dao.ProductRepository;
import org.open4goods.helper.GenericFileLogger;
import org.open4goods.model.Localisable;
import org.open4goods.model.blog.BlogPost;
import org.open4goods.model.dto.WikiPage;
import org.open4goods.model.product.Product;
import org.open4goods.services.VerticalsConfigService;
import org.open4goods.ui.config.yml.UiConfig;
import org.open4goods.ui.controllers.ui.pages.SitemapEntry;
import org.open4goods.ui.controllers.ui.pages.SitemapExposedController;
import org.open4goods.xwiki.model.FullPage;
import org.open4goods.xwiki.services.XwikiFacadeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;

import ch.qos.logback.classic.Level;
import cz.jiripinkas.jsitemapgenerator.ChangeFreq;
import cz.jiripinkas.jsitemapgenerator.WebPage;
import cz.jiripinkas.jsitemapgenerator.generator.SitemapGenerator;
import cz.jiripinkas.jsitemapgenerator.generator.SitemapIndexGenerator;

/**
 * Service in charge of generating the localised sitemaps
 *
 * @author Goulven.Furet
 *
 */

public class SitemapGenerationService {

	private static final Logger LOGGER = LoggerFactory.getLogger(SitemapGenerationService.class);

	private static final String SITEMAP_NAME_ROOT_PAGES = "main-pages.xml";
	private static final String SITEMAP_NAME_PRODUCT_PAGES = "product-pages.xml";
	private static final String SITEMAP_NAME_BLOG_PAGES = "blog-pages.xml";
	private static final String SITEMAP_NAME_WIKI_PAGES = "wiki-pages.xml";
	private static final String SITEMAP_NAME_VERTICAL_PAGES = "verticals-pages.xml";	
	
	private ProductRepository aggregatedDataRepository;
	private UiConfig uiConfig;

	// The flag that indicates wether opendata export is running or not
	private AtomicBoolean exportRunning = new AtomicBoolean(false);


	private Logger statsLogger;

	private VerticalsConfigService verticalsConfigService;
	private BlogService blogService;
	// TODO : To allow last mod date and check existence
	private final XwikiFacadeService xwikiService;
	
	private ApplicationContext context;

	private Map<String, SitemapExposedController> annotatedControllers;
	
	public SitemapGenerationService(ProductRepository aggregatedDataRepository, UiConfig uiConfig, VerticalsConfigService verticalConfigService,  BlogService blogService, XwikiFacadeService xwikiService, ApplicationContext context) {
		this.aggregatedDataRepository = aggregatedDataRepository;
		this.verticalsConfigService = verticalConfigService;
		this.blogService = blogService;
		this.statsLogger = GenericFileLogger.initLogger("stats-sitemap", Level.INFO, uiConfig.logsFolder(), false);
		this.xwikiService = xwikiService;
		this.uiConfig = uiConfig;
		this.annotatedControllers =  context.getBeansOfType(SitemapExposedController.class);
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

		///////////////////////////////////
		// Adding pages, per languages
		///////////////////////////////////
		uiConfig.getNamings().getBaseUrls().entrySet() .forEach(e-> {
			
			String lang = e.getKey();
			
			String baseUrl = uiConfig.getNamings().getBaseUrls().get(lang) ;
			
			// Adding contoller based pages
			addControllerPages(baseUrl, lang);
			
			
			// Adding blog posts
			addBlogPost(blogService.getPosts(),  baseUrl, lang );
			
			// Adding wiki pages			
			addWikiPages( baseUrl, lang);
				
			// Adding products
//			addProductsPages(baseUrl, lang);
			
			// Adding vertical relativ material
			addVerticalPages( baseUrl, lang);
			// Adding products
			
			// Generating the sitemap index
				
			SitemapIndexGenerator index = SitemapIndexGenerator.of(baseUrl+"sitemap/" );
			index = index
				.addPage(SITEMAP_NAME_ROOT_PAGES)
				.addPage(SITEMAP_NAME_BLOG_PAGES)
				.addPage(SITEMAP_NAME_WIKI_PAGES)
				.addPage(SITEMAP_NAME_VERTICAL_PAGES)
				.addPage(SITEMAP_NAME_PRODUCT_PAGES);
				
				try {
					index.toFile(getSitemapFile("sitemap.xml", lang));
				} catch (IOException e1) {
					LOGGER.error("Error while generating sitemap index",e1);
				}
		});
		
		statsLogger.info("Sitemap generated ");

		exportRunning.set(false);
	}


	/**
	 * Generate a sitemap in a language for controller exposed pages
	 * @param sitemap
	 * @param baseUrl	
	 */
	private void addControllerPages(String baseUrl, String language) {
		 
		SitemapGenerator sitemap = SitemapGenerator.of(baseUrl);
		
		// Adding the home page
		sitemap = sitemap.addPage(getWebPage("/", ChangeFreq.WEEKLY, 1.0));
		
		for (Entry<String, SitemapExposedController> e : annotatedControllers.entrySet()) {
			
			try {
				for (SitemapEntry value : e.getValue().getMultipleExposedUrls())
				{					
					// .substrint : remove the //
					String url =  baseUrl+value.i18n(language).substring(1);
					LOGGER.info("Adding controller page to {} sitemap : {}",language, url);
					
					sitemap = sitemap.addPage(getWebPage(url, value.getFrequency(), value.getPriority()));
				}
			} catch (Exception e1) {
				LOGGER.error("Cannot add controller to sitemap : {}",e,e1);
			}
		}

		// Writing sitemap
		try {
			sitemap.toFile(getSitemapFile(SITEMAP_NAME_ROOT_PAGES, language));
		} catch (IOException e1) {
			LOGGER.error("Error while writing controller sitemap",e1);
		}
	}

	


	/**
	 * Add valid products with (ecoscore, activOffers and genAi completed) 
	 * @param vertical
	 * @param sitemap
	 * @param baseUrl
	 * @param language
	 */
	private void addProductsPages( String baseUrl, String language) {	
		SitemapGenerator sitemap = SitemapGenerator.of(baseUrl);
		
		for (VerticalConfig vertical : verticalsConfigService.getConfigsWithoutDefault()) {
			List<Product> datas = aggregatedDataRepository.exportVerticalWithValidDateOrderByEcoscore(vertical.getId())
			// Filtering on products having genAI content
					.filter(e -> null != e.getAiDescriptions())
					// TODO : Not really filtered per language
					.filter(e -> e.getAiDescriptions().size() > 1)
					
			.toList();
			
			for (Product data : datas) {
				String url = data.url(language);
				LOGGER.info("Adding product page to sitemap : {}",url);
				sitemap = sitemap.addPage(getWebPage(url, ChangeFreq.DAILY, 1.0, Date.from(Instant.ofEpochMilli(data.getLastChange()))));			
			}
		}
		
		// Writing sitemap
		try {
			sitemap.toFile(getSitemapFile(SITEMAP_NAME_PRODUCT_PAGES, language));
		} catch (IOException e1) {
			LOGGER.error("Error while writing product sitemap",e1);
		}
		
	}

	

	/**
	 * Add conf defined wiki pages to sitemap
	 * @param list
	 * @param sitemap
	 * @param baseUrl 
	 * @param language 
	 */
	private void addWikiPages( String baseUrl, String language) {
				
		SitemapGenerator sitemap = SitemapGenerator.of(baseUrl);
		
		for (Entry<String, Localisable> entry : uiConfig.getWikiPagesMapping().entrySet()) {
			FullPage page = null;
			try {
				page = xwikiService.getFullPage(entry.getKey());
			} catch (Exception e) {
				LOGGER.error("Error while retrieving wiki page  {} : {}",entry.getValue(),e.getMessage() ,e);
			}
			if (null == page) {
				return;
			}
			
			String u =  baseUrl+entry.getValue().i18n(language);
			LOGGER.info("Adding wiki page to sitemap : {}",u);
			sitemap = sitemap.addPage(getWebPage(u, ChangeFreq.MONTHLY, 0.8, page.getWikiPage().getModified().getTime()));
			
			
		}
		
		// Writing sitemap
		try {
			sitemap.toFile(getSitemapFile(SITEMAP_NAME_WIKI_PAGES, language));
		} catch (IOException e) {
			LOGGER.error("Error while writing wiki sitemap",e);
		}
	}
	
	/**
	 * Add blog posts to a sitemap
	 * @param list
	 * @param sitemap
	 * @param baseUrl 
	 */
	private void addBlogPost(List<BlogPost> posts, String baseUrl, String language) {
		SitemapGenerator sitemap = SitemapGenerator.of(baseUrl);
		for (BlogPost post : posts) {
				// TODO : Blog is immutable (not translated). At last have it in conf / constants
				String url = baseUrl+"blog/"+  post.getUrl();
				LOGGER.info("Adding blog entry to sitemap : {}",url);
				sitemap = sitemap.addPage(getWebPage(url, ChangeFreq.MONTHLY, 0.8, Date.from(Instant.ofEpochMilli(post.getCreatedMs()))));
		}
		// Writing sitemap
		try {
			sitemap.toFile(getSitemapFile(SITEMAP_NAME_BLOG_PAGES, language));
		} catch (IOException e) {
			LOGGER.error("Error while writing blog sitemap",e);
		}
	}
	
	/**
	 * Add specific vertical page
	 * @param v
	 * @param productI18nElements
	 * @param sitemap
	 * @param baseUrl 
	 * @param language 
	 * @throws IOException 
	 */
	private void addVerticalPages( String baseUrl, String language){
		
		SitemapGenerator sitemap = SitemapGenerator.of(baseUrl);
		
		for (VerticalConfig v : verticalsConfigService.getConfigsWithoutDefault() )
		{

			ProductI18nElements i18n = v.i18n(language) ;
			sitemap = sitemap.addPage(getWebPage( baseUrl+ i18n.getVerticalHomeUrl(), ChangeFreq.WEEKLY, 1.0));
			//  >> Vertical home page
				
			
			//  >> Vertical specific pages
			for (WikiPageConfig e : i18n.getWikiPages()) {
				FullPage page = null;
				try {
					page = xwikiService.getFullPage(e.getWikiUrl());
					
					String url = baseUrl+ i18n.getVerticalHomeUrl()+"/" + e.getVerticalUrl();
					LOGGER.info("Adding to sitemap : {}",url);
					sitemap = sitemap.addPage(getWebPage(url, ChangeFreq.MONTHLY, 0.9 ));
				} catch (Exception ex) {
					LOGGER.error("Error while retrieving wiki page  {} : {}",e.getWikiUrl(),ex.getMessage());
				}
				if (null == page) {
					return;
				}
			}
		}
		
		// Writing sitemap
		try {
			sitemap.toFile(getSitemapFile(SITEMAP_NAME_VERTICAL_PAGES, language));
		} catch (IOException e) {
			LOGGER.error("Error while writing vertical sitemap",e);
		}
	}
	

	/**
	 * Get a site map file
	 * @param name
	 * @param language
	 * @return
	 */
	public File getSitemapFile(String name, String language) {
		try {
			Files.createDirectories(Path.of(uiConfig.siteMapFolder().getAbsolutePath()+"/" + language + "/" ));
		} catch (IOException e) {
			LOGGER.error("Error while creating sitemap folder",e);
		}
		return new File(uiConfig.siteMapFolder().getAbsolutePath()+"/" + language + "/" + name);
	}

	/**
	 * Helper method to get a webpage instance
	 * @param path
	 * @param changeFreq
	 * @param priority
	 * @return
	 */
	private WebPage getWebPage(String path, ChangeFreq changeFreq, double priority) {
		return getWebPage(path, changeFreq, priority,null);
	}

	/**
	 * Helper method to get a webpage instance
	 * @param path
	 * @param changeFreq
	 * @param priority
	 * @return
	 */
	private WebPage getWebPage(String path, ChangeFreq changeFreq, double priority, Date lastMod ) {
		
		WebPage w = WebPage.of(path);
		w.setChangeFreq(changeFreq);
		w.setPriority(priority);
		if (null != lastMod) { 
			w.setLastMod(lastMod);
		}
		return w;
	}
	
	
}
