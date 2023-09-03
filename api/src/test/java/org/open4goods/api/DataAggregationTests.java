package org.open4goods.api;

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.open4goods.crawler.controller.CrawlController;
import org.open4goods.dao.ProductRepository;
import org.open4goods.services.DataSourceConfigService;
import org.open4goods.services.SerialisationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.Assert;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { Api.class, CrawlController.class})
//@AutoConfigureMockMvc
//@TestPropertySource(
//  locations = "classpath:application-devsec.yml")

@ActiveProfiles(profiles = { "devsec" })


public class DataAggregationTests {

//	
//	@Autowired
//	private CsvDatasourceFetchingService csvDatasourceFetchingService;
//	
	@Autowired
	private SerialisationService serialisationService;

	@Autowired
	private ProductRepository productRepository;
//	
//	@Autowired
//	private  FetchersService fetchersService;
//
	@Autowired
	private DataSourceConfigService datasourceConfigService;
	
	
	@Test
	public void registrationWorksThroughAllLayers() throws Exception {
		
//		
//		for (String datasources : datasourceConfigService.datasourceConfigs()) {
//			
//		}
//			
//		fetchersService.getWebDatasourceFetchingService().synchCrawl(p, url)
//		
//		
//		
////		crawlerOrchestrationController.triggerAllCsvFetcher();
//		csvDatasourceFetchingService.start(null, null);
//		datasourceFetchingService.start(null, null);
		productRepository.countMainIndex();

	}

	@Test
	public void whenContextLoads_thenServiceISNotNull() {
		System.out.println(productRepository.countMainIndex());
		Assert.isTrue(productRepository.countMainIndex() != 0);
	}

}