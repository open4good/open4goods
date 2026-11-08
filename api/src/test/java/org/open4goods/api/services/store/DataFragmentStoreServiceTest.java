package org.open4goods.api.services.store;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.open4goods.api.services.AggregationFacadeService;
import org.open4goods.commons.services.BarcodeValidationService;
import org.open4goods.model.StandardiserService;
import org.open4goods.model.attribute.ReferentielKey;
import org.open4goods.model.datafragment.DataFragment;
import org.open4goods.model.eprel.EprelProduct;
import org.open4goods.model.product.Product;
import org.open4goods.services.productrepository.config.IndexationConfig;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.springframework.data.elasticsearch.ElasticsearchErrorCause;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.MultiGetItem;
import org.springframework.data.elasticsearch.core.query.Query;

/**
 * Tests {@link DataFragmentStoreService#aggregateAndstore} against a real
 * {@link ProductRepository} backed by mocked Elasticsearch operations.
 *
 * <p>GOU-1700: in the night of 04/11/2026, established products (Electrolux
 * 7333394145372, Beko 8690842629433) were rewritten from a single offer
 * fragment: new {@code creationDate}, EPREL data, scores and Icecat sources lost.
 *
 * <p>GOU-1841: since v1.0.80, 411 books (ISBN-13 ids) were recreated every night,
 * because a feed published their ISBN-10 ({@code 2726862217}): the lookup used
 * {@code Long.valueOf} of the raw GTIN while the identity aggregation indexed the
 * product under its ISBN-13 ({@code 9782726862216}).
 */
class DataFragmentStoreServiceTest {

	private static final long GTIN = 7333394145372L;
	private static final long CREATION_DATE = 1_700_000_000_000L;
	/** "Revue technique automobile", published as ISBN-10 2726862217 by haynes.com. */
	private static final long ISBN_13 = 9782726862216L;
	private static final String ISBN_10 = "2726862217";

	private ElasticsearchOperations elasticsearchOperations;
	private AggregationFacadeService generationService;
	private ProductRepository repository;
	private DataFragmentStoreService service;

	@BeforeEach
	void setUp() throws Exception {
		elasticsearchOperations = mock(ElasticsearchOperations.class);
		generationService = mock(AggregationFacadeService.class);
		when(generationService.updateOne(any(), any())).thenAnswer(inv -> inv.getArgument(1));

		IndexationConfig indexationConfig = new IndexationConfig();
		indexationConfig.setProductWorkers(0);
		indexationConfig.setPartialProductWorkers(0);
		indexationConfig.setDataFragmentworkers(0);

		repository = new ProductRepository(indexationConfig, elasticsearchOperations);
		service = new DataFragmentStoreService(mock(StandardiserService.class), generationService, repository,
				new BarcodeValidationService(), indexationConfig);
	}

	private static Product existingProduct() {
		Product product = new Product();
		product.setId(GTIN);
		product.setCreationDate(CREATION_DATE);
		product.setVertical("dishwasher");
		product.getAttributes().getReferentielAttributes().put(ReferentielKey.GTIN, String.valueOf(GTIN));
		product.getDatasourceCodes().put("eprel", 1L);
		product.getDatasourceCodes().put("icecat.biz", 2L);
		product.setEprelDatas(new EprelProduct());
		return product;
	}

	private static DataFragment offer(String gtin) {
		DataFragment fragment = new DataFragment();
		fragment.setDatasourceName("manomano.fr");
		fragment.addReferentielAttribute(ReferentielKey.GTIN, gtin);
		fragment.setFragmentHashCode(42);
		return fragment;
	}

	private void elasticReturns(MultiGetItem<Product> item) {
		when(elasticsearchOperations.multiGet(any(Query.class), eq(Product.class), eq(ProductRepository.CURRENT_INDEX)))
				.thenReturn(List.of(item));
	}

	/**
	 * Behaves like Elasticsearch: a multi-get only returns the stored products whose
	 * id is asked for.
	 */
	private void elasticStores(Product... products) {
		when(elasticsearchOperations.multiGet(any(Query.class), eq(Product.class), eq(ProductRepository.CURRENT_INDEX)))
				.thenAnswer(inv -> {
					Collection<String> ids = inv.getArgument(0, Query.class).getIds();
					return ids.stream()
							.map(id -> List.of(products).stream().filter(p -> String.valueOf(p.getId()).equals(id))
									.findFirst().map(p -> MultiGetItem.of(p, null))
									.orElseGet(() -> MultiGetItem.of(null, null)))
							.toList();
				});
	}

	private static Product existingBook() {
		Product product = existingProduct();
		product.setId(ISBN_13);
		product.setVertical(null);
		product.getAttributes().getReferentielAttributes().put(ReferentielKey.GTIN, String.valueOf(ISBN_13));
		return product;
	}

	private List<Product> queuedFullProducts() {
		return new ArrayList<>(repository.getFullProductQueue());
	}

	@Test
	void offerWithLeadingZeroGtinUpdatesTheExistingProduct() throws Exception {
		Product existing = existingProduct();
		elasticReturns(MultiGetItem.of(existing, null));

		// A feed publishes the GTIN-14 form of the EAN-13 ("0" + 13 digits)
		service.aggregateAndstore(List.of(offer("07333394145372")));

		ArgumentCaptor<Product> aggregated = ArgumentCaptor.forClass(Product.class);
		verify(generationService).updateOne(any(), aggregated.capture());
		assertThat(aggregated.getValue()).isSameAs(existing);

		assertThat(queuedFullProducts()).singleElement().satisfies(p -> {
			assertThat(p.getCreationDate()).isEqualTo(CREATION_DATE);
			assertThat(p.getEprelDatas()).isNotNull();
			assertThat(p.getVertical()).isEqualTo("dishwasher");
			assertThat(p.getDatasourceCodes()).containsKeys("eprel", "icecat.biz", "manomano.fr");
		});
	}

