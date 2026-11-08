package org.open4goods.api.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.open4goods.api.config.yml.ApiProperties;
import org.open4goods.commons.services.ResourceService;
import org.open4goods.model.price.AggregatedPrices;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.ProductPartialUpdateField;
import org.open4goods.model.product.ProductPartialUpdateHolder;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.feedservice.service.FeedIndexingService;
import org.open4goods.services.feedservice.service.FeedService;
import org.open4goods.services.productrepository.config.IndexationConfig;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.open4goods.verticals.VerticalsConfigService;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;

/**
 * GOU-1709: {@link BatchService#batch(VerticalConfig)} loads a vertical at the start of the 13:00 batch and
 * writes it back at the end. Offer updates indexed meanwhile (full product, partial update, or already in the
 * index) must survive: the queue is no longer flushed, and the batch never writes offers older than the latest
 * realtime update.
 */
class BatchServiceRealtimeOffersTest {

	private static final long GTIN = 7333394145372L;
	private static final long OTHER_GTIN = 8690842629433L;
	private static final long LOADED_AT = 1_700_000_000_000L;
	private static final long UPDATED_AT = LOADED_AT + 60_000L;

	private final VerticalConfig vertical = new VerticalConfig();
	private ElasticsearchOperations elasticsearchOperations;
	private CompletionFacadeService completionFacadeService;
	private ProductRepository repository;
	private BatchService batchService;

	/** Documents the index returns when the batch reads back the products it is about to write. */
	private final Map<Long, Product> indexed = new HashMap<>();

	@BeforeEach
	void setUp() {
		vertical.setId("dishwasher");

		IndexationConfig indexationConfig = new IndexationConfig();
		indexationConfig.setProductWorkers(0);
		indexationConfig.setPartialProductWorkers(0);
		elasticsearchOperations = mock(ElasticsearchOperations.class);
		when(elasticsearchOperations.search(any(Query.class), eq(Product.class), eq(ProductRepository.CURRENT_INDEX)))
				.thenAnswer(inv -> searchHits(new ArrayList<>(indexed.values())));
		repository = spy(new ProductRepository(indexationConfig, elasticsearchOperations));
		doAnswer(inv -> Stream.of(loadedProduct())).when(repository).getProductsMatchingVerticalId(vertical);

		completionFacadeService = mock(CompletionFacadeService.class);
		batchService = new BatchService(mock(AggregationFacadeService.class), completionFacadeService,
				mock(VerticalsConfigService.class), repository, mock(FeedIndexingService.class), mock(FeedService.class),
				mock(SerialisationService.class), mock(ResourceService.class), mock(ApiProperties.class));
	}

	/** The product as the batch loads it: one offer, never updated since {@link #LOADED_AT}. */
	private Product loadedProduct() {
		Product product = new Product();
		product.setId(GTIN);
		product.setVertical(vertical.getId());
		product.setLastChange(LOADED_AT);
		product.setOffersCount(1);
		return product;
	}

	/** The same product after a realtime aggregation, while the batch computes: a second offer. */
	private static Product realtimeUpdate(long lastChange) {
		Product product = new Product();
		product.setId(GTIN);
		product.setLastChange(lastChange);
		product.setOffersCount(2);
		product.setPrice(new AggregatedPrices());
		return product;
	}

	/** Runs {@code realtime} while the batch completes the products it loaded. */
	private void duringBatch(Runnable realtime) {
		doAnswer(inv -> {
			realtime.run();
			return null;
		}).when(completionFacadeService).processAll(anySet(), eq(vertical));
	}

	@SuppressWarnings("unchecked")
	private static SearchHits<Product> searchHits(List<Product> products) {
		List<SearchHit<Product>> hits = new ArrayList<>();
		for (Product product : products) {
			SearchHit<Product> hit = mock(SearchHit.class);
			when(hit.getContent()).thenReturn(product);
			hits.add(hit);
		}
		SearchHits<Product> searchHits = mock(SearchHits.class);
		when(searchHits.getSearchHits()).thenReturn(hits);
		return searchHits;
	}

