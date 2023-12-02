
package org.open4goods.crawler.services.fetching;

import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.open4goods.config.yml.datasource.DataSourceProperties;
import org.open4goods.config.yml.datasource.HtmlDataSourceProperties;
import org.open4goods.crawler.config.yml.FetcherProperties;
import org.open4goods.crawler.extractors.Extractor;
import org.open4goods.crawler.extractors.XpathExtractor;
import org.open4goods.crawler.services.DataFragmentCompletionService;
import org.open4goods.crawler.services.IndexationService;
import org.open4goods.exceptions.ValidationException;
import org.open4goods.helper.DocumentHelper;
import org.open4goods.helper.GenericFileLogger;
import org.open4goods.model.data.DataFragment;
import org.slf4j.Logger;
import org.slf4j.helpers.NOPLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.w3c.dom.Document;

import ch.qos.logback.classic.Level;
import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.crawler.exceptions.ContentFetchException;
import edu.uci.ics.crawler4j.crawler.exceptions.PageBiggerThanMaxSizeException;
import edu.uci.ics.crawler4j.crawler.exceptions.ParseException;
import edu.uci.ics.crawler4j.fetcher.PageFetchResult;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
import edu.uci.ics.crawler4j.parser.NotAllowedContentException;
import edu.uci.ics.crawler4j.url.WebURL;

public class DataFragmentWebCrawler extends WebCrawler {

//	protected static final Logger logger = LoggerFactory.getLogger(DataFragmentWebCrawler.class);

	private final static Pattern queryUrl = Pattern.compile("\\?|&");

//	private final ObjectMapper mapper = new ObjectMapper();

	private final static Pattern GLOBAL_EXCLUSION_RULES = Pattern.compile(
			".*(\\.(css|js|bmp|gif|jpe?g|png|tiff?|mid|mp2|mp3|mp4|wav|avi|mov|mpeg|ram|m4v|pdf|exe|rm|ico|smil|wmv|swf|wma|gz|zip|rar|rss)(\\?(.*))?$)");


	@Autowired
	private FetcherProperties globalConf;


	protected @Autowired Environment env;
	
	
	@Autowired
	private DataFragmentCompletionService completionService;

	private @Autowired IndexationService indexationService;


	/**
	 * This specific crawler configuration
	 */
	private final DataSourceProperties datasourceProperties;
	private final HtmlDataSourceProperties webdatasourceProperties;


	private final List<Extractor> extractors = new ArrayList<>();

	private final Logger dedicatedLogger;

	private Logger urlsLogger;



	/**
	 * If links should be followed. Used by direct fetching mode
	 */
	private boolean shouldFollowLinks = true;

	private final String datasourceConfigName;



	public DataFragmentWebCrawler(final String datasourceConfigName, final Environment environment, final DataSourceProperties config, final HtmlDataSourceProperties webdatasourceProperties, final List<Extractor> extractors,
                           final Logger dedicatedLogger) {
		datasourceProperties = config;
		this.webdatasourceProperties = webdatasourceProperties;
		// Re-sort to have xpath extractor first
		// That's a performance improvement to bypass the processing if category
		// (marking a valid page, and always provided by xpath extractor)
		for (final Extractor e : extractors) {
			if (e instanceof XpathExtractor) {
				this.extractors.add(0, e);
			} else {
				this.extractors.add(e);
			}
		}

		this.dedicatedLogger = dedicatedLogger;
		this.datasourceConfigName = datasourceConfigName;

	}



	@Override
	public void onStart() {
		super.onStart();

		// Starting url-logger if defined
		if (globalConf.isLogAccepted() || globalConf.isLogRejected()) {
			// Logging to console according to dev profile and conf
			boolean toConsole = ArrayUtils.contains(env.getActiveProfiles(), "dev") || ArrayUtils.contains(env.getActiveProfiles(), "devsec");
			// TODO : Not nice, mutualize


            urlsLogger = GenericFileLogger.initLogger(  datasourceProperties.getName()+"-urls", Level.INFO,
					globalConf.getCrawlerLogDir(), toConsole);
		} else {
			urlsLogger = NOPLogger.NOP_LOGGER;
		}

	}

