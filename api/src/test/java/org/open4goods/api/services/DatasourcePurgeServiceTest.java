package org.open4goods.api.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.open4goods.model.attribute.ProductAttribute;
import org.open4goods.model.attribute.SourcedAttribute;
import org.open4goods.model.price.AggregatedPrice;
import org.open4goods.model.price.AggregatedPrices;
import org.open4goods.model.price.Currency;
import org.open4goods.model.price.PriceHistory;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.ProductCondition;
import org.open4goods.services.productrepository.services.ProductRepository;

/**
 * Unit tests of {@link DatasourcePurgeService} (GOU-1592): offers and data of the purged datasource are removed,
 * other datasources are left intact, aggregated prices are recomputed, a second run touches nothing and a dry run
 * writes nothing.
 */
class DatasourcePurgeServiceTest {

	private static final String RAKUTEN = "rakuten.com";
	private static final String OTHER = "fnac.com";
	/** Another Rakuten-like datasource name, which must never be mistaken for the purged one. */
	private static final String RAKUTEN_FR = "Rakuten FR";

	private ProductRepository repository;
	private DatasourcePurgeService service;
	private List<Product> index;

	@BeforeEach
	void setUp() {
		repository = mock(ProductRepository.class);
		service = new DatasourcePurgeService(repository);
		index = new ArrayList<>();
		// A new stream on each call, over the same (in-memory) documents, as a scroll on the index would be
		when(repository.exportAll()).thenAnswer(invocation -> index.stream());
	}

	@Test
	void purgeRemovesOnlyTheDatasourceOffersAndRecomputesPrices() {
		Product mixed = mixedProduct(1L);
		Product rakutenOnly = product(2L, offer(RAKUTEN, 7.0, ProductCondition.OCCASION));
		rakutenOnly.getDatasourceCodes().put(RAKUTEN, 2L);
		rakutenOnly.getPrice().setMinPrice(rakutenOnly.getPrice().getOffers().iterator().next());
		rakutenOnly.setOffersCount(1);
		Product untouched = product(3L, offer(OTHER, 30.0, ProductCondition.NEW), offer(RAKUTEN_FR, 31.0, ProductCondition.NEW));
		untouched.getDatasourceCodes().put(OTHER, 3L);
		untouched.getDatasourceCodes().put(RAKUTEN_FR, 4L);
		index.addAll(List.of(mixed, rakutenOnly, untouched));

		DatasourcePurgeReport report = service.purge(RAKUTEN, false);

		assertThat(report.dryRun()).isFalse();
		assertThat(report.scannedProducts()).isEqualTo(3);
		assertThat(report.touchedProducts()).isEqualTo(2);
		assertThat(report.removedOffers()).isEqualTo(3);
		assertThat(report.savedProducts()).isEqualTo(2);
		assertThat(report.failedProducts()).isZero();
		assertThat(report.sampleGtins()).containsExactlyInAnyOrder(mixed.gtin(), rakutenOnly.gtin());

		assertThat(stored()).extracting(Product::getId).containsExactlyInAnyOrder(1L, 2L);

		// Mixed product: only the other datasources remain, best offer and counters recomputed
		assertThat(mixed.getPrice().getOffers()).extracting(AggregatedPrice::getDatasourceName)
				.containsExactlyInAnyOrder(OTHER, RAKUTEN_FR);
		assertThat(mixed.getPrice().getMinPrice().getDatasourceName()).isEqualTo(OTHER);
		assertThat(mixed.getPrice().getMinPrice().getPrice()).isEqualTo(20.0);
		assertThat(mixed.getOffersCount()).isEqualTo(2);
		assertThat(mixed.getPrice().getConditions()).containsExactly(ProductCondition.NEW);
		assertThat(mixed.getDatasourceCodes()).containsOnlyKeys(OTHER, RAKUTEN_FR);
		assertThat(mixed.getCategoriesByDatasources()).containsOnlyKeys(OTHER, RAKUTEN_FR);
		assertThat(mixed.getDatasourceCategories()).containsExactlyInAnyOrder("LIVRES", "SHARED");
		assertThat(mixed.getDescriptionsByDatasource()).containsOnlyKeys(OTHER);
		assertThat(mixed.getAkaBrands()).containsOnlyKeys(OTHER);
		assertThat(mixed.getAttributes().getAll()).containsOnlyKeys("AUTHOR");
		assertThat(mixed.getAttributes().getAll().get("AUTHOR").getSource())
				.extracting(SourcedAttribute::getDataSourcename).containsExactly(OTHER);
		// History is a daily best price, not an offer: kept as is
		assertThat(mixed.getPrice().getNewPricehistory()).hasSize(1);

		// Rakuten-only product: no offer left
		assertThat(rakutenOnly.getPrice().getOffers()).isEmpty();
		assertThat(rakutenOnly.getPrice().getMinPrice()).isNull();
		assertThat(rakutenOnly.getOffersCount()).isZero();
		assertThat(rakutenOnly.getPrice().getConditions()).isEmpty();
		assertThat(rakutenOnly.getDatasourceCodes()).isEmpty();

		// Product without Rakuten data: never written, nothing removed
		assertThat(untouched.getPrice().getOffers()).hasSize(2);
		assertThat(untouched.getDatasourceCodes()).containsOnlyKeys(OTHER, RAKUTEN_FR);
	}

