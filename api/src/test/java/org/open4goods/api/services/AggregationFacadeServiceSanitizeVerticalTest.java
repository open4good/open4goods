package org.open4goods.api.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.LongStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InOrder;
import org.open4goods.api.config.yml.ApiProperties;
import org.open4goods.api.services.aggregation.aggregator.StandardAggregator;
import org.open4goods.brand.service.BrandService;
import org.open4goods.commons.exceptions.AggregationSkipException;
import org.open4goods.commons.services.BarcodeForensicsService;
import org.open4goods.commons.services.BarcodeValidationService;
import org.open4goods.commons.services.Gs1PrefixService;
import org.open4goods.commons.services.textgen.BlablaService;
import org.open4goods.icecat.services.IcecatFeatureResolver;
import org.open4goods.model.price.AggregatedPrices;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.ProductPartialUpdateField;
import org.open4goods.model.product.ProductPartialUpdateHolder;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.productrepository.config.IndexationConfig;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.verticals.VerticalsConfigService;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;

/**
 * GOU-2274: {@link AggregationFacadeService#sanitizeVertical(VerticalConfig)}, behind
 * {@code POST /aggregate/verticals/{vertical}}, reads the products of a vertical through a scroll and writes them
 * back after aggregation. Like the batch (GOU-1709), it queues them by pages, each page getting the newest realtime
 * offers just before being queued: an offer update indexed or queued during the run is never overwritten by the
 * offers the product was read with. {@code POST /aggregate/verticals} runs every vertical, and a failing one does
 * not stop the next ones (GOU-2280).
 */
class AggregationFacadeServiceSanitizeVerticalTest {

	private static final long GTIN = 7333394145372L;
	private static final long OTHER_GTIN = 8690842629433L;
	private static final long LOADED_AT = 1_700_000_000_000L;
	private static final long UPDATED_AT = LOADED_AT + 60_000L;

	@TempDir
	Path logs;

	private final VerticalConfig vertical = new VerticalConfig();
	private final VerticalsConfigService verticalsConfigService = mock(VerticalsConfigService.class);
	private ElasticsearchOperations elasticsearchOperations;
	private ProductRepository repository;
	private StandardAggregator aggregator;
	private AggregationFacadeService facade;

	/** Products the scroll on the vertical's categories returns, in order. */
	private final List<Product> loaded = new ArrayList<>();

	/** Documents the index returns when the run reads back the products it is about to write. */
	private final Map<Long, Product> indexed = new HashMap<>();

	/** Set when the product stream is closed, which releases the scroll. */
	private final AtomicBoolean streamClosed = new AtomicBoolean();

	@BeforeEach
	void setUp() throws Exception {
		vertical.setId("washing-machine");

		IndexationConfig indexationConfig = new IndexationConfig();
		indexationConfig.setProductWorkers(0);
		indexationConfig.setPartialProductWorkers(0);
		elasticsearchOperations = mock(ElasticsearchOperations.class);
		when(elasticsearchOperations.search(any(Query.class), eq(Product.class), eq(ProductRepository.CURRENT_INDEX)))
				.thenAnswer(inv -> searchHits(new ArrayList<>(indexed.values())));
		repository = spy(new ProductRepository(indexationConfig, elasticsearchOperations));
		doAnswer(inv -> loaded.stream().onClose(() -> streamClosed.set(true))).when(repository)
				.getProductsMatchingCategoriesOrVerticalId(vertical);

		ApiProperties apiProperties = new ApiProperties();
		apiProperties.setRootFolder(logs + "/");
		facade = spy(new AggregationFacadeService(repository, apiProperties, mock(Gs1PrefixService.class),
				verticalsConfigService, mock(BarcodeValidationService.class), mock(BrandService.class),
				mock(BlablaService.class), mock(IcecatFeatureResolver.class), mock(BarcodeForensicsService.class)));
		aggregator = mock(StandardAggregator.class);
		doReturn(aggregator).when(facade).getStandardAggregator();
	}

	/** A product as the scroll reads it: one offer, never updated since {@link #LOADED_AT}. */
	private Product loadedProduct(long id) {
		Product product = new Product();
		product.setId(id);
		product.setVertical(vertical.getId());
		product.setLastChange(LOADED_AT);
		product.setOffersCount(1);
		loaded.add(product);
		return product;
	}

	/** The same product after a realtime aggregation, while the run aggregates: a second offer. */
	private static Product realtimeUpdate(long lastChange) {
		Product product = new Product();
		product.setId(GTIN);
		product.setLastChange(lastChange);
		product.setOffersCount(2);
		product.setPrice(new AggregatedPrices());
		return product;
	}