	@Override
	public boolean shouldVisit(final Page referringPage, final WebURL url) {

		dedicatedLogger.debug("shouldVisit() ; {}", url.getURL());

		if (GLOBAL_EXCLUSION_RULES.matcher(url.toString()).matches()) {
			// Is excluded by the global exclusion rules
			// stat.getNotVisited().incrementAndGet();

			if (globalConf.isLogRejected()) {
				urlsLogger.info("no ; global exclusion rules ; {}", url.getURL());
			}
			return false;
		}

		/////////////////////////////
		// Domains filtering
		/////////////////////////////
		if (!url.getURL().startsWith(webdatasourceProperties.getBaseUrl())) {
			if (globalConf.isLogRejected()) {
				urlsLogger.info("no ; domain rules ; {}", url.getURL());
			}
			return false;
		}

		///////////////////////////////////////////
		// Url with parameters filtering
		//////////////////////////////////////////

		if (queryUrl.matcher(url.getURL()).find()) {
			if (globalConf.isLogRejected()) {
				urlsLogger.info("no ; query url ; {}", url.getURL());
			}
			return false;
		}


		///////////////////////////////
		// The must NOT contains filter
		//////////////////////////////
		if (webdatasourceProperties.getUrlExclusionsFilter() != null && webdatasourceProperties.getUrlExclusionsFilter().size() != 0) {
			// Have to apply the urlExclusionsFilter

			for (final String filter : webdatasourceProperties.getUrlExclusionsFilter()) {
				if (url.getURL().contains(filter)) {
					if (globalConf.isLogRejected()) {
						urlsLogger.info("no ; urlExclusionsFilter ; {}", url.getURL());
					}
					return false;
				}
			}
		}

		///////////////////////////////
		// The must contains filter
		//////////////////////////////
		if (webdatasourceProperties.getUrlContainsFilter() != null && webdatasourceProperties.getUrlContainsFilter().size() != 0) {
			// Have to apply the urlContainsFilter

			for (final String filter : webdatasourceProperties.getUrlContainsFilter()) {
				if (url.getURL().contains(filter)) {
					if (globalConf.isLogAccepted()) {
						urlsLogger.info("yes ; urlContainsRule ; {}", url.getURL());
					}
					return true;
				}
			}
			if (globalConf.isLogRejected()) {
				urlsLogger.info("no; urlContainsRule ; {}", url.getURL());
			}
			return false;
		}


		final boolean ret = super.shouldVisit(referringPage, url);

		if (ret) {
			// stat.getShouldVisit().incrementAndGet();
			if (globalConf.isLogAccepted()) {
				urlsLogger.info("yes ;  ; {}", url.getURL());
			}
		} else {
			// stat.getNotVisited().incrementAndGet();
			if (globalConf.isLogRejected()) {
				urlsLogger.info("no ; crawl4j 'super' rules ; {}", url.getURL());
			}
		}

		return ret;
	}

	@Override
	/**
	 */
	public void visit(final Page page) {
		final String url = page.getWebURL().getURL();
		dedicatedLogger.info("Visiting {}", url);


		///////////////////////////////
		// The must contains filter
		//////////////////////////////
		if (webdatasourceProperties.getUrlContainsExtractionFilter() != null && webdatasourceProperties.getUrlContainsExtractionFilter().size() != 0) {
			// Have to apply the urlContainsFilter

			boolean skip = true;
			for (final String filter : webdatasourceProperties.getUrlContainsExtractionFilter()) {
				if (url.contains(filter)) {
					skip = false;
					break;
				}
			}

			if (skip) {
				return;
			}
		}

		///////////////////////////////
		// The must NOT contains filter
		//////////////////////////////
		if (webdatasourceProperties.getUrlExclusionsExtractionFilter() != null && webdatasourceProperties.getUrlExclusionsExtractionFilter().size() != 0) {
			// Have to apply the urlExclusionsFilter

			for (final String filter : webdatasourceProperties.getUrlExclusionsExtractionFilter()) {
				if (url.contains(filter)) {
					return;
				}
			}
		}


		// Retrieve the clean document and delegate
		try {

			if (!(page.getParseData() instanceof HtmlParseData)) {
				dedicatedLogger.warn("Url {} will not be parsed because of unhandled type : {}", url,
						page.getParseData().getClass());
				return;
			}

			final HtmlParseData parseData = (HtmlParseData) page.getParseData();

			final Document doc = DocumentHelper.getDocument(parseData.getHtml());

			///////////////////////////
			// Delegating data parsing
			///////////////////////////

			// Creating the new offer

			// Apply the extractors
			DataFragment o = null;

			if (null == o) {
				o = DataFragment.newOffer(url, datasourceProperties.getName());
			}

			processExtraction(o, page, doc, extractors,getMyController());

			if (null == o) {
				dedicatedLogger.warn("no DataFragment : {}", url);
				return;
			}


			// DataFragment "standard" completion
			completionService.complete(o, datasourceConfigName, datasourceProperties, dedicatedLogger);

//			// Logging
//			if (datasourceProperties.getDevMode() && env.acceptsProfiles("dev")) {
//				final String output = serialisationService.toJson(o, true);
//				dedicatedLogger.warn(
//						"\n=============================\nOFFER\n=============================\n{}\n=============================\n{}",
//						url, output);
//			}

			////////////////////////
			// Validating offer
			////////////////////////
			try {
				final Set<String> validationFields = datasourceProperties.getValidationFields();

				if (null != validationFields && 0 != validationFields.size()) {
					o.validate(validationFields);
				} else {
					dedicatedLogger.warn(
							"No validations fields defined for {} !!!!!! Item will be added without any validation",
							datasourceProperties.getName());
				}
			} catch (final ValidationException e) {
				dedicatedLogger.info("offer skipped ; validation failed ; {} ; {}",
						StringUtils.join(e.getResult(), ", "), url);
				return;
			}

			// If "registry mode", then update the buffer

			try {

				///////////////////////////
				// Sending to indexation
				///////////////////////////

				
				// If a "non product" indexation, store as CSV
				
				
				if (datasourceProperties.getWebDatasource().isBrand2csv()) {
					
					// TODO
					throw new RuntimeException("Indexing brand score");
					
					
				} else {
					indexationService.index(o,datasourceConfigName);					
				}
				

			} catch (final Exception e) {
				logger.error("error at {}", url, e);
			}


		} catch (final Exception e) {
			logger.error("Unexpected error while parsing {} : {}", page.getWebURL().getURL(), e.getMessage(), e);
		}

	}

