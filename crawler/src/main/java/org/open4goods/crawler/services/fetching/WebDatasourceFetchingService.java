package org.open4goods.crawler.services.fetching;

import java.io.File;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.http.message.BasicHeader;
import org.open4goods.config.yml.datasource.CrawlProperties;
import org.open4goods.config.yml.datasource.DataSourceProperties;
import org.open4goods.config.yml.datasource.ExtractorConfig;
import org.open4goods.config.yml.datasource.HtmlDataSourceProperties;
import org.open4goods.crawler.config.yml.FetcherProperties;
import org.open4goods.crawler.extractors.Extractor;
import org.open4goods.crawler.model.CustomUrlProvider;
import org.open4goods.crawler.repository.IndexationRepository;
import org.open4goods.crawler.services.IndexationService;
import org.open4goods.exceptions.TechnicalException;
import org.open4goods.model.constants.TimeConstants;
import org.open4goods.model.crawlers.WebIndexationStats;
import org.open4goods.model.data.DataFragment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;

import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.crawler.authentication.AuthInfo;
import edu.uci.ics.crawler4j.fetcher.PageFetcher;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtConfig;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtServer;

/**
 * Service that handles the csv  datasources fetching
 *
 * @author goulven
 *
 */

public class WebDatasourceFetchingService extends DatasourceFetchingService{

	private static final Logger logger = LoggerFactory.getLogger(WebDatasourceFetchingService.class);


	private @Autowired ApplicationContext applicationContext;

	private final FetcherProperties fetcherProperties;

	private final IndexationService indexationService;

	/** The internally maintained map of crawl controllers**/
	private final Map<String, CrawlController> controllers = new ConcurrentHashMap<>();
	/** The internally maintained map of crawl start date**/
	private final Map<String, Long> controllersDate = new ConcurrentHashMap<>();
	/**	The internally maintained map of activ crawl config	 */
	private final Map<String, DataSourceProperties> controllersConfig = new ConcurrentHashMap<>();

	@Autowired
	private  AutowireCapableBeanFactory autowireCapableBeanFactory;
	

	public WebDatasourceFetchingService(final IndexationService indexationService, final FetcherProperties fetcherProperties, IndexationRepository repository, final String logsFolder, boolean toConsole) {
		super(logsFolder, toConsole,repository);
		this.fetcherProperties = fetcherProperties;
		this.indexationService = indexationService;
	
	}

	/**
	 * Schedule the controller map cleanup, to evict terminated crawls
	 */
	@Scheduled(initialDelay=0L, fixedDelay=TimeConstants.CRAWLER_REMOVE_FINISHED_CRAWLERS_MS)
	private void cleanControllers() {

		// Calling the terminate to collect stats
		controllers.entrySet().stream().filter(e -> e.getValue().isFinished()).forEach(s -> super.finished(stats().get(s.getKey()), controllersConfig.get(s.getKey())));
		controllersConfig.entrySet().removeIf(e -> controllers.get(e.getKey()).isFinished());
		controllersDate.entrySet().removeIf(e -> controllers.get(e.getKey()).isFinished());
		controllers.entrySet().removeIf(e -> e.getValue().isFinished());
	}

