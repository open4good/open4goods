package org.open4goods.ui.services;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.open4goods.commons.helper.GenericFileLogger;
import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.ProductI18nElements;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.model.vertical.VerticalSubCategory;
import org.open4goods.model.vertical.WikiPageConfig;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.ui.config.yml.UiConfig;
import org.open4goods.verticals.VerticalsConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import ch.qos.logback.classic.Level;
import cz.jiripinkas.jsitemapgenerator.ChangeFreq;
import cz.jiripinkas.jsitemapgenerator.WebPage;
import cz.jiripinkas.jsitemapgenerator.generator.SitemapGenerator;
import cz.jiripinkas.jsitemapgenerator.generator.SitemapIndexGenerator;

/**
 * Generates localised XML sitemaps for nudger.fr.
 *
 * <p>One sitemap file is produced per language and per content type:
 * <ul>
 *   <li><b>category-pages.xml</b> – vertical landing pages, sub-category pages
 *       (including auto-generated sub-categories from
 *       {@code classpath*:categories/<vertical>/*.yml}), and vertical-specific
 *       editorial pages.</li>
 *   <li><b>product-pages.xml</b> – individual product pages.</li>
 * </ul>
 * A sitemap index file ({@code sitemap.xml}) is produced alongside each set.</p>
 *
 * <p>Generation runs on a weekly schedule with a one-hour initial delay.
 * Concurrent invocations are rejected via an {@link AtomicBoolean} guard.</p>
 *
 * @author Goulven.Furet
 */
public class SitemapGenerationService {

	private static final Logger LOGGER = LoggerFactory.getLogger(SitemapGenerationService.class);

	private static final String SITEMAP_NAME_PRODUCT_PAGES = "product-pages.xml";
	private static final String SITEMAP_NAME_VERTICAL_PAGES = "category-pages.xml";
	private static final String SITEMAP_NAME_DEFAULT_GUIDES = "guides.xml";

	private final ProductRepository aggregatedDataRepository;
	private final UiConfig uiConfig;
	private final AtomicBoolean exportRunning = new AtomicBoolean(false);
	private final Logger statsLogger;
	private final VerticalsConfigService verticalsConfigService;

	public SitemapGenerationService(ProductRepository aggregatedDataRepository, UiConfig uiConfig,
			VerticalsConfigService verticalConfigService) {
		this.aggregatedDataRepository = aggregatedDataRepository;
		this.verticalsConfigService = verticalConfigService;
		this.statsLogger = GenericFileLogger.initLogger("stats-sitemap", Level.INFO, uiConfig.logsFolder());
		this.uiConfig = uiConfig;
	}

	/**
	 * Builds all sitemap files for every configured language.
	 *
	 * <p>Guarded by {@link AtomicBoolean}: if a previous run is still in progress
	 * the method returns immediately without starting a second generation.</p>
	 */
	@Scheduled(initialDelay = 1000L * 3600, fixedDelay = 1000L * 3600 * 24 * 7)
	public void generate() {
		if (!exportRunning.compareAndSet(false, true)) {
			LOGGER.warn("Sitemap generation already in progress – skipping");
			return;
		}

		try {
			LOGGER.info("Starting sitemap generation");
			statsLogger.info("Starting sitemap generation");

			uiConfig.getNamings().getBaseUrls().entrySet().forEach(e -> {
				String lang = e.getKey();
				String baseUrl = e.getValue();

				addProductsPages(baseUrl, lang);
				addVerticalPages(baseUrl, lang);
				boolean hasDefaultGuides = addDefaultGuidePages(baseUrl, lang);

				SitemapIndexGenerator index = SitemapIndexGenerator.of(baseUrl + "sitemap/")
						.addPage(SITEMAP_NAME_VERTICAL_PAGES)
						.addPage(SITEMAP_NAME_PRODUCT_PAGES);
				if (hasDefaultGuides) {
					index = index.addPage(SITEMAP_NAME_DEFAULT_GUIDES);
				}

				try {
					index.toFile(getSitemapFile("sitemap.xml", lang));
				} catch (IOException ex) {
					LOGGER.error("Error while generating sitemap index", ex);
				}
			});

			statsLogger.info("Sitemap generated");
		} finally {
			exportRunning.set(false);
		}
	}

	/**
	 * Adds individual product pages to the product sitemap.
	 *
	 * <p>Only enabled verticals are considered. Products are ordered by
	 * eco-score and must have a valid date.</p>
	 *
	 * @param baseUrl  site base URL for the target language
	 * @param language BCP-47 language tag
	 */
	private void addProductsPages(String baseUrl, String language) {
		SitemapGenerator sitemap = SitemapGenerator.of(baseUrl);

		for (VerticalConfig vertical : verticalsConfigService.getConfigsWithoutDefault(true)) {
			List<Product> products = aggregatedDataRepository
					.exportVerticalWithValidDateOrderByEcoscore(vertical.getId(), false)
					.toList();

			for (Product data : products) {
				String url = vertical.getBaseUrl(language) + "/" + data.url(language);
				LOGGER.info("Adding product page to sitemap : {}", url);
				sitemap = sitemap.addPage(getWebPage(url, ChangeFreq.DAILY, 1.0,
						Date.from(Instant.ofEpochMilli(data.getLastChange()))));
			}
		}

		try {
			sitemap.toFile(getSitemapFile(SITEMAP_NAME_PRODUCT_PAGES, language));
		} catch (IOException e) {
			LOGGER.error("Error while writing product sitemap", e);
		}
	}