	@Test
	void secondRunTouchesNoProduct() {
		index.addAll(List.of(mixedProduct(1L), product(2L, offer(OTHER, 5.0, ProductCondition.NEW))));

		service.purge(RAKUTEN, false);
		DatasourcePurgeReport second = service.purge(RAKUTEN, false);

		assertThat(second.scannedProducts()).isEqualTo(2);
		assertThat(second.touchedProducts()).isZero();
		assertThat(second.removedOffers()).isZero();
		assertThat(second.savedProducts()).isZero();
		assertThat(second.sampleGtins()).isEmpty();
		// Only the first run wrote, and only the product that held Rakuten data
		verify(repository, times(1)).store(anyCollection());
	}

	@Test
	void dryRunReportsWithoutWriting() {
		index.addAll(List.of(mixedProduct(1L), product(2L, offer(OTHER, 5.0, ProductCondition.NEW))));

		DatasourcePurgeReport report = service.purge(RAKUTEN, true);

		assertThat(report.dryRun()).isTrue();
		assertThat(report.scannedProducts()).isEqualTo(2);
		assertThat(report.touchedProducts()).isEqualTo(1);
		assertThat(report.removedOffers()).isEqualTo(2);
		assertThat(report.savedProducts()).isZero();
		assertThat(report.sampleGtins()).containsExactly(index.getFirst().gtin());
		verify(repository, never()).store(anyCollection());
		verify(repository, never()).store(anyCollection(), anyString());
		verify(repository, never()).index(any());
		verify(repository, never()).forceIndex(any());
		verify(repository, never()).addToFullindexationQueue(anyCollection());
		verify(repository, never()).addToPartialIndexationQueue(anyCollection());
		verify(repository, never()).bulkUpdateDocument(anyCollection());
	}

	@Test
	void writesByBulksAndCapsSamples() {
		int count = DatasourcePurgeService.SAVE_BATCH_SIZE + 1;
		for (long id = 1; id <= count; id++) {
			Product p = product(id, offer(RAKUTEN, 10.0 + id, ProductCondition.NEW));
			index.add(p);
		}

		DatasourcePurgeReport report = service.purge(RAKUTEN, false);

		assertThat(report.touchedProducts()).isEqualTo(count);
		assertThat(report.savedProducts()).isEqualTo(count);
		assertThat(report.sampleGtins()).hasSize(DatasourcePurgeService.SAMPLE_GTINS_SIZE);
		verify(repository, times(2)).store(anyCollection());
	}

	@Test
	void blankDatasourceIsRejected() {
		assertThatThrownBy(() -> service.purge(" ", true)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> service.start(null, true)).isInstanceOf(IllegalArgumentException.class);
		verify(repository, never()).exportAll();
	}

	@Test
	void backgroundRunReportsCompletion() throws InterruptedException {
		index.add(mixedProduct(1L));

		DatasourcePurgeStatus running = service.start(RAKUTEN, true);
		assertThat(running.state()).isIn(DatasourcePurgeStatus.State.RUNNING, DatasourcePurgeStatus.State.COMPLETED);

		Instant deadline = Instant.now().plus(Duration.ofSeconds(10));
		while (service.getStatus().state() == DatasourcePurgeStatus.State.RUNNING && Instant.now().isBefore(deadline)) {
			Thread.sleep(20);
		}

		DatasourcePurgeStatus done = service.getStatus();
		assertThat(done.state()).isEqualTo(DatasourcePurgeStatus.State.COMPLETED);
		assertThat(done.datasourceName()).isEqualTo(RAKUTEN);
		assertThat(done.dryRun()).isTrue();
		assertThat(done.report().touchedProducts()).isEqualTo(1);
		verify(repository, never()).store(anyCollection());
	}

