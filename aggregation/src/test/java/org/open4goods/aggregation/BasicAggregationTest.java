//package org.open4goods.aggregation;
//
//
//import static org.junit.jupiter.api.Assertions.assertNotNull;
//import static org.junit.jupiter.api.Assertions.assertTrue;
//import static org.junit.jupiter.api.Assertions.fail;
//
//import java.util.ArrayList;
//import java.util.HashSet;
//import java.util.List;
//
//import org.junit.jupiter.api.Test;
//import org.open4goods.aggregation.AbstractAggregationService;
//import org.open4goods.aggregation.aggregator.AbstractAggregator;
//import org.open4goods.aggregation.aggregator.RealTimeAggregator;
//import org.open4goods.aggregation.services.aggregation.PriceAggregationService;
//import org.open4goods.exceptions.NotAddedException;
//import org.open4goods.model.product.AggregatedData;
//import org.open4goods.services.DataSourceConfigService;
//import org.open4goods.services.SerialisationService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//
//@SpringBootTest
//public class BasicAggregationTest {
//
//	private @Autowired AbstractAggregator dataAggregationService;
//
//	private @Autowired DataSourceConfigService dataSourceConfigService;
//
//
//	@Configuration
//    static class TestConfig {
//
//		 @Bean List<AbstractAggregationService> aggregationServices() {
//			final List<AbstractAggregationService> services = new ArrayList<>();
//
//			services.add(new PriceAggregationService("./",null));
//
//			return services;
//		}
//
//		 @Bean AbstractAggregator dataAggregationService() {
//			return new RealTimeAggregator(aggregationServices());
//		}
//		 
//		 @Bean SerialisationService serialisationService() {
//			return new SerialisationService();
//		}
//    }
//
//	@Test
//	public void test() {
//		AggregatedData pr = null;
//		try {
//			pr = dataAggregationService.build(new HashSet<>(), new HashSet<>());
//		} catch (final NotAddedException e) {
//			fail (e.getMessage());
//		}
//		assertNotNull(pr);
//		assertTrue(pr.getAggregationResult().getParticipantServices().size() > 0);
//
//	}
//
//}
