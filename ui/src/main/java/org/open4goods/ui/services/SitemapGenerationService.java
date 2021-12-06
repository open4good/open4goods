package org.open4goods.ui.services;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.io.FileUtils;
import org.open4goods.dao.AggregatedDataRepository;
import org.open4goods.helper.GenericFileLogger;
import org.open4goods.model.product.AggregatedData;
import org.open4goods.ui.config.yml.UiConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import com.redfin.sitemapgenerator.ChangeFreq;
import com.redfin.sitemapgenerator.WebSitemapGenerator;
import com.redfin.sitemapgenerator.WebSitemapUrl;
import com.redfin.sitemapgenerator.WebSitemapUrl.Options;

import ch.qos.logback.classic.Level;

/**
 * Service in charge of generating the CSV opendata file
 *
 * @author Goulven.Furet
 *
 */


public class SitemapGenerationService {

	private static final Logger LOGGER = LoggerFactory.getLogger(SitemapGenerationService.class);

	private AggregatedDataRepository aggregatedDataRepository;
	private UiConfig uiConfig;

	// The flag that indicates wether opendata export is running or not
	private AtomicBoolean exportRunning = new AtomicBoolean(false);

	private final Map<String, WebSitemapGenerator> siteGens = new HashMap<>();

	private Logger statsLogger;


	public SitemapGenerationService(AggregatedDataRepository aggregatedDataRepository, UiConfig uiConfig) {
		this.aggregatedDataRepository = aggregatedDataRepository;
		this.uiConfig = uiConfig;
		this.statsLogger = GenericFileLogger.initLogger("stats-sitemap", Level.INFO, uiConfig.logsFolder(), false);
	}

	/**
	 * Iterates over all aggregatedData to generate the sitemap
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
		// Initialisation
		init();

		AtomicLong totalItems= new AtomicLong(0);
		// Processing each data
		aggregatedDataRepository.exportAllHavingPrices()
		// TODO (conf) : Number of offers count to figure in sitemap
		.filter(e -> e.getOffersCount() > 1)
		.forEach(e -> {			
			onAggregatedData(e);
			totalItems.incrementAndGet();
		});

		// Closing
		terminate();
		
		statsLogger.info("Sitemap generated with {} items", totalItems.get());
		
		exportRunning.set(false);

	}

	public void init() {

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
				siteGens.put(urls.getKey(), new WebSitemapGenerator(
						uiConfig.getBaseUrl(urls.getKey()) + "sitemap/" + urls.getKey() + "/", folder));
			} catch (final MalformedURLException e) {
				LOGGER.warn("Invalid url in sitemap : {}", e.getMessage());
			}

			/////////////////////////
			// Adding common pages
			/////////////////////////

			final WebSitemapGenerator sg = siteGens.get(urls.getKey());

			// TODO(design,0.25,seo) : add opendata and other static pages
			for (final String serverName : uiConfig.getNamings().getServerNames().values()) {
				addUrl(sg, "https://" + serverName + "/", ChangeFreq.MONTHLY, 1.0);
//				addUrl(sg, "https://" + serverName + "/api", ChangeFreq.MONTHLY, 1.0);
			}
		}

	}

	/**
	 * Convert an aggregated data to a sitemap entry
	 * 
	 * @param data
	 */
	public void onAggregatedData(final AggregatedData data) {

		for (final Entry<String, String> urls : uiConfig.getNamings().getServerNames().entrySet()) {
			try {

				//TODO : Not internationalized
//				final String u = uiConfig.getBaseUrl(urls.getKey()) + data.getUrls().getUrls().get(urls.getKey());
				final String u = uiConfig.getBaseUrl(urls.getKey()) + data.getNames().getName();
				
				// Getting the last modified date
				Date now = null;
				if (null != data.getLastChange()) {
					now = Date.from(Instant.ofEpochMilli(data.getLastChange()));
				}

				// Adding page
				addUrl(siteGens.get(urls.getKey()), u, ChangeFreq.WEEKLY, 0.9, now);

			} catch (final Exception e) {
				LOGGER.warn("Error while adding url in sitemap : {}", e.getMessage());
			}
		}
	}

	public void terminate() {

		LOGGER.info("Ending site map generation");
		for (final WebSitemapGenerator w : siteGens.values()) {
			w.write();
			w.writeSitemapsWithIndex();
		}

	}

	/**
	 * Add an url to the sitemap
	 *
	 * @param sitemap
	 * @param url
	 * @param changeFreq
	 * @param priority
	 * @param lastMod
	 */
	private void addUrl(final WebSitemapGenerator sitemap, final String url, final ChangeFreq changeFreq,
			final double priority, final Date lastMod) {
		try {
			final Options options = new Options(url);
			options.changeFreq(changeFreq);
			if (null != lastMod) {
				options.lastMod(lastMod);
			}
			options.priority(priority);
			sitemap.addUrl(new WebSitemapUrl(options));
		} catch (final Exception e) {
			LOGGER.error("Error while adding {} to sitemap : {}", url, e.getMessage());
		}

	}

	private void addUrl(final WebSitemapGenerator sg, final String string, final ChangeFreq monthly, final double d) {
		addUrl(sg, string, monthly, d, null);

	}

}