	/**
	 * Build a DataFragment from a crawled page given a set of extractors to use for
	 * extraction.
	 *
	 * @param o
	 * @param page
	 * @param url
	 * @param parseData
	 * @param doc
	 * @param extractorzs
	 * @return
	 */
	private DataFragment processExtraction(final DataFragment o, final Page page, final Document doc,
			final List<Extractor> extractorzs,final edu.uci.ics.crawler4j.crawler.CrawlController crawlController) {

		final String url = page.getWebURL().getURL();
		final HtmlParseData parseData = (HtmlParseData) page.getParseData();

		// Getting the default locale for "default" price parsing
		Locale locale;
		if (!StringUtils.isEmpty(page.getLanguage())) {
			locale = Locale.of(page.getLanguage());
		} else {
			locale = Locale.getDefault(Locale.Category.FORMAT);
		}

		for (final Extractor extractor : extractorzs) {
			try {
				dedicatedLogger.debug("offer data extraction ; {} ;  {}", extractor.getClass().getSimpleName(), url);

				extractor.parse(url, page, parseData, doc, datasourceProperties, locale, o, this,crawlController);

				if (null != o && webdatasourceProperties.getEvictIfNoXpathCategory() && extractor instanceof XpathExtractor
						&& null != extractor.getExtractorConfig().getCategory()) {
					if (StringUtils.isEmpty(o.getCategory())) {
						dedicatedLogger.warn(
								"XpathExtractor has returned no category ({}). Skipping remainings extraction process : {}",
								extractor.getExtractorConfig().getCategory(), url);
						return null;
					}
				}

			}

			catch (final Exception e) {
				logger.warn("Unexpected error while parsing product with {} at {} >> {}",
						extractor.getClass().getSimpleName(), url, e);
			}
		}

		return o;
	}

	public List<Extractor> getExtractors() {
		return extractors;
	}

	/**
	 * Visit and parse an url immediatly with crawler configured extractors and a given DataFragment to complete
	 *
	 * @param url
	 * @param extractorz
	 * @return
	 */
	public DataFragment visitNow(final edu.uci.ics.crawler4j.crawler.CrawlController crawlController, final String url, final DataFragment existing) {
		return visitNow(crawlController, url,extractors,existing);
	}


	/**
	 * Visit and parse an url immediatly with crawler configured extractors
	 *
	 * @param url
	 * @param extractorz
	 * @return
	 */
	public DataFragment visitNow(final edu.uci.ics.crawler4j.crawler.CrawlController crawlController, final String url) {
		return visitNow(crawlController, url,extractors,DataFragment.newOffer(url));

	}

	public DataFragment visitNow(final edu.uci.ics.crawler4j.crawler.CrawlController crawlController, final String url, final List<Extractor> extractors) {
		return visitNow(crawlController, url,extractors, DataFragment.newOffer(url));
	}