	@Test
	void failedLookupNeverRecreatesTheProduct() throws Exception {
		ElasticsearchErrorCause cause = new ElasticsearchErrorCause("es_rejected_execution_exception", "rejected", null,
				null, List.of(), List.of());
		elasticReturns(MultiGetItem.of(null,
				MultiGetItem.Failure.of(ProductRepository.MAIN_INDEX_NAME, null, String.valueOf(GTIN), null, cause)));

		service.aggregateAndstore(List.of(offer(String.valueOf(GTIN))));

		verify(generationService, never()).updateOne(any(), any());
		assertThat(queuedFullProducts()).isEmpty();
	}

	@Test
	void absentProductIsCreatedOnceForSeveralFragmentsOfTheSameBuffer() throws Exception {
		elasticReturns(MultiGetItem.of(null, null));

		DataFragment fnac = offer(String.valueOf(GTIN));
		fnac.setDatasourceName("fnac.com");
		long before = System.currentTimeMillis();
		service.aggregateAndstore(List.of(offer("07333394145372"), fnac));

		ArgumentCaptor<Product> aggregated = ArgumentCaptor.forClass(Product.class);
		verify(generationService, times(2)).updateOne(any(), aggregated.capture());
		assertThat(aggregated.getAllValues().get(0)).isSameAs(aggregated.getAllValues().get(1));
		assertThat(aggregated.getValue().getCreationDate()).isGreaterThanOrEqualTo(before);
		assertThat(aggregated.getValue().getDatasourceCodes()).containsKeys("manomano.fr", "fnac.com");
	}

	@Test
	void isbn10FragmentUpdatesTheExistingIsbn13Product() throws Exception {
		Product existing = existingBook();
		elasticStores(existing);

		service.aggregateAndstore(List.of(offer(ISBN_10)));

		ArgumentCaptor<Product> aggregated = ArgumentCaptor.forClass(Product.class);
		verify(generationService).updateOne(any(), aggregated.capture());
		assertThat(aggregated.getValue()).isSameAs(existing);
		assertThat(queuedFullProducts()).singleElement().satisfies(p -> {
			assertThat(p.getCreationDate()).isEqualTo(CREATION_DATE);
			assertThat(p.getDatasourceCodes()).containsKeys("eprel", "icecat.biz", "manomano.fr");
		});
	}

	@Test
	void isbn10AndIsbn13FragmentsOfTheSameBufferEnrichTheExistingProduct() throws Exception {
		Product existing = existingBook();
		elasticStores(existing);

		DataFragment oscaro = offer(String.valueOf(ISBN_13));
		oscaro.setDatasourceName("oscaro.com");
		service.aggregateAndstore(List.of(offer(ISBN_10), oscaro));

		ArgumentCaptor<Product> aggregated = ArgumentCaptor.forClass(Product.class);
		verify(generationService, times(2)).updateOne(any(), aggregated.capture());
		assertThat(aggregated.getAllValues()).allSatisfy(p -> assertThat(p).isSameAs(existing));
		assertThat(existing.getCreationDate()).isEqualTo(CREATION_DATE);
	}

	@Test
	void absentBookIsCreatedOnceForItsIsbn10AndIsbn13Fragments() throws Exception {
		elasticStores();

		DataFragment oscaro = offer(String.valueOf(ISBN_13));
		oscaro.setDatasourceName("oscaro.com");
		service.aggregateAndstore(List.of(offer(ISBN_10), oscaro));

		ArgumentCaptor<Product> aggregated = ArgumentCaptor.forClass(Product.class);
		verify(generationService, times(2)).updateOne(any(), aggregated.capture());
		assertThat(aggregated.getAllValues().get(0)).isSameAs(aggregated.getAllValues().get(1));
	}

	/**
	 * UPC-A ({@code 036000291452}), its EAN-13 ({@code 0036000291452}), its GTIN-14
	 * and its numeric id: the sanitisation only adds or drops leading zeros, the id
	 * stays the same.
	 */
	@ParameterizedTest
	@ValueSource(strings = { "036000291452", "0036000291452", "00036000291452", "36000291452" })
	void upcAFragmentWithOrWithoutLeadingZerosUpdatesTheExistingProduct(String gtin) throws Exception {
		Product existing = existingProduct();
		existing.setId(36000291452L);
		elasticStores(existing);

		service.aggregateAndstore(List.of(offer(gtin)));

		ArgumentCaptor<Product> aggregated = ArgumentCaptor.forClass(Product.class);
		verify(generationService).updateOne(any(), aggregated.capture());
		assertThat(aggregated.getValue()).isSameAs(existing);
	}

	@Test
	void ean8FragmentUpdatesTheExistingProduct() throws Exception {
		Product existing = existingProduct();
		existing.setId(40170725L);
		elasticStores(existing);

		service.aggregateAndstore(List.of(offer("40170725")));

		ArgumentCaptor<Product> aggregated = ArgumentCaptor.forClass(Product.class);
		verify(generationService).updateOne(any(), aggregated.capture());
		assertThat(aggregated.getValue()).isSameAs(existing);
	}

	@Test
	void fragmentWithAnInvalidBarcodeIsSkipped() throws Exception {
		elasticStores();

		// Wrong check digit
		service.aggregateAndstore(List.of(offer("7333394145373")));

		verify(generationService, never()).updateOne(any(), any());
		verify(elasticsearchOperations, never()).multiGet(any(Query.class), eq(Product.class),
				eq(ProductRepository.CURRENT_INDEX));
		assertThat(queuedFullProducts()).isEmpty();
	}
}
