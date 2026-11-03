package org.open4goods.ui.services;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import org.open4goods.commons.helper.GenericFileLogger;
import org.open4goods.model.localization.DomainLanguage;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.ProductCanonicalUrl;
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
import cz.jiripinkas.jsitemapgenerator.AbstractGenerator;
import cz.jiripinkas.jsitemapgenerator.ChangeFreq;
import cz.jiripinkas.jsitemapgenerator.WebPage;
import cz.jiripinkas.jsitemapgenerator.generator.SitemapGenerator;
import cz.jiripinkas.jsitemapgenerator.generator.SitemapIndexGenerator;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * Generates localised XML sitemaps for nudger.fr.
 *
 * <p>One sitemap file is produced per language and per content type:
 * <ul>
 *   <li><b>category-pages.xml</b>: vertical landing pages, sub-category pages
 *       (including auto-generated sub-categories from
 *       {@code classpath*:categories/<vertical>/*.yml}), and vertical-specific
 *       editorial pages.</li>
 *   <li><b>product-pages.xml</b>: individual product pages, at the canonical
 *       path given by {@link ProductCanonicalUrl} (the front-api
 *       {@code fullSlug}), so that no listed URL redirects.</li>
 * </ul>
 * A sitemap index file ({@code sitemap.xml}) is produced alongside each set.</p>
 *
 * <p>Generation runs a few minutes after startup, then daily. Every file is
 * written to a temporary sibling and moved over the previous one, so a
 * partial file is never served; a failed file keeps the previous version.
 * Failures are logged at ERROR and counted in the
 * {@value #METRIC_GENERATION} metric ({@code result=failure}); the
 * {@value #METRIC_LAST_SUCCESS} gauge exposes the epoch second of the last
 * fully successful run. Concurrent invocations are rejected via an
 * {@link AtomicBoolean} guard.</p>
 *
 * @author Goulven.Furet
 */
public class SitemapGenerationService {

	private static final Logger LOGGER = LoggerFactory.getLogger(SitemapGenerationService.class);

	static final String SITEMAP_NAME_PRODUCT_PAGES = "product-pages.xml";
	static final String SITEMAP_NAME_VERTICAL_PAGES = "category-pages.xml";
	static final String SITEMAP_NAME_DEFAULT_GUIDES = "guides.xml";
	static final String SITEMAP_NAME_INDEX = "sitemap.xml";

	/** Counter of generation runs, tagged {@code result=success|failure}. */
	static final String METRIC_GENERATION = "sitemap.generation";
	/** Epoch second of the last fully successful generation, 0 before the first one. */
	static final String METRIC_LAST_SUCCESS = "sitemap.generation.last.success";
	/** Number of URLs written in the last product sitemap, tagged by language. */
	static final String METRIC_PRODUCT_URLS = "sitemap.product.urls";

	private final ProductRepository aggregatedDataRepository;
	private final UiConfig uiConfig;
	private final AtomicBoolean exportRunning = new AtomicBoolean(false);
	private final Logger statsLogger;
	private final VerticalsConfigService verticalsConfigService;
	private final MeterRegistry meterRegistry;
	private final Counter successCounter;
	private final Counter failureCounter;
	private final AtomicLong lastSuccessEpochSeconds = new AtomicLong();
	private final Map<String, AtomicLong> productUrlCounts = new ConcurrentHashMap<>();

	public SitemapGenerationService(ProductRepository aggregatedDataRepository, UiConfig uiConfig,
			VerticalsConfigService verticalConfigService, MeterRegistry meterRegistry) {
		this.aggregatedDataRepository = aggregatedDataRepository;
		this.verticalsConfigService = verticalConfigService;
		this.statsLogger = GenericFileLogger.initLogger("stats-sitemap", Level.INFO, uiConfig.logsFolder());
		this.uiConfig = uiConfig;
		this.meterRegistry = meterRegistry;
		this.successCounter = Counter.builder(METRIC_GENERATION).tag("result", "success")
				.description("Sitemap generation runs").register(meterRegistry);
		this.failureCounter = Counter.builder(METRIC_GENERATION).tag("result", "failure")
				.description("Sitemap generation runs").register(meterRegistry);
		Gauge.builder(METRIC_LAST_SUCCESS, lastSuccessEpochSeconds, AtomicLong::get)
				.description("Epoch second of the last successful sitemap generation")
				.baseUnit("seconds")
				.register(meterRegistry);
	}

	/**
	 * Builds all sitemap files for every configured language, five minutes
	 * after startup and then every day.
	 *
	 * <p>Guarded by {@link AtomicBoolean}: if a previous run is still in progress
	 * the method returns immediately without starting a second generation.</p>
	 *
	 * @return {@code true} when every file was written, {@code false} when a
	 *         file failed or a run was already in progress
	 */
	@Scheduled(initialDelay = 5, fixedDelay = 24 * 60, timeUnit = TimeUnit.MINUTES)
	public boolean generate() {
		if (!exportRunning.compareAndSet(false, true)) {
			LOGGER.warn("Sitemap generation already in progress, skipping");
			return false;
		}

		try {
			LOGGER.info("Starting sitemap generation");
			statsLogger.info("Starting sitemap generation");

			boolean success = true;
			for (Map.Entry<String, String> e : uiConfig.getNamings().getBaseUrls().entrySet()) {
				success &= generateLanguage(e.getKey(), e.getValue());
			}

			if (success) {
				successCounter.increment();
				lastSuccessEpochSeconds.set(Instant.now().getEpochSecond());
				statsLogger.info("Sitemap generated");
				LOGGER.info("Sitemap generated");
			} else {
				failureCounter.increment();
				statsLogger.error("Sitemap generation failed, previous files kept for the failed parts");
				LOGGER.error("Sitemap generation failed, previous files kept for the failed parts");
			}
			return success;
		} catch (RuntimeException e) {
			failureCounter.increment();
			statsLogger.error("Sitemap generation failed: {}", e.getMessage());
			LOGGER.error("Sitemap generation failed", e);
			return false;
		} finally {
			exportRunning.set(false);
		}
	}

	/**
	 * Writes every sitemap file of one language. A failing file is logged and
	 * keeps its previous version; the other files are still written.
	 *
	 * @param lang    language key of {@code namings.baseUrls}, also the folder name
	 * @param baseUrl site base URL for this language
	 * @return {@code true} when every file of the language was written
	 */
	private boolean generateLanguage(String lang, String baseUrl) {
		boolean success = true;

		try {
			success &= writeProductPages(baseUrl, lang);
		} catch (IOException | RuntimeException e) {
			LOGGER.error("Error while generating product sitemap for language {}", lang, e);
			success = false;
		}

		try {
			writeSitemap(addVerticalPages(baseUrl, lang), SITEMAP_NAME_VERTICAL_PAGES, lang);
		} catch (IOException | RuntimeException e) {
			LOGGER.error("Error while generating category sitemap for language {}", lang, e);
			success = false;
		}

		boolean hasDefaultGuides = false;
		try {
			hasDefaultGuides = addDefaultGuidePages(baseUrl, lang);
		} catch (IOException | RuntimeException e) {
			LOGGER.error("Error while generating default guides sitemap for language {}", lang, e);
			success = false;
		}

		try {
			SitemapIndexGenerator index = SitemapIndexGenerator.of(baseUrl + "sitemap/")
					.addPage(SITEMAP_NAME_VERTICAL_PAGES)
					.addPage(SITEMAP_NAME_PRODUCT_PAGES);
			if (hasDefaultGuides) {
				index = index.addPage(SITEMAP_NAME_DEFAULT_GUIDES);
			}
			writeSitemap(index, SITEMAP_NAME_INDEX, lang);
		} catch (IOException | RuntimeException e) {
			LOGGER.error("Error while generating sitemap index for language {}", lang, e);
			success = false;
		}
		return success;
	}

	/**
	 * Writes the product sitemap of one language.
	 *
	 * <p>Only products of enabled verticals, not excluded and with a recent
	 * price and an impact score are listed (pages of disabled verticals are
	 * {@code noindex}, uncategorised products are not listed). Each URL is the
	 * base URL followed by {@link ProductCanonicalUrl#fullSlug}, the value the
	 * front-api exposes as {@code fullSlug}. {@code lastmod} is the product
	 * {@code lastChange}, omitted when unknown.</p>
	 *
	 * @param baseUrl  site base URL for the target language
	 * @param language language key of {@code namings.baseUrls}
	 * @return {@code true} when the file was written, {@code false} when the
	 *         language has no domain language
	 * @throws IOException when the file cannot be written
	 */
	private boolean writeProductPages(String baseUrl, String language) throws IOException {
		DomainLanguage domainLanguage = toDomainLanguage(language);
		if (domainLanguage == null) {
			LOGGER.error("No domain language for sitemap language '{}', product sitemap not generated", language);
			return false;
		}

		SitemapGenerator sitemap = SitemapGenerator.of(baseUrl);
		Set<String> paths = new LinkedHashSet<>();

		for (VerticalConfig vertical : verticalsConfigService.getConfigsWithoutDefault(true)) {
			try (Stream<Product> products = aggregatedDataRepository
					.exportVerticalWithValidDateOrderByEcoscore(vertical.getId(), false)) {
				for (Product product : (Iterable<Product>) products::iterator) {
					String path = productPath(product, vertical, domainLanguage);
					if (path == null || !paths.add(path)) {
						continue;
					}
					LOGGER.debug("Adding product page to sitemap : {}", path);
					Date lastMod = product.getLastChange() > 0
							? Date.from(Instant.ofEpochMilli(product.getLastChange()))
							: null;
					sitemap = sitemap.addPage(getWebPage(path.substring(1), ChangeFreq.DAILY, 1.0, lastMod));
				}
			}
		}

		writeSitemap(sitemap, SITEMAP_NAME_PRODUCT_PAGES, language);
		productUrlCounts.computeIfAbsent(language, this::registerProductUrlsGauge).set(paths.size());
		statsLogger.info("{} product URLs written for language {}", paths.size(), language);
		return true;
	}

	/**
	 * Canonical path of a product in the sitemap, or {@code null} when the
	 * product must not be listed.
	 *
	 * @param product        exported product
	 * @param vertical       enabled vertical the product was exported from
	 * @param domainLanguage domain language of the sitemap
	 * @return canonical path starting with a slash, or {@code null}
	 */
	static String productPath(Product product, VerticalConfig vertical, DomainLanguage domainLanguage) {
		if (product == null || !vertical.getId().equals(product.getVertical())) {
			return null;
		}
		return ProductCanonicalUrl.fullSlug(product, vertical, domainLanguage);
	}

	/**
	 * Maps a {@code namings.baseUrls} key to the front-api domain language:
	 * {@code fr} to {@link DomainLanguage#fr}, {@code en} and
	 * {@code default} to {@link DomainLanguage#en}.
	 *
	 * @param language language key
	 * @return domain language, or {@code null} when none matches
	 */
	static DomainLanguage toDomainLanguage(String language) {
		if (language == null) {
			return null;
		}
		if (ProductCanonicalUrl.DEFAULT_LANGUAGE_KEY.equals(language)) {
			return DomainLanguage.en;
		}
		for (DomainLanguage candidate : DomainLanguage.values()) {
			if (candidate.name().equalsIgnoreCase(language) || candidate.languageTag().equalsIgnoreCase(language)) {
				return candidate;
			}
		}
		return null;
	}

	private AtomicLong registerProductUrlsGauge(String language) {
		AtomicLong count = new AtomicLong();
		Gauge.builder(METRIC_PRODUCT_URLS, count, AtomicLong::get)
				.description("URLs written in the last product sitemap")
				.tag("language", language)
				.register(meterRegistry);
		return count;
	}

	/**
	 * Adds vertical landing pages, sub-category pages, and vertical-specific
	 * editorial pages to the category sitemap.
	 *
	 * <p>Sub-categories include both those defined inline in the vertical YAML
	 * and those auto-generated from
	 * {@code classpath*:categories/<vertical>/*.yml}, both are available via
	 * {@link VerticalConfig#getSubCategories()}.</p>
	 *
	 * @param baseUrl  site base URL for the target language
	 * @param language BCP-47 language tag
	 * @return the category sitemap
	 */
	private SitemapGenerator addVerticalPages(String baseUrl, String language) {
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
				LOGGER.debug("Adding sub-category page to sitemap : {}", url);
				sitemap = sitemap.addPage(getWebPage(url, ChangeFreq.WEEKLY, 0.9));
			}

			// Buying guide pages discovered from guides/{vertical-id}/*.md
			for (String guideSlug : v.getGuides()) {
				String url = baseUrl + i18n.getVerticalHomeUrl() + "/" + guideSlug;
				LOGGER.debug("Adding guide page to sitemap : {}", url);
				sitemap = sitemap.addPage(getWebPage(url, ChangeFreq.MONTHLY, 0.8));
			}

			// Vertical-specific editorial pages (rendered by CategoryPage.vue /
			// CategoryFiltersSidebar.vue from VerticalConfig i18n, no longer XWiki-backed)
			for (WikiPageConfig wikiPage : i18n.getWikiPages()) {
				String url = baseUrl + i18n.getVerticalHomeUrl() + "/" + wikiPage.getVerticalUrl();
				LOGGER.debug("Adding to sitemap : {}", url);
				sitemap = sitemap.addPage(getWebPage(url, ChangeFreq.MONTHLY, 0.9));
			}
		}
		return sitemap;
	}

	/**
	 * Adds non-vertical buying guides (stored under {@code guides/default/}) to the
	 * dedicated {@code guides.xml} sitemap, served at {@code /guides/{slug}}.
	 *
	 * <p>{@code guides/default/} is empty today, so the file is not written and
	 * the frontend does not list it in its sitemap index.</p>
	 *
	 * @param baseUrl  site base URL for the target language
	 * @param language BCP-47 language tag
	 * @return {@code true} if at least one guide was written, {@code false} if skipped
	 * @throws IOException when the file cannot be written
	 */
	private boolean addDefaultGuidePages(String baseUrl, String language) throws IOException {
		List<String> guides = verticalsConfigService.getDefaultGuides();
		if (guides.isEmpty()) {
			return false;
		}

		SitemapGenerator sitemap = SitemapGenerator.of(baseUrl);
		for (String guideSlug : guides) {
			String url = baseUrl + "guides/" + guideSlug;
			LOGGER.debug("Adding default guide page to sitemap : {}", url);
			sitemap = sitemap.addPage(getWebPage(url, ChangeFreq.MONTHLY, 0.8));
		}

		writeSitemap(sitemap, SITEMAP_NAME_DEFAULT_GUIDES, language);
		return true;
	}

	/**
	 * Writes a sitemap atomically: the XML goes to a hidden temporary file in
	 * the same folder, which is then moved over the target. Readers see either
	 * the previous complete file or the new complete file.
	 *
	 * @param generator sitemap or index generator
	 * @param name      sitemap file name
	 * @param language  language subfolder
	 * @throws IOException when the file cannot be written or moved
	 */
	private void writeSitemap(AbstractGenerator<?> generator, String name, String language) throws IOException {
		Path target = getSitemapFile(name, language).toPath();
		// Not Files.createTempFile: it creates owner-only files, and the frontend
		// process reads them. The run guard makes this fixed name safe.
		Path tmp = target.resolveSibling("." + name + ".tmp");
		try {
			Files.writeString(tmp, generator.toString(), StandardCharsets.UTF_8);
			try {
				Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			} catch (AtomicMoveNotSupportedException e) {
				Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
			}
		} finally {
			Files.deleteIfExists(tmp);
		}
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
	 * @param path       page path relative to the base URL
	 * @param changeFreq expected change frequency
	 * @param priority   sitemap priority (0.0-1.0)
	 * @return configured {@link WebPage}
	 */
	private WebPage getWebPage(String path, ChangeFreq changeFreq, double priority) {
		return getWebPage(path, changeFreq, priority, null);
	}

	/**
	 * Builds a {@link WebPage} with an optional last-modification date.
	 *
	 * @param path       page path relative to the base URL
	 * @param changeFreq expected change frequency
	 * @param priority   sitemap priority (0.0-1.0)
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