	@SuppressWarnings("unchecked")
	private List<Product> stored() {
		ArgumentCaptor<Collection<Product>> captor = ArgumentCaptor.forClass(Collection.class);
		verify(repository, atLeastOnce()).store(captor.capture());
		return captor.getAllValues().stream().flatMap(Collection::stream).collect(Collectors.toList());
	}

	/**
	 * A book sold by Rakuten (cheapest, new and used) and by two other datasources, with Rakuten data in every
	 * datasource-keyed field.
	 */
	private static Product mixedProduct(long id) {
		Product p = product(id,
				offer(RAKUTEN, 9.0, ProductCondition.NEW),
				offer(RAKUTEN, 4.0, ProductCondition.OCCASION),
				offer(OTHER, 20.0, ProductCondition.NEW),
				offer(RAKUTEN_FR, 21.0, ProductCondition.NEW));
		AggregatedPrice rakutenUsed = p.getPrice().getOffers().stream()
				.filter(o -> o.getPrice() == 4.0).findFirst().orElseThrow();
		p.getPrice().setMinPrice(rakutenUsed);
		p.getPrice().setConditions(new HashSet<>(Set.of(ProductCondition.NEW, ProductCondition.OCCASION)));
		p.getPrice().setNewPricehistory(new ArrayList<>(List.of(new PriceHistory(rakutenUsed))));
		p.setOffersCount(4);

		p.getDatasourceCodes().putAll(Map.of(RAKUTEN, 1L, OTHER, 2L, RAKUTEN_FR, 3L));
		p.getCategoriesByDatasources().putAll(Map.of(RAKUTEN, "SHARED", OTHER, "LIVRES"));
		p.getDatasourceCategories().addAll(Set.of("SHARED", "LIVRES"));
		// "SHARED" is also mapped by another datasource below, it must stay
		p.getCategoriesByDatasources().put(RAKUTEN_FR, "SHARED");
		p.getDescriptionsByDatasource().putAll(Map.of(RAKUTEN, "desc rakuten", OTHER, "desc other"));
		p.getAkaBrands().putAll(Map.of(RAKUTEN, "GALLIMARD", OTHER, "FOLIO"));

		ProductAttribute author = new ProductAttribute();
		author.setName("AUTHOR");
		author.setValue("CAMUS");
		author.getSource().add(source(RAKUTEN, "CAMUS"));
		author.getSource().add(source(OTHER, "CAMUS"));
		p.getAttributes().getAll().put("AUTHOR", author);

		ProductAttribute rakutenOnly = new ProductAttribute();
		rakutenOnly.setName("TYPE DE PRODUIT");
		rakutenOnly.setValue("LIVRE");
		rakutenOnly.getSource().add(source(RAKUTEN, "LIVRE"));
		p.getAttributes().getAll().put("TYPE DE PRODUIT", rakutenOnly);
		return p;
	}

	private static Product product(long id, AggregatedPrice... offers) {
		Product p = new Product(id);
		AggregatedPrices prices = new AggregatedPrices();
		// Distinct prices: AggregatedPrice equality is (currency, price)
		prices.setOffers(new HashSet<>(List.of(offers)));
		p.setPrice(prices);
		p.setOffersCount(offers.length);
		return p;
	}

	private static AggregatedPrice offer(String datasource, double price, ProductCondition condition) {
		AggregatedPrice offer = new AggregatedPrice();
		offer.setDatasourceName(datasource);
		offer.setUrl("https://" + datasource + ".example/offer/" + price);
		offer.setOfferName(datasource + " offer");
		offer.setPrice(price);
		offer.setCurrency(Currency.EUR);
		offer.setProductState(condition);
		offer.setTimeStamp(System.currentTimeMillis());
		return offer;
	}

	private static SourcedAttribute source(String datasource, String value) {
		SourcedAttribute s = new SourcedAttribute();
		s.setDataSourcename(datasource);
		s.setValue(value);
		return s;
	}
}
