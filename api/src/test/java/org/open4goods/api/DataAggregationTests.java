//package org.open4goods.api;
//
//import org.junit.jupiter.api.Test;
//import org.junit.runner.RunWith;
//import org.junit.runners.Suite.SuiteClasses;
//import org.open4goods.api.config.ApiConfig;
//import org.open4goods.api.controller.api.CrawlerOrchestrationController;
//import org.open4goods.crawler.Crawler;
//import org.open4goods.crawler.services.FetchersService;
//import org.open4goods.crawler.services.fetching.CsvDatasourceFetchingService;
//import org.open4goods.crawler.services.fetching.DatasourceFetchingService;
//import org.open4goods.dao.ProductRepository;
//import org.open4goods.services.DataSourceConfigService;
//import org.open4goods.services.SerialisationService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.test.context.junit4.SpringRunner;
//import org.springframework.util.Assert;
//
//@RunWith(SpringRunner.class)
//@SpringBootTest(classes = { Api.class, ApiConfig.class,  ProductRepository.class })
////@AutoConfigureMockMvc
////@TestPropertySource(
////  locations = "classpath:application-devsec.yml")
//
//@ActiveProfiles(profiles = { "devsec", "dev" })
//@SuiteClasses(value = {CrawlingTest.class})
//
//
//public class DataAggregationTests {
//
////	
////	@Autowired
////	private CsvDatasourceFetchingService csvDatasourceFetchingService;
////	
//	@Autowired
//	private SerialisationService serialisationService;
//
//	@Autowired
//	private ProductRepository productRepository;
////	
////	@Autowired
////	private  FetchersService fetchersService;
////
//	@Autowired
//	private DataSourceConfigService datasourceConfigService;
//	
//	
//	@Test
//	public void registrationWorksThroughAllLayers() throws Exception {
//		
////		
////		for (String datasources : datasourceConfigService.datasourceConfigs()) {
////			
////		}
////			
////		fetchersService.getWebDatasourceFetchingService().synchCrawl(p, url)
////		
////		
////		
//////		crawlerOrchestrationController.triggerAllCsvFetcher();
////		csvDatasourceFetchingService.start(null, null);
////		datasourceFetchingService.start(null, null);
//		productRepository.countMainIndex();
//
//	}
//
//	@Test
//	public void whenContextLoads_thenServiceISNotNull() {
//		System.out.println(productRepository.countMainIndex());
//		Assert.isTrue(productRepository.countMainIndex() == 0);
//	}
//
//}