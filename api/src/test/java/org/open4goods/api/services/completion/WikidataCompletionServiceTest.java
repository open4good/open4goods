package org.open4goods.api.services.completion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.open4goods.api.config.yml.ApiProperties;
import org.open4goods.api.services.AggregationFacadeService;
import org.open4goods.api.services.aggregation.aggregator.StandardAggregator;
import org.open4goods.commons.services.DataSourceConfigService;
import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.services.wikidataservice.config.WikidataServiceProperties;
import org.open4goods.services.wikidataservice.service.WikidataLookupService;
import org.open4goods.services.wikidataservice.service.WikidataSearchService;
import org.open4goods.verticals.VerticalsConfigService;

import ch.qos.logback.classic.Level;

/**
 * Refresh rule of the Wikidata completion (GOU-2099): a product for which no entity was found is not searched again
 * on each daily batch.
 */
class WikidataCompletionServiceTest {

	private static final long DAY_MS = 24L * 3600 * 1000;

	private WikidataCompletionService service;
	private WikidataSearchService searchService;
	private final VerticalConfig vertical = new VerticalConfig();
	private Product product;

	@BeforeEach
	void setUp() {
		ApiProperties apiProperties = mock(ApiProperties.class);
		when(apiProperties.logsFolder()).thenReturn("/tmp/logs");
		when(apiProperties.aggLogLevel()).thenReturn(Level.INFO);
		AggregationFacadeService aggregationFacadeService = mock(AggregationFacadeService.class);
		when(aggregationFacadeService.getStandardAggregator("wikidata-aggregation"))
				.thenReturn(mock(StandardAggregator.class));
		searchService = mock(WikidataSearchService.class);
		WikidataServiceProperties properties = new WikidataServiceProperties();
		properties.setPolitenessDelayMs(0);

		service = new WikidataCompletionService(mock(ProductRepository.class), mock(VerticalsConfigService.class),
				apiProperties, mock(DataSourceConfigService.class), aggregationFacadeService, searchService,
				mock(WikidataLookupService.class), properties);
		product = new Product(3_760_000_000_017L);
	}

	@Test
	void productNotFoundIsNotSearchedAgainWithinTheMissWindow() {
		when(searchService.searchByGtin(anyString())).thenReturn(Optional.empty());
		when(searchService.searchByBrandModel(any(), anyList())).thenReturn(Optional.empty());

		assertThat(service.shouldProcess(vertical, product)).isTrue();
		service.processProduct(vertical, product);

		assertThat(product.getDatasourceCodes()).containsKey(WikidataCompletionService.MISS_CODE);
		assertThat(service.shouldProcess(vertical, product)).isFalse();
	}

	@Test
	void productNotFoundIsSearchedAgainAfterTheMissWindow() {
		product.getDatasourceCodes().put(WikidataCompletionService.MISS_CODE, System.currentTimeMillis() - 8 * DAY_MS);

		assertThat(service.shouldProcess(vertical, product)).isTrue();
	}

	@Test
	void foundProductKeepsItsRefreshWindow() {
		product.getDatasourceCodes().put(WikidataCompletionService.DATASOURCE_NAME,
				System.currentTimeMillis() - 2 * DAY_MS);

		assertThat(service.shouldProcess(vertical, product)).isFalse();

		product.getDatasourceCodes().put(WikidataCompletionService.DATASOURCE_NAME,
				System.currentTimeMillis() - 31 * DAY_MS);

		assertThat(service.shouldProcess(vertical, product)).isTrue();
	}
}