	/** Runs {@code realtime} while the run aggregates the products it read. */
	private void duringSanitisation(Runnable realtime) throws AggregationSkipException {
		doAnswer(inv -> {
			realtime.run();
			return inv.getArgument(0);
		}).when(aggregator).onProduct(any());
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
	void fullUpdateQueuedDuringTheRunSurvivesIt() throws Exception {
		loadedProduct(GTIN);
		Product realtime = realtimeUpdate(UPDATED_AT);
		Product otherProduct = new Product();
		otherProduct.setId(OTHER_GTIN);
		duringSanitisation(() -> repository.addToFullindexationQueue(List.of(realtime, otherProduct)));

		facade.sanitizeVertical(vertical);

		// Nothing realtime is dropped, and the sanitised version, indexed last, carries the newer offers
		assertThat(fullQueue()).hasSize(3);
		assertThat(fullQueue().subList(0, 2)).containsExactly(realtime, otherProduct);
		Product written = fullQueue().get(2);
		assertThat(written).isNotSameAs(realtime);
		assertThat(written.getId()).isEqualTo(GTIN);
		assertThat(written.getVertical()).isEqualTo(vertical.getId());
		assertThat(written.getOffersCount()).isEqualTo(2);
		assertThat(written.getPrice()).isSameAs(realtime.getPrice());
		assertThat(written.getLastChange()).isEqualTo(UPDATED_AT);
	}

	@Test
	void partialUpdateQueuedDuringTheRunSurvivesIt() throws Exception {
		loadedProduct(GTIN);
		AggregatedPrices price = new AggregatedPrices();
		ProductPartialUpdateHolder partial = new ProductPartialUpdateHolder(GTIN);
		partial.addChange(ProductPartialUpdateField.LAST_CHANGE, UPDATED_AT);
		partial.addChange(ProductPartialUpdateField.PRICE, price);
		partial.addChange(ProductPartialUpdateField.OFFERS_COUNT, 2);
		duringSanitisation(() -> repository.addToPartialIndexationQueue(List.of(partial)));

		facade.sanitizeVertical(vertical);

		assertThat(repository.getPartialProductQueue()).containsExactly(partial);
		assertThat(fullQueue()).singleElement().satisfies(written -> {
			assertThat(written.getOffersCount()).isEqualTo(2);
			assertThat(written.getPrice()).isSameAs(price);
			assertThat(written.getLastChange()).isEqualTo(UPDATED_AT);
		});
	}

	@Test
	void updateIndexedDuringTheRunIsNotOverwritten() throws Exception {
		loadedProduct(GTIN);
		Product realtime = realtimeUpdate(UPDATED_AT);
		duringSanitisation(() -> indexed.put(GTIN, realtime));

		facade.sanitizeVertical(vertical);

		assertThat(fullQueue()).singleElement().satisfies(written -> {
			assertThat(written.getOffersCount()).isEqualTo(2);
			assertThat(written.getPrice()).isSameAs(realtime.getPrice());
			assertThat(written.getLastChange()).isEqualTo(UPDATED_AT);
		});
	}

	@Test
	void olderStateNeverReplacesTheLoadedOffers() throws Exception {
		Product product = loadedProduct(GTIN);
		Product stale = realtimeUpdate(LOADED_AT - 1);
		duringSanitisation(() -> {
			indexed.put(GTIN, stale);
			repository.addToFullindexationQueue(List.of(stale));
		});

		facade.sanitizeVertical(vertical);

		assertThat(fullQueue()).hasSize(2);
		assertThat(fullQueue().get(0)).isSameAs(stale);
		assertThat(fullQueue().get(1)).isSameAs(product);
		assertThat(product.getOffersCount()).isEqualTo(1);
		assertThat(product.getPrice()).isNotSameAs(stale.getPrice());
		assertThat(product.getLastChange()).isEqualTo(LOADED_AT);
	}

	@Test
	void productsAreStreamedAndQueuedByPagesWithTheNewestOffersAppliedFirst() throws Exception {
		LongStream.rangeClosed(1, BatchService.INDEXATION_PAGE_SIZE + 1L).forEach(this::loadedProduct);
		Product lastOfFirstPage = loaded.get(BatchService.INDEXATION_PAGE_SIZE - 1);
		Product firstOfSecondPage = loaded.get(BatchService.INDEXATION_PAGE_SIZE);

		facade.sanitizeVertical(vertical);

		// The first page is written before the next product is even aggregated: the vertical is never held whole
		InOrder order = inOrder(aggregator, repository);
		order.verify(aggregator).onProduct(lastOfFirstPage);
		order.verify(repository).applyNewerOfferStates(
				argThat(page -> page.size() == BatchService.INDEXATION_PAGE_SIZE && page.contains(lastOfFirstPage)));
		order.verify(repository).addToFullindexationQueue(
				argThat(page -> page.size() == BatchService.INDEXATION_PAGE_SIZE && page.contains(lastOfFirstPage)));
		order.verify(aggregator).onProduct(firstOfSecondPage);
		order.verify(repository).applyNewerOfferStates(List.of(firstOfSecondPage));
		order.verify(repository).addToFullindexationQueue(List.of(firstOfSecondPage));

		assertThat(fullQueue()).containsExactlyElementsOf(loaded);
		verify(repository, never()).index(any());
		assertThat(streamClosed).isTrue();
	}

	@Test
	void skippedProductIsNotWritten() throws Exception {
		Product skipped = loadedProduct(GTIN);
		Product kept = loadedProduct(OTHER_GTIN);
		doThrow(new AggregationSkipException("invalid barcode")).when(aggregator).onProduct(skipped);

		facade.sanitizeVertical(vertical);

		assertThat(fullQueue()).containsExactly(kept);
		verify(repository).applyNewerOfferStates(List.of(kept));
	}

	@Test
	void verticalWithoutProductsWritesNothing() {
		facade.sanitizeVertical(vertical);

		verify(repository, never()).applyNewerOfferStates(anyCollection());
		verify(repository, never()).addToFullindexationQueue(anyCollection());
		assertThat(streamClosed).isTrue();
	}

	@Test
	void failingVerticalDoesNotStopTheNextOnes() {
		VerticalConfig failing = new VerticalConfig();
		failing.setId("tv");
		VerticalConfig next = new VerticalConfig();
		next.setId("oven");
		when(verticalsConfigService.getConfigsWithoutDefault()).thenReturn(List.of(failing, next));
		doThrow(new IllegalStateException("Elasticsearch timeout")).when(facade).sanitizeVertical(failing);
		doNothing().when(facade).sanitizeVertical(next);

		facade.sanitizeAllVerticals();

		verify(facade).sanitizeVertical(next);
	}
}