	/**
	 * TODO(gof) : should be made agnostic of dedicatedLogger and datasourceproperties
	 * Visit and parse an url immediatly with custom extrcators
	 * @param url
	 * @param extractorz
	 * @param existing
	 * @return
	 */
	public DataFragment visitNow(final edu.uci.ics.crawler4j.crawler.CrawlController crawlController, final String url, final List<Extractor> extractorz, final DataFragment existing) {

		WebURL curURL = new WebURL();
		curURL.setURL(url);
		PageFetchResult fetchResult = null;
		Page page = new Page(curURL);
		try {

			fetchResult = crawlController.getPageFetcher().fetchPage(curURL);
			int statusCode = fetchResult.getStatusCode();

			// Trying to follow the 30x
			 if (statusCode == HttpStatus.SC_MOVED_PERMANENTLY ||
	                    statusCode == HttpStatus.SC_MOVED_TEMPORARILY ||
	                    statusCode == HttpStatus.SC_MULTIPLE_CHOICES ||
	                    statusCode == HttpStatus.SC_SEE_OTHER ||
	                    statusCode == HttpStatus.SC_TEMPORARY_REDIRECT ||
	                    statusCode == 308) {


				    dedicatedLogger.info("Following redirection from {} to {}",url,fetchResult.getMovedToUrl());
				 	curURL = new WebURL();
					curURL.setURL(fetchResult.getMovedToUrl());
					page = new Page(curURL);
					fetchResult = crawlController.getPageFetcher().fetchPage(curURL);
					statusCode = fetchResult.getStatusCode();

			 }




			page.setFetchResponseHeaders(fetchResult.getResponseHeaders());
			page.setStatusCode(statusCode);
			if (statusCode < 200 || statusCode > 299) {
				dedicatedLogger.warn("Status code {} encountered while direct crawling {}", statusCode, url);

			} else { // if status code is 200
				if (!curURL.getURL().equals(fetchResult.getFetchedUrl())) {
					curURL.setURL(fetchResult.getFetchedUrl());
				}

				if (!fetchResult.fetchContent(page, crawlController.getConfig().getMaxDownloadSize())) {
					throw new ContentFetchException();
				}

				if (page.isTruncated()) {
					dedicatedLogger.warn(
							"Warning: unknown page size exceeded max-download-size, truncated to: "
									+ "({}), at URL: {}",
							crawlController.getConfig().getMaxDownloadSize(), curURL.getURL());
				}

				try {
					crawlController.getParser().parse(page, curURL.getURL());
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				final Document doc ;
				DataFragment ret = null;
				if (!(page.getParseData() instanceof HtmlParseData) ) {
//					final TextParseData parseData = (TextParseData) page.getParseData();
//					doc = DocumentHelper.getDocument(parseData.getTextContent());

				} else {
					// Classical crawler4j one
					final HtmlParseData parseData = (HtmlParseData) page.getParseData();
					doc = DocumentHelper.getDocument(parseData.getHtml());
					ret = processExtraction(existing, page, doc, extractorz,crawlController);
					completionService.complete(ret,datasourceConfigName, datasourceProperties, dedicatedLogger);
				}
				

				if (null == ret) {
					dedicatedLogger.warn("visitNow extraction returned null for {}",url);
				}



				return ret;
			}
		} catch (final PageBiggerThanMaxSizeException e) {
			onPageBiggerThanMaxSize(curURL.getURL(), e.getPageSize());
		} catch (final ParseException pe) {
			onParseError(curURL);
		} catch (ContentFetchException | SocketTimeoutException cfe) {
			onContentFetchError(curURL);
			onContentFetchError(page);
		} catch (final NotAllowedContentException nace) {
			dedicatedLogger.debug("Skipping: {} as it contains binary content which you configured not to crawl",
					curURL.getURL());
		} catch (final Exception e) {
			onUnhandledException(curURL, e);
		} finally {
			if (fetchResult != null) {
				fetchResult.discardContentIfNotConsumed();
			}
		}
		return null;
	}



	public void setShouldFollowLinks(final boolean shouldFollowLinks) {
		this.shouldFollowLinks = shouldFollowLinks;
	}


    @Override
	protected boolean shouldFollowLinksIn(final WebURL url) {
        return shouldFollowLinks ;
    }

    @Override
	protected void onContentFetchError(final WebURL webUrl) {
    	dedicatedLogger.warn("Can't fetch content of: {}", webUrl.getURL());
    }

    @Override
	protected void onContentFetchError(final Page page) {
        dedicatedLogger.warn("Can't fetch content of: {}", page.getWebURL().getURL());
    }

    @Override
    	public void onBeforeExit() {
    		// TODO Auto-generated method stub
    		super.onBeforeExit();
    	}

}