	/** Products of the full indexation queue, in the order the workers will index them. */
	private List<Product> fullQueue() {
		return new ArrayList<>(repository.getFullProductQueue());
	}

	@Test
	void fullUpdateQueuedDuringTheBatchSurvivesIt() {
		Product realtime = realtimeUpdate(UPDATED_AT);
		Product otherVertical = new Product();
		otherVertical.setId(OTHER_GTIN);
		duringBatch(() -> repository.addToFullindexationQueue(List.of(realtime, otherVertical)));

		batchService.batch(vertical);

		// Nothing realtime is dropped, and the batch version, indexed last, carries the newer offers
		assertThat(fullQueue()).hasSize(3);
		assertThat(fullQueue().subList(0, 2)).containsExactly(realtime, otherVertical);
		Product written = fullQueue().get(2);
		assertThat(written).isNotSameAs(realtime);
		assertThat(written.getId()).isEqualTo(GTIN);
		assertThat(written.getVertical()).isEqualTo(vertical.getId());
		assertThat(written.getOffersCount()).isEqualTo(2);
		assertThat(written.getPrice()).isSameAs(realtime.getPrice());
		assertThat(written.getLastChange()).isEqualTo(UPDATED_AT);
	}

	@Test
	void partialUpdateQueuedDuringTheBatchSurvivesIt() {
		AggregatedPrices price = new AggregatedPrices();
		ProductPartialUpdateHolder partial = new ProductPartialUpdateHolder(GTIN);
		partial.addChange(ProductPartialUpdateField.LAST_CHANGE, UPDATED_AT);
		partial.addChange(ProductPartialUpdateField.PRICE, price);
		partial.addChange(ProductPartialUpdateField.OFFERS_COUNT, 2);
		duringBatch(() -> repository.addToPartialIndexationQueue(List.of(partial)));

		batchService.batch(vertical);

		assertThat(repository.getPartialProductQueue()).containsExactly(partial);
		assertThat(fullQueue()).singleElement().satisfies(written -> {
			assertThat(written.getOffersCount()).isEqualTo(2);
			assertThat(written.getPrice()).isSameAs(price);
			assertThat(written.getLastChange()).isEqualTo(UPDATED_AT);
		});
	}

	@Test
	void updateIndexedDuringTheBatchIsNotOverwritten() {
		Product realtime = realtimeUpdate(UPDATED_AT);
		duringBatch(() -> indexed.put(GTIN, realtime));

		batchService.batch(vertical);

		assertThat(fullQueue()).singleElement().satisfies(written -> {
			assertThat(written.getOffersCount()).isEqualTo(2);
			assertThat(written.getPrice()).isSameAs(realtime.getPrice());
			assertThat(written.getLastChange()).isEqualTo(UPDATED_AT);
		});
	}

	@Test
	void olderStateNeverReplacesTheBatchOffers() {
		Product stale = realtimeUpdate(LOADED_AT - 1);
		duringBatch(() -> {
			indexed.put(GTIN, stale);
			repository.addToFullindexationQueue(List.of(stale));
		});

		batchService.batch(vertical);

		Product written = fullQueue().get(1);
		assertThat(written).isNotSameAs(stale);
		assertThat(written.getOffersCount()).isEqualTo(1);
		assertThat(written.getPrice()).isNotSameAs(stale.getPrice());
		assertThat(written.getLastChange()).isEqualTo(LOADED_AT);
	}

	@Test
	void indexReadFailureStillWritesTheBatchWithQueuedOffers() {
		when(elasticsearchOperations.search(any(Query.class), eq(Product.class), eq(ProductRepository.CURRENT_INDEX)))
				.thenThrow(new IllegalStateException("es_rejected_execution_exception"));
		Product realtime = realtimeUpdate(UPDATED_AT);
		duringBatch(() -> repository.addToFullindexationQueue(List.of(realtime)));

		batchService.batch(vertical);

		assertThat(fullQueue()).hasSize(2);
		assertThat(fullQueue().get(1).getOffersCount()).isEqualTo(2);
	}
}