	/**
	 * Make a synchronous crawl. To be used for test purpose, as crawler and controller are reinstanciated from scratch.
	 * Controller
	 * @param dsProperties
	 * @param url
	 * @return
	 * @throws TechnicalException 
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	public DataFragment synchCrawl(final DataSourceProperties dsProperties,final String url) throws TechnicalException, IOException, InterruptedException {
		final CrawlController realTimeController = createCrawlController("REALTIME-"+dsProperties.getName(), dsProperties.webDataSource().getCrawlConfig());
		realTimeController.getConfig().setCleanupDelaySeconds(1);
		realTimeController.getConfig().setThreadMonitoringDelaySeconds(1);
		realTimeController.getConfig().setShutdownOnEmptyQueue(true);

		final DataFragmentWebCrawler crawler = createWebCrawler("REALTIME-"+dsProperties.getName(), dsProperties, dsProperties.webDataSource());

		realTimeController.addSeed(url);

		// Limiting number of pages to initialy provided url
		realTimeController.getConfig().setMaxPagesToFetch(1);

		// Class loading
		logger.info("Starting sync crawler for datasource {} with url {}", dsProperties.getName(), url);

		final DataFragment ret = crawler.visitNow(realTimeController, url);

		realTimeController.shutdown();

		return ret;
	}



	@Override
	public void start(final DataSourceProperties provider, final String datasourceConfName) throws TechnicalException {
		final CrawlController controller = createCrawlController(datasourceConfName, provider.getWebDatasource().getCrawlConfig());


		// Adding base url
		try {
			controller.addSeed(provider.getWebDatasource().getBaseUrl());
		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// Adding optional initial url's
		if (null != provider.getWebDatasource().getInitialUrls()) {
			for (final String url : provider.getWebDatasource().getInitialUrls()) {
				try {
					controller.addSeed(url);
				} catch (IOException | InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		// Adding optional initial url's
		if (null != provider.getWebDatasource().getCustomUrlProviderClass()) {
				
			try {
				CustomUrlProvider seedProvider = ((CustomUrlProvider)Class.forName(provider.getWebDatasource().getCustomUrlProviderClass()).newInstance());
				autowireCapableBeanFactory.autowireBean(seedProvider);
				Set<String> urls = seedProvider.getUrls();
				urls.stream().forEach(e -> {
					try {
						controller.addSeed(e);
					} catch (IOException | InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				});
//				controller.addSeed(datasourceConfName);
				
				
			} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
			
		}

		
		
		
		
		
		
		final DataFragmentWebCrawler webCrawler = createWebCrawler(datasourceConfName, provider, provider.getWebDatasource());

		controllers.put(datasourceConfName, controller);
		controllersDate.put(datasourceConfName, System.currentTimeMillis());
		controllersConfig.put(datasourceConfName, provider);



		logger.info("Starting crawler : {}", datasourceConfName);

		// Class loading
		logger.info("Starting async crawler {} with {} threads", datasourceConfName, provider.getWebDatasource().getCrawlConfig().getThreads());
		controllers.get(datasourceConfName).startNonBlocking(() -> webCrawler, provider.getWebDatasource().getCrawlConfig().getThreads() );

	}


	@Override
	public void stop(final String providerName) {
		logger.info("Stopping crawler : {}", providerName);
		controllers.get(providerName).shutdown();
		indexationService.clearIndexedCounter(providerName);
	}


	/**
	 * Creates an http crawl controller
	 * @param webDataSourceProperties
	 * @param provider
	 * @return
	 * @throws TechnicalException 
	 */
	public CrawlController createCrawlController(final String providerName, CrawlProperties crawlProperties) throws TechnicalException {
		CrawlController controller = null;

		// Getting the crawler configuration instance (from datasource if specified, take the default one otherelse)
		if (null == crawlProperties) {
			crawlProperties = fetcherProperties.getDefaultCrawlConfig();
		}

		// Setting the robots config
		final RobotstxtConfig robotsConfig = new RobotstxtConfig();
		robotsConfig.setUserAgentName(crawlProperties.getUserAgentString());
		robotsConfig.setEnabled(crawlProperties.getRobotsTxtCompliance());

		// Setting the crawl config

		final CrawlConfig crawlConfig = new CrawlConfig();
		crawlConfig.setUserAgentString(crawlProperties.getUserAgentString());
		crawlConfig.setCleanupDelaySeconds(crawlProperties.getCleanupDelaySeconds());
		crawlConfig.setConnectionTimeout(crawlProperties.getConnectionTimeout());
		crawlConfig.setMaxConnectionsPerHost(crawlProperties.getMaxConnectionsPerHost());
		crawlConfig.setMaxDownloadSize(crawlProperties.getMaxDownloadSize());
		crawlConfig.setMaxOutgoingLinksToFollow(crawlProperties.getMaxOutgoingLinksToFollow());
		crawlConfig.setMaxTotalConnections(crawlProperties.getMaxTotalConnections());
		crawlConfig.setPolitenessDelay(crawlProperties.getPolitenessDelay());
		crawlConfig.setProxyHost(crawlProperties.getProxyHost());
		crawlConfig.setProxyPassword(crawlProperties.getProxyPassword());
		crawlConfig.setProxyUsername(crawlProperties.getProxyUsername());
		crawlConfig.setProxyPort(crawlProperties.getProxyPort());
		crawlConfig.setSocketTimeout(crawlProperties.getSocketTimeout());
		crawlConfig.setThreadMonitoringDelaySeconds(crawlProperties.getThreadMonitoringDelaySeconds());
		crawlConfig.setThreadShutdownDelaySeconds (crawlProperties.getThreadShutdownDelaySeconds());

//		// Auth infos must not be empty to proxy to apply
		crawlConfig.setAuthInfos(new ArrayList<AuthInfo>());
//


		// Custom headers
		final Set<BasicHeader> defaultHeaders = new HashSet<>();
		crawlProperties.getDefaultHeaders().stream().forEach((h) -> defaultHeaders.add(new BasicHeader(h.getName(), h.getValue())))  ;
		crawlConfig.setDefaultHeaders(defaultHeaders );


		// Forcing the unshared crawlfolder
		crawlConfig.setCrawlStorageFolder(fetcherProperties.getCrawlerStorage() + File.separator + providerName);

		// Validating
		try {
			crawlConfig.validate();
		} catch (final Exception e) {
			throw new TechnicalException(e);
		}

		// Creating the controller
		PageFetcher fetcher = null;
		try {
			fetcher = new PageFetcher(crawlConfig);
		} catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			
			
		final RobotstxtServer robots = new RobotstxtServer(robotsConfig, fetcher);
		try {
			controller = new CrawlController(crawlConfig, fetcher, robots);
		} catch (final Exception e1) {
			throw new TechnicalException("Cannot instanciate crawlController : " + providerName, e1);
		}
		return controller;
	}


