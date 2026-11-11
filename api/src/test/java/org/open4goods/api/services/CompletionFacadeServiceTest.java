package org.open4goods.api.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.open4goods.api.config.yml.ApiProperties;
import org.open4goods.api.services.completion.AmazonCompletionService;
import org.open4goods.api.services.completion.EprelCompletionService;
import org.open4goods.api.services.completion.IcecatCompletionService;
import org.open4goods.api.services.completion.ResourceCompletionService;
import org.open4goods.api.services.completion.WikidataCompletionService;
import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.VerticalConfig;

/**
 * Durations and counters of the completion pass (GOU-2099).
 */
class CompletionFacadeServiceTest {

	private final ResourceCompletionService resource = mock(ResourceCompletionService.class);
	private final IcecatCompletionService icecat = mock(IcecatCompletionService.class);
	private final EprelCompletionService eprel = mock(EprelCompletionService.class);
	private final WikidataCompletionService wikidata = mock(WikidataCompletionService.class);
	private final AmazonCompletionService amazon = mock(AmazonCompletionService.class);

	@ParameterizedTest
	@ValueSource(ints = { 1, 4 })
	void countsProcessedAndSkippedProductsPerCompletionService(int concurrency) {
		Set<Product> products = products(3);
		Product processed = products.iterator().next();
		when(eprel.process(any(), any())).thenAnswer(invocation -> invocation.getArgument(1) == processed);

		CompletionTimings timings = facade(concurrency).processAll(products, vertical());

		String eprelName = eprel.getClass().getSimpleName();
		assertThat(timings.products()).isEqualTo(3);
		assertThat(timings.processed(eprelName)).isEqualTo(1);
		assertThat(timings.skipped(eprelName)).isEqualTo(2);
		assertThat(timings.skipped(wikidata.getClass().getSimpleName())).isEqualTo(3);
		assertThat(timings.summary()).contains(eprelName + ": processed 1 (failed 0");
	}

	@ParameterizedTest
	@ValueSource(ints = { 1, 4 })
	void aFailingCompletionStopsTheFollowingOnesOfTheProduct(int concurrency) {
		Set<Product> products = products(1);
		when(eprel.process(any(), any())).thenThrow(new IllegalStateException("boom"));

		CompletionTimings timings = facade(concurrency).processAll(products, vertical());

		assertThat(timings.products()).isEqualTo(1);
		assertThat(timings.processed(eprel.getClass().getSimpleName())).isEqualTo(1);
		assertThat(timings.summary()).contains("failed 1");
		verify(wikidata, never()).process(any(), any());
		verify(amazon, never()).process(any(), any());
	}

	private CompletionFacadeService facade(int concurrency) {
		ApiProperties properties = new ApiProperties();
		properties.setCompletionConcurrency(concurrency);
		return new CompletionFacadeService(resource, icecat, eprel, wikidata, amazon, properties);
	}

	private static VerticalConfig vertical() {
		VerticalConfig vertical = new VerticalConfig();
		vertical.setId("oven");
		return vertical;
	}

	private static Set<Product> products(int count) {
		Set<Product> products = new HashSet<>();
		for (int i = 1; i <= count; i++) {
			products.add(new Product(3_760_000_000_000L + i));
		}
		return products;
	}
}