	/**
	 * Adds vertical landing pages, sub-category pages, and vertical-specific
	 * editorial pages to the category sitemap.
	 *
	 * <p>Sub-categories include both those defined inline in the vertical YAML
	 * and those auto-generated from
	 * {@code classpath*:categories/<vertical>/*.yml} — both are available via
	 * {@link VerticalConfig#getSubCategories()}.</p>
	 *
	 * @param baseUrl  site base URL for the target language
	 * @param language BCP-47 language tag
	 */
	private void addVerticalPages(String baseUrl, String language) {
		SitemapGenerator sitemap = SitemapGenerator.of(baseUrl);

		for (VerticalConfig v : verticalsConfigService.getConfigsWithoutDefault(true)) {
			ProductI18nElements i18n = v.i18n(language);

			// Vertical home page
			sitemap = sitemap.addPage(getWebPage(baseUrl + i18n.getVerticalHomeUrl(), ChangeFreq.WEEKLY, 1.0));

			// Sub-category pages (inline + auto-generated from categories/*.yml)
			for (VerticalSubCategory subCategory : v.getSubCategories()) {
				String subCategorySlug = subCategory.getSlug() == null ? null : subCategory.getSlug().i18n(language);
				if (subCategorySlug == null || subCategorySlug.isBlank()) {
					continue;
				}
				String url = baseUrl + i18n.getVerticalHomeUrl() + "/" + subCategorySlug;
				LOGGER.info("Adding sub-category page to sitemap : {}", url);
				sitemap = sitemap.addPage(getWebPage(url, ChangeFreq.WEEKLY, 0.9));
			}

			// Buying guide pages discovered from guides/{vertical-id}/*.md
			for (String guideSlug : v.getGuides()) {
				String url = baseUrl + i18n.getVerticalHomeUrl() + "/" + guideSlug;
				LOGGER.info("Adding guide page to sitemap : {}", url);
				sitemap = sitemap.addPage(getWebPage(url, ChangeFreq.MONTHLY, 0.8));
			}

			// Vertical-specific editorial pages (rendered by CategoryPage.vue /
			// CategoryFiltersSidebar.vue from VerticalConfig i18n, no longer XWiki-backed)
			for (WikiPageConfig wikiPage : i18n.getWikiPages()) {
				String url = baseUrl + i18n.getVerticalHomeUrl() + "/" + wikiPage.getVerticalUrl();
				LOGGER.info("Adding to sitemap : {}", url);
				sitemap = sitemap.addPage(getWebPage(url, ChangeFreq.MONTHLY, 0.9));
			}
		}

		try {
			sitemap.toFile(getSitemapFile(SITEMAP_NAME_VERTICAL_PAGES, language));
		} catch (IOException e) {
			LOGGER.error("Error while writing vertical sitemap", e);
		}
	}

	/**
	 * Adds non-vertical buying guides (stored under {@code guides/default/}) to the
	 * dedicated {@code guides.xml} sitemap, served at {@code /guides/{slug}}.
	 *
	 * @param baseUrl  site base URL for the target language
	 * @param language BCP-47 language tag
	 * @return {@code true} if at least one guide was written, {@code false} if skipped
	 */
	private boolean addDefaultGuidePages(String baseUrl, String language) {
		List<String> guides = verticalsConfigService.getDefaultGuides();
		if (guides.isEmpty()) {
			return false;
		}

		SitemapGenerator sitemap = SitemapGenerator.of(baseUrl);
		for (String guideSlug : guides) {
			String url = baseUrl + "guides/" + guideSlug;
			LOGGER.info("Adding default guide page to sitemap : {}", url);
			sitemap = sitemap.addPage(getWebPage(url, ChangeFreq.MONTHLY, 0.8));
		}

		try {
			sitemap.toFile(getSitemapFile(SITEMAP_NAME_DEFAULT_GUIDES, language));
		} catch (IOException e) {
			LOGGER.error("Error while writing default guides sitemap", e);
		}
		return true;
	}

	/**
	 * Returns a {@link File} handle for the given sitemap name under the
	 * language-specific subfolder, creating intermediate directories as needed.
	 *
	 * @param name     sitemap file name (e.g. {@code "product-pages.xml"})
	 * @param language BCP-47 language tag used as subfolder name
	 * @return the target {@link File}
	 */
	public File getSitemapFile(String name, String language) {
		try {
			Files.createDirectories(Path.of(uiConfig.siteMapFolder().getAbsolutePath() + "/" + language + "/"));
		} catch (IOException e) {
			LOGGER.error("Error while creating sitemap folder", e);
		}
		return new File(uiConfig.siteMapFolder().getAbsolutePath() + "/" + language + "/" + name);
	}

	/**
	 * Builds a {@link WebPage} with no last-modification date.
	 *
	 * @param path       full URL of the page
	 * @param changeFreq expected change frequency
	 * @param priority   sitemap priority (0.0–1.0)
	 * @return configured {@link WebPage}
	 */
	private WebPage getWebPage(String path, ChangeFreq changeFreq, double priority) {
		return getWebPage(path, changeFreq, priority, null);
	}

	/**
	 * Builds a {@link WebPage} with an optional last-modification date.
	 *
	 * @param path       full URL of the page
	 * @param changeFreq expected change frequency
	 * @param priority   sitemap priority (0.0–1.0)
	 * @param lastMod    last modification date, or {@code null} to omit the field
	 * @return configured {@link WebPage}
	 */
	private WebPage getWebPage(String path, ChangeFreq changeFreq, double priority, Date lastMod) {
		WebPage w = WebPage.of(path);
		w.setChangeFreq(changeFreq);
		w.setPriority(priority);
		if (lastMod != null) {
			w.setLastMod(lastMod);
		}
		return w;
	}
}