	/**
	 * Instanciate the OfferWebCrawler from setting
	 * will receive the products
	 * @param extractorConfigs
	 *
	 * @param p
	 * @return
	 */
	public DataFragmentWebCrawler createWebCrawler(final String datasourceConfigName, final DataSourceProperties datasourceConfig , final HtmlDataSourceProperties webDatasourceProps) {

		///////////
		// Dedicated logging
		///////////
		final Logger dedicatedLogger = createDatasourceLogger(datasourceConfigName, datasourceConfig,fetcherProperties.getCrawlerLogDir());

		dedicatedLogger.info("dedicated logging started for {}",datasourceConfigName);

		return createWebCrawler(datasourceConfigName, datasourceConfig, webDatasourceProps, dedicatedLogger);
	}




	/**
	 * Instanciate the OfferWebCrawler from setting and given a custom logger
	 * will receive the products
	 * @param extractorConfigs
	 *
	 * @param p
	 * @return
	 */
	public DataFragmentWebCrawler createWebCrawler(final String datasourceConfigName, final DataSourceProperties datasourceConfig , final HtmlDataSourceProperties webDatasourceProps, final Logger targetLogger) {



		//////////////////
		// Instanciating extractors
		//////////////////
		final List<Extractor> extractors = getExtractors(webDatasourceProps, targetLogger);

		final DataFragmentWebCrawler instance = new DataFragmentWebCrawler(datasourceConfigName, env, datasourceConfig ,webDatasourceProps, extractors, targetLogger);
		applicationContext.getAutowireCapableBeanFactory().autowireBean(instance);

		return instance;
	}


	/**
	 * Instanciate extractors for a given webdatasource
	 * @param webDatasourceProps
	 * @param dedicatedLogger
	 * @return
	 */
	public List<Extractor> getExtractors(final HtmlDataSourceProperties webDatasourceProps, final Logger dedicatedLogger) {
		final List<Extractor> extractors = new ArrayList<>();

		for (final ExtractorConfig conf : webDatasourceProps.getExtractors()) {
			try {
				final Extractor extractor = Extractor.getInstance( conf, dedicatedLogger);
				applicationContext.getAutowireCapableBeanFactory().autowireBean(extractor);

				extractors.add(extractor);
			} catch (final Exception e) {
				logger.error("Cannot instanciate extractor {}", conf.getClass(), e);
			}
		}
		return extractors;
	}

	/**
	 * Return the stats
	 */
	@Override
	public Map<String, WebIndexationStats> stats() {

		final var ret = new HashMap<String,WebIndexationStats>();

		for (final Entry<String, CrawlController> entry : getControllers().entrySet()) {
			final WebIndexationStats c = new WebIndexationStats(entry.getKey(), 0L);
			c.setFinished(entry.getValue().isFinished());
			c.setShuttingDown(entry.getValue().isShuttingDown());
			c.setStartDate(controllersDate.get(entry.getKey()));
			if (null != entry.getValue().getFrontier() && !entry.getValue().isFinished()) {
				c.setQueueLength(entry.getValue().getFrontier().getQueueLength());
				c.setNumberOfProcessedDatas(entry.getValue().getFrontier().getNumberOfProcessedPages());
			}
			c.setNumberOfIndexedDatas(indexationService.getIndexed(entry.getKey()));
			ret.put(entry.getKey(), c);
		}
		return ret;
	}

	/**
	 *
	 * @return all running controllers
	 */
	public Map<String, CrawlController> getControllers() {
		return controllers;
	}

	/**
	 *
	 * @return the running crawl jobs
	 */
	public Set<CrawlController> runings() {
		return getControllers().values().stream().filter(e -> !e.isFinished()).collect(Collectors.toSet());
	}



}
