//
//package org.open4goods.api.datasources;
//
//
//
//import static org.junit.jupiter.api.Assertions.fail;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.Map.Entry;
//
//import org.apache.commons.lang3.StringUtils;
//import org.junit.jupiter.api.Test;
//import org.open4goods.api.NoHopCustomDataFragmentRepository;
//import org.open4goods.api.config.yml.ApiProperties;
//import org.open4goods.api.services.StoreImagesService;
//import org.open4goods.api.services.store.DataFragmentStoreService;
//import org.open4goods.config.yml.datasource.CrawlProperties;
//import org.open4goods.config.yml.datasource.DataSourceProperties;
//import org.open4goods.config.yml.datasource.HtmlDataSourceProperties;
//import org.open4goods.config.yml.test.TestResultReport;
//import org.open4goods.config.yml.test.TestUrl;
//import org.open4goods.crawler.config.yml.FetcherProperties;
//import org.open4goods.crawler.services.DataFragmentCompletionService;
//import org.open4goods.crawler.services.IndexationService;
//import org.open4goods.crawler.services.fetching.CsvDatasourceFetchingService;
//import org.open4goods.crawler.services.fetching.DataFragmentWebCrawler;
//import org.open4goods.crawler.services.fetching.WebDatasourceFetchingService;
//import org.open4goods.exceptions.ValidationException;
//import org.open4goods.model.constants.Currency;
//import org.open4goods.model.data.DataFragment;
//import org.open4goods.model.data.Price;
//import org.open4goods.services.DataSourceConfigService;
//import org.open4goods.services.EvaluationService;
//import org.open4goods.services.ImageMagickService;
//import org.open4goods.services.SerialisationService;
//import org.open4goods.services.StandardiserService;
//import org.open4goods.services.alerting.AbstractAlertingService;
//import org.open4goods.services.alerting.AlertingServiceMock;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//import com.google.common.collect.Sets;
//
//import edu.uci.ics.crawler4j.crawler.CrawlController;
//import store.repository.DataFragmentRepository;
//
//@SpringBootTest
//
//
////TODO(test) : Test regression on favico
//public class DatasourceRegressionTest {
//
//	private static final String DATASOURCES_ENV_PARAMETER = "datasources";
//
//	protected static final Logger logger = LoggerFactory.getLogger(DatasourceRegressionTest.class);
//
//	// If true, the error datafragments will be shown
//	private static boolean WITH_DETAILS = false;
//	// If true, those tests will be bypassed
//	private static final boolean BYPASS = false;
//
//
//	@Configuration
//	public static class DrtConfig {
//
//		@Bean
//		AbstractAlertingService alertingService() {
//			return new AlertingServiceMock();
//		}
//
//		@Bean
//		FetcherProperties fetcherProperties() {
//			return new FetcherProperties();
//		}
//
//		@Bean
//		DataFragmentRepository dfRepo() {
//			return new NoHopCustomDataFragmentRepository();		
//			
//		}
//
//		@Bean
//		public WebDatasourceFetchingService webDatasourceFetchingService(@Autowired final IndexationService indexationService,
//				@Autowired final ApiProperties apiProperties, @Autowired final FetcherProperties fetcherProperties, @Autowired final AbstractAlertingService alertingService) {
//			return new WebDatasourceFetchingService(indexationService, fetcherProperties, fetcherProperties.getLogsDir(), alertingService);
//		}
//
//		@Bean
//		public CsvDatasourceFetchingService csvDatasourceFetchingService(@Autowired final IndexationService indexationService,
//				@Autowired final ApiProperties apiProperties, @Autowired final FetcherProperties fetcherProperties, @Autowired final AbstractAlertingService alertingService,
//				 @Autowired final WebDatasourceFetchingService httpFetchingService, @Autowired final  DataFragmentCompletionService dfCompletionService) {
//			return new CsvDatasourceFetchingService(dfCompletionService, indexationService, fetcherProperties,  httpFetchingService,fetcherProperties.getLogsDir(), alertingService);
//		}
//
//		@Bean
//		public EvaluationService evaluationService() {
//			return new EvaluationService();
//		}
//
//		@Bean
//		public SerialisationService serialisationService() {
//			return new SerialisationService();
//		}
//
//		@Bean
//		public ApiProperties apiProperties() {
//			return new ApiProperties();
//		}
//
//		public @Bean StoreImagesService brandAndStoreService(@Autowired final ApiProperties config, @Autowired final ImageMagickService imageMagick) {
//			return new StoreImagesService(config.getWorkFolder() + "/favicons/", imageMagick);
//		}
//
//		public @Bean ImageMagickService imageService() {
//			return new ImageMagickService();
//		}
//
//		// TODO(gof) : review standardisers
//		@Bean
//		StandardiserService standardiserService() {
//			return new StandardiserService() {
//
//				@Override
//				public void standarise(final Price price, final Currency currency) {
//
//				}
//			};
//		}
//
//		@Bean
//		public IndexationService indexationService(@Autowired final DataFragmentStoreService dataFragmentStoreService) {
//			return new IndexationService();
//		}
//
//		public @Bean DataFragmentStoreService dataFragmentStoreService(@Autowired DataFragmentRepository dataFragmentsRepository, @Autowired final ApiProperties config) {
//			return new DataFragmentStoreService(dataFragmentsRepository, config.getDataFragmentsQueueFolderLocation(),
//					config.getDataFragmentsDequeueSize(), config.getDataFragmentsDequeuePeriodMs(),config.getDataFragmentsDequeueWorkers());
//		}
//
//
//		/** The bean providing datasource configurations **/
//		public @Bean DataSourceConfigService datasourceConfigService(@Autowired final ApiProperties config) {
//			return new DataSourceConfigService(config.getDatasourcesFolders());
//		}
//
//		@Bean
//		public DataFragmentCompletionService offerCompletionService() {
//			return new DataFragmentCompletionService();
//		}
//
//	}
//
//
//	private @Autowired WebDatasourceFetchingService httpFetchingService;
//
//	private @Autowired DataSourceConfigService datasourceService;
//
//
//	private @Autowired CsvDatasourceFetchingService csvFetchingService;
//
//	private @Autowired DataFragmentCompletionService completionService;
//
//	private @Autowired SerialisationService serialisationService;
//
//
//	@Test
//	public void testRegression() {
//
//
//		if (null == System.getProperty(DATASOURCES_ENV_PARAMETER) && BYPASS) {
//			logger.error("DATASOURCE REGRESSION TESTS WILL BE SKIPPED !");
//			System.out.println("DATASOURCE REGRESSION TESTS WILL BE SKIPPED !");
//			return;
//
//		}
//
//
//		///////////////////////////////////////
//		// Indexing test data
//		///////////////////////////////////////
//		logger.info("Starting regression test for {} datasources");
//
//		// config.setDedicatedLogToConsole(true);
//
//		final List<TestResultReport> reports = new ArrayList<>();
//		int totUrl = 0;
//
//
//
//
//		// Loading all datasources
//		Map<String, DataSourceProperties> dss = datasourceService.getDatasourceConfigs();
//
//		//NOTE(gof) : so ugly !
//		final Map<String, DataSourceProperties> dss2 = dss;
//
//		// If
//		if (null != System.getProperty(DATASOURCES_ENV_PARAMETER)) {
//			logger.warn("Running regression against specified datasources : {}",System.getProperty(DATASOURCES_ENV_PARAMETER));
//			final Map<String, DataSourceProperties> cleaned = new HashMap<>();
//			Sets.newHashSet(System.getProperty(DATASOURCES_ENV_PARAMETER).split(",|;")).stream().forEach(e -> {
//				if (dss2.containsKey(e)) {
//					cleaned.put(e, dss2.get(e));
//				}
//				else {
//					logger.warn("Cannot find datasource {}",e);
//				}
//			});
//
//			WITH_DETAILS = true;
//			dss = cleaned;
//		}
//
//
//		for (final Entry<String, DataSourceProperties> dsProperties : dss.entrySet()) {
//
//			if (null != dsProperties.getValue().getApiDatasource()) {
//
//				//TODO(test) : Test api regression here
//			}else if (dsProperties.getValue().getCsvDatasource() != null) {
//
//
//
//				if (null == dsProperties.getValue().getCsvDatasource().getTestDatas() ||dsProperties.getValue().getCsvDatasource().getTestDatas().size() == 0) {
//									logger.error("WARNING  : " + dsProperties.getKey() + " HAS NO REGRESSION TESTS URLS");
////					fail("WARNING  : " + dsProperties.getKey() + " HAS NO CSV REGRESSION DATA TESTS ");
////					continue;
//				}
//
//
//
////				for ( final TestCsvLine testData : dsProperties.getValue().getCsvDatasource().getTestDatas()) {
////
////					////////////////////////////////
////					// Case of a CSV datasource
////					////////////////////////////////
////
////					try {
////						final DataFragment df = csvFetchingService.synchFetch(dsProperties.getKey(), dsProperties.getValue(), dsProperties.getValue().getCsvDatasource().getTestHeaders() , testData.getCsvLine());
////
////						// Completing
////						completionService.complete(df,dsProperties.getKey(), dsProperties.getValue(), logger);
////
////						// Validating
////						try {
////							df.validate(dsProperties.getValue().getValidationFields());
////
////							final TestResultReport report = testData.test(df, dsProperties.getKey());
////							if (report.getMessages().size() > 0) {
////								reports.add(report);
////							}
////
////						} catch (final ValidationException e) {
////							final TestResultReport nodata = new TestResultReport(df,dsProperties.getKey());
////							nodata.setUrl(testData.getCsvLine() );
////							nodata.addMessage(  e.getMessage() );
////							reports.add(nodata);
////						} catch (final Exception e) {
////							final TestResultReport nodata = new TestResultReport(df,dsProperties.getKey());
////							nodata.setUrl(testData.getCsvLine() );
////							nodata.addMessage(  e.getMessage() );
////							reports.add(nodata);
////
////						}
////
////					} catch (IOException | ValidationException e) {
////						fail("Unexpected exeption");
////						e.printStackTrace();
////					}
////
////
////
////
////
////			}
////
////
//
//
//
//			} else if (dsProperties.getValue().getWebDatasource() != null) {
//				////////////////////////////////
//				// Case of a WEB datasource
//				////////////////////////////////
//
//				CrawlController controler = null;
//				DataFragmentWebCrawler crawler = null;
//
//				final HtmlDataSourceProperties wds = dsProperties.getValue().getWebDatasource();
//
//				try {
//					logger.info("Configuring direct crawler for CSV datasource {}", dsProperties.getKey());
//					final CrawlProperties cc = wds.getCrawlConfig();
//					cc.setCleanupDelaySeconds(1);
//					cc.setCleanupDelaySeconds(1);
//					controler = httpFetchingService.createCrawlController("csv-" + dsProperties.getKey(), cc);
//					crawler = httpFetchingService.createWebCrawler(dsProperties.getKey(), dsProperties.getValue(), wds,logger);
//					crawler.setShouldFollowLinks(false);
//
//				} catch (final Exception e) {
//					logger.error("Error while instanciating crawlers", e);
//				}
//
//
//				if (null == wds.getTestUrls() || wds.getTestUrls().size() == 0) {
////					logger.error("WARNING  : " + dsProperties.getName() + " HAS NO REGRESSION TESTS URLS");
//					fail("WARNING  : " + dsProperties.getKey() + " HAS NO REGRESSION TESTS URLS");
//					continue;
//				}
//
//				logger.info("Testing datasource stability for {}", dsProperties.getKey());
//
//				final Map<String, DataFragment> byUrls = new HashMap<>();
//				for (final TestUrl tu : wds.getTestUrls()) {
//					final DataFragment df = crawler.visitNow(controler, tu.getUrl());
//
//					if (null == df) {
//
//						//TODO(bug) : handling special cases where running through authenticated proxy on gitlab runner
//						if (!StringUtils.isEmpty(wds.getCrawlConfig().getProxyHost())) {
//							logger.warn("No data for {} but a proxy is used. Skipping it on IC because of crawl4j authenticated proxy bug",tu.getUrl());
//						} else {
//							final TestResultReport nodata = new TestResultReport(df,dsProperties.getKey());
//							nodata.setUrl(tu.getUrl());
//							nodata.addMessage("no datafragment");
//							reports.add(nodata);
//						}
//
//
//					} else {
//
//						try {
//							df.validate(dsProperties.getValue().getValidationFields());
//
//							byUrls.put(tu.getUrl(), df);
//
//						} catch (final ValidationException e) {
//							final TestResultReport nodata = new TestResultReport(df,dsProperties.getKey());
//							nodata.setUrl(tu.getUrl() );
//							nodata.addMessage(  e.getMessage() );
//							reports.add(nodata);
//						}
//
//
//					}
//				}
//
//				for (final TestUrl tu : wds.getTestUrls()) {
//					totUrl ++;
//					if (byUrls.containsKey(tu.getUrl())) 	{
//						final TestResultReport report = tu.test(byUrls.get(tu.getUrl()),dsProperties.getKey());
//						if (report.getMessages().size() > 0) {
//							reports.add(report);
//						}
//					}
//				}
//				controler.shutdown();
//
//			} else {
//				fail ("Not a web or CSV datasource : " + dsProperties.getKey());
//			}
//		}
//
//
//
//
//
//		final StringBuilder sb = new StringBuilder();
//		boolean failed = false;
//		sb.append(
//				"\n=============================== DATASOURCE REGRESSION REPORT ===================================\n");
//
//		sb.append(reports.size()).append("/").append(totUrl).append(" failed\n");
//
//		for (final TestResultReport report : reports) {
//			failed = true;
//
//
//			sb.append(
//					"=============================== REGRESSIONS IN "+report.getDatasourceConfigName() +"===================================\n");
//			sb.append(report.getUrl()).append("\n");
//			sb.append(org.apache.commons.lang3.StringUtils.join(report.getMessages(), "\n"));
//
//
//			if (WITH_DETAILS) {
//				sb.append(
//						"\n------------------------------- DATAFRAGMENT ------------------------------------------------\\n");
//				sb.append(serialisationService.toJson(report.getData(), true));
//			}
//
//			sb.append(
//					"\n=============================================================================================\\n");
//
//		}
//
//		if (failed) {
//			logger.warn(sb.toString());
//			fail("Regressions found on datasources \n" + sb.toString());
//
//
//		}
//	}
//
//}