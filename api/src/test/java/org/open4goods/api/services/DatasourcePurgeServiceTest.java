package org.open4goods.api.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Spliterators;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.open4goods.model.attribute.ProductAttribute;
import org.open4goods.model.attribute.SourcedAttribute;
import org.open4goods.model.price.AggregatedPrice;
import org.open4goods.model.price.AggregatedPrices;
import org.open4goods.model.price.Currency;
import org.open4goods.model.price.PriceHistory;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.ProductCondition;
import org.open4goods.model.product.ProductPartialUpdateHolder;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.BulkFailureException;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

/**
 * Unit tests of {@link DatasourcePurgeService} (GOU-1592): offers and data of the purged datasource are removed,
 * other datasources are left intact, aggregated prices are recomputed, a second run touches nothing and a dry run
 * writes nothing. Progress lines and the persisted status (COMPLETED, FAILED, INTERRUPTED after a restart) are
 * covered too (GOU-1815). Scope OFFERS (GOU-2101) removes only the offers, by partial updates of the prices.
 */
class DatasourcePurgeServiceTest {

	private static final String RAKUTEN = "rakuten.com";
	private static final String OTHER = "fnac.com";
	/** Another Rakuten-like datasource name, which must never be mistaken for the purged one. */
	private static final String RAKUTEN_FR = "Rakuten FR";

	private static final long PROGRESS_INTERVAL = 50_000;

	@TempDir
	Path tempDir;

	private ProductRepository repository;
	private DatasourcePurgeService service;
	private List<Product> index;
	private Path statusFile;

	@BeforeEach
	void setUp() {
		repository = mock(ProductRepository.class);
		statusFile = tempDir.resolve("reports").resolve("datasource-purge-status.json");
		service = new DatasourcePurgeService(repository, statusFile, PROGRESS_INTERVAL);
		index = new ArrayList<>();
		// A new stream on each call, over the same (in-memory) documents, as a scroll on the index would be
		when(repository.exportAll()).thenAnswer(invocation -> index.stream());
		when(repository.exportAll(any(String[].class), anyInt())).thenAnswer(invocation -> index.stream());
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

		DatasourcePurgeReport report = service.purge(RAKUTEN, DatasourcePurgeScope.ALL, false);

		assertThat(report.dryRun()).isFalse();
		assertThat(report.scannedProducts()).isEqualTo(3);
		assertThat(report.touchedProducts()).isEqualTo(2);
		assertThat(report.removedOffers()).isEqualTo(3);
		assertThat(report.savedProducts()).isEqualTo(2);
		assertThat(report.failedProducts()).isZero();
		assertThat(report.sampleGtins()).containsExactlyInAnyOrder(mixed.gtin(), rakutenOnly.gtin());

		assertThat(stored()).extracting(Product::getId).containsExactlyInAnyOrder(1L, 2L);
		// ALL keeps its GOU-1592 behaviour: complete products stored, no partial update
		assertThat(report.scope()).isEqualTo(DatasourcePurgeScope.ALL);
		verify(repository, never()).bulkUpdateDocument(anyCollection());

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

		service.purge(RAKUTEN, DatasourcePurgeScope.ALL, false);
		DatasourcePurgeReport second = service.purge(RAKUTEN, DatasourcePurgeScope.ALL, false);

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

		DatasourcePurgeReport report = service.purge(RAKUTEN, DatasourcePurgeScope.ALL, true);

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

		DatasourcePurgeReport report = service.purge(RAKUTEN, DatasourcePurgeScope.ALL, false);

		assertThat(report.touchedProducts()).isEqualTo(count);
		assertThat(report.savedProducts()).isEqualTo(count);
		assertThat(report.sampleGtins()).hasSize(DatasourcePurgeService.SAMPLE_GTINS_SIZE);
		verify(repository, times(2)).store(anyCollection());
	}

	@Test
	void blankDatasourceIsRejected() {
		assertThatThrownBy(() -> service.purge(" ", DatasourcePurgeScope.OFFERS, true)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> service.start(null, DatasourcePurgeScope.OFFERS, true)).isInstanceOf(IllegalArgumentException.class);
		verify(repository, never()).exportAll();
		verify(repository, never()).exportAll(any(String[].class), anyInt());
	}

	@Test
	void backgroundRunReportsCompletion() throws InterruptedException {
		index.add(mixedProduct(1L));

		DatasourcePurgeStatus running = service.start(RAKUTEN, DatasourcePurgeScope.ALL, true);
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

	@Test
	void progressLineHoldsCountersAndSampleGtins() {
		DatasourcePurgeService everyTwo = new DatasourcePurgeService(repository, statusFile, 2);
		index.addAll(List.of(product(1L, offer(RAKUTEN, 1.0, ProductCondition.NEW)),
				product(2L, offer(RAKUTEN, 2.0, ProductCondition.NEW)),
				product(3L, offer(OTHER, 3.0, ProductCondition.NEW))));

		Logger logger = (Logger) LoggerFactory.getLogger(DatasourcePurgeService.class);
		Level level = logger.getLevel();
		logger.setLevel(Level.INFO);
		ListAppender<ILoggingEvent> appender = new ListAppender<>();
		appender.start();
		logger.addAppender(appender);
		try {
			everyTwo.purge(RAKUTEN, DatasourcePurgeScope.ALL, true);
		} finally {
			logger.detachAppender(appender);
			logger.setLevel(level);
		}

		List<String> progress = appender.list.stream().map(ILoggingEvent::getFormattedMessage)
				.filter(message -> message.contains("products scanned")).toList();
		assertThat(progress).containsExactly("Purge of datasource rakuten.com (scope=ALL, dryRun=true): 2 products scanned, "
				+ "2 touched, 2 offers removed, 0 saved, 0 failed, sample GTINs [" + new Product(1L).gtin() + ", "
				+ new Product(2L).gtin() + "]");
	}

	@Test
	void completedRunIsWrittenToTheStatusFile() throws Exception {
		index.add(mixedProduct(1L));

		service.start(RAKUTEN, DatasourcePurgeScope.ALL, true);
		DatasourcePurgeStatus done = awaitStatus(service, s -> s.state() != DatasourcePurgeStatus.State.RUNNING);

		assertThat(done.state()).isEqualTo(DatasourcePurgeStatus.State.COMPLETED);
		assertThat(Files.readString(statusFile, StandardCharsets.UTF_8)).contains("\"COMPLETED\"");
		assertThat(statusFile.resolveSibling(statusFile.getFileName() + ".tmp")).doesNotExist();

		// Read back by a restarted api: the last report is still there
		DatasourcePurgeStatus readBack = new DatasourcePurgeService(repository, statusFile, PROGRESS_INTERVAL).getStatus();
		assertThat(readBack).isEqualTo(done);
		assertThat(readBack.report().touchedProducts()).isEqualTo(1);
		assertThat(readBack.report().removedOffers()).isEqualTo(2);
		assertThat(readBack.report().sampleGtins()).containsExactly(new Product(1L).gtin());
		assertThat(readBack.finishedAt()).isNotNull();
	}

	@Test
	void failedRunIsWrittenToTheStatusFile() throws Exception {
		when(repository.exportAll()).thenThrow(new IllegalStateException("index unavailable"));

		service.start(RAKUTEN, DatasourcePurgeScope.ALL, true);
		DatasourcePurgeStatus done = awaitStatus(service, s -> s.state() != DatasourcePurgeStatus.State.RUNNING);

		assertThat(done.state()).isEqualTo(DatasourcePurgeStatus.State.FAILED);
		assertThat(done.error()).contains("index unavailable");
		assertThat(Files.readString(statusFile, StandardCharsets.UTF_8)).contains("\"FAILED\"");
		DatasourcePurgeStatus readBack = new DatasourcePurgeService(repository, statusFile, PROGRESS_INTERVAL).getStatus();
		assertThat(readBack.state()).isEqualTo(DatasourcePurgeStatus.State.FAILED);
		assertThat(readBack.error()).contains("index unavailable");
	}

	@Test
	void runningStatusReadAtStartupBecomesInterruptedAndANewRunIsAccepted() throws Exception {
		List<Product> products = List.of(product(1L, offer(RAKUTEN, 1.0, ProductCondition.NEW)),
				product(2L, offer(RAKUTEN, 2.0, ProductCondition.NEW)),
				product(3L, offer(RAKUTEN, 3.0, ProductCondition.NEW)));
		CountDownLatch blocked = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		// The scroll stalls after 2 products, once the progress step of the second one is written
		when(repository.exportAll()).thenReturn(stallingStream(products, 2, blocked, release));
		DatasourcePurgeService stopped = new DatasourcePurgeService(repository, statusFile, 2);
		stopped.start(RAKUTEN, DatasourcePurgeScope.ALL, true);
		assertThat(blocked.await(10, TimeUnit.SECONDS)).isTrue();

		DatasourcePurgeService restarted = new DatasourcePurgeService(repository, statusFile, 2);

		DatasourcePurgeStatus interrupted = restarted.getStatus();
		assertThat(interrupted.state()).isEqualTo(DatasourcePurgeStatus.State.INTERRUPTED);
		assertThat(interrupted.datasourceName()).isEqualTo(RAKUTEN);
		assertThat(interrupted.finishedAt()).isNull();
		assertThat(interrupted.lastProgressAt()).isAfterOrEqualTo(interrupted.startedAt());
		assertThat(interrupted.report().scannedProducts()).isEqualTo(2);
		assertThat(interrupted.report().touchedProducts()).isEqualTo(2);
		assertThat(interrupted.report().removedOffers()).isEqualTo(2);
		assertThat(interrupted.report().sampleGtins()).containsExactly(new Product(1L).gtin(), new Product(2L).gtin());
		assertThat(Files.readString(statusFile, StandardCharsets.UTF_8)).contains("\"INTERRUPTED\"");

		// The old run ends before the new one starts, as the old api would be gone
		release.countDown();
		awaitStatus(stopped, s -> s.state() != DatasourcePurgeStatus.State.RUNNING);

		// A new run is accepted after INTERRUPTED: it starts over, it does not resume
		when(repository.exportAll()).thenAnswer(invocation -> products.stream());
		DatasourcePurgeStatus running = restarted.start(RAKUTEN, DatasourcePurgeScope.ALL, true);
		assertThat(running.state()).isEqualTo(DatasourcePurgeStatus.State.RUNNING);
		assertThat(running.startedAt()).isAfter(interrupted.startedAt());
		DatasourcePurgeStatus done = awaitStatus(restarted, s -> s.state() != DatasourcePurgeStatus.State.RUNNING);
		assertThat(done.state()).isEqualTo(DatasourcePurgeStatus.State.COMPLETED);
		assertThat(done.report().scannedProducts()).isEqualTo(3);
		assertThat(new DatasourcePurgeService(repository, statusFile, 2).getStatus()).isEqualTo(done);
	}

	@Test
	void unreadableStatusFileStartsIdle() throws Exception {
		Files.createDirectories(statusFile.getParent());
		Files.writeString(statusFile, "{not json", StandardCharsets.UTF_8);

		assertThat(new DatasourcePurgeService(repository, statusFile, PROGRESS_INTERVAL).getStatus())
				.isEqualTo(DatasourcePurgeStatus.idle());
	}

	@Test
	void offersScopeRemovesOnlyOffersAndWritesPricesByPartialUpdate() {
		Product mixed = mixedProduct(1L);
		Product untouched = product(3L, offer(OTHER, 30.0, ProductCondition.NEW));
		untouched.getDatasourceCodes().put(OTHER, 3L);
		index.addAll(List.of(mixed, untouched));

		DatasourcePurgeReport report = service.purge(RAKUTEN, DatasourcePurgeScope.OFFERS, false);

		assertThat(report.scope()).isEqualTo(DatasourcePurgeScope.OFFERS);
		assertThat(report.scannedProducts()).isEqualTo(2);
		assertThat(report.touchedProducts()).isEqualTo(1);
		assertThat(report.removedOffers()).isEqualTo(2);
		assertThat(report.savedProducts()).isEqualTo(1);
		assertThat(report.failedProducts()).isZero();
		assertThat(report.sampleGtins()).containsExactly(mixed.gtin());

		// Only the prices are read, and written by partial update: never a complete product
		verify(repository).exportAll(DatasourcePurgeService.OFFERS_FIELDS, ProductRepository.FULL_PRODUCT_PAGE_SIZE);
		verify(repository, never()).exportAll();
		verify(repository, never()).store(anyCollection());
		List<ProductPartialUpdateHolder> updates = partialUpdates();
		assertThat(updates).singleElement().satisfies(update -> {
			assertThat(update.getProductId()).isEqualTo(1L);
			assertThat(update.getChanges()).containsOnlyKeys("price", "offersCount");
			assertThat(update.getChanges().get("price")).isSameAs(mixed.getPrice());
			assertThat(update.getChanges().get("offersCount")).isEqualTo(2);
		});

		// Offers of the other datasources remain, best offer and counters recomputed
		assertThat(mixed.getPrice().getOffers()).extracting(AggregatedPrice::getDatasourceName)
				.containsExactlyInAnyOrder(OTHER, RAKUTEN_FR);
		assertThat(mixed.getPrice().getMinPrice().getDatasourceName()).isEqualTo(OTHER);
		assertThat(mixed.getPrice().getMinPrice().getPrice()).isEqualTo(20.0);
		assertThat(mixed.getOffersCount()).isEqualTo(2);
		assertThat(mixed.getPrice().getConditions()).containsExactly(ProductCondition.NEW);
		assertThat(mixed.getPrice().getNewPricehistory()).hasSize(1);

		// The product sheet data of the datasource stays
		assertThat(mixed.getDatasourceCodes()).containsOnlyKeys(RAKUTEN, OTHER, RAKUTEN_FR);
		assertThat(mixed.getCategoriesByDatasources()).containsOnlyKeys(RAKUTEN, OTHER, RAKUTEN_FR);
		assertThat(mixed.getDescriptionsByDatasource()).containsOnlyKeys(RAKUTEN, OTHER);
		assertThat(mixed.getAkaBrands()).containsOnlyKeys(RAKUTEN, OTHER);
		assertThat(mixed.getAttributes().getAll()).containsOnlyKeys("AUTHOR", "TYPE DE PRODUIT");
		assertThat(mixed.getAttributes().getAll().get("AUTHOR").getSource())
				.extracting(SourcedAttribute::getDataSourcename).containsExactlyInAnyOrder(RAKUTEN, OTHER);
	}

	@Test
	void offersScopeIgnoresProductsWithOnlySheetDataOfTheDatasource() {
		Product sheetOnly = mixedProduct(1L);
		// Same Rakuten sheet data (codes, attributes, categories, description, brand alias), but no Rakuten offer
		sheetOnly.getPrice().getOffers().removeIf(offer -> RAKUTEN.equals(offer.getDatasourceName()));
		sheetOnly.getPrice().setMinPrice(sheetOnly.getPrice().getOffers().stream()
				.filter(offer -> OTHER.equals(offer.getDatasourceName())).findFirst().orElseThrow());
		Product codesOnly = new Product(2L);
		codesOnly.getDatasourceCodes().put(RAKUTEN, 2L);
		index.addAll(List.of(sheetOnly, codesOnly));

		DatasourcePurgeReport report = service.purge(RAKUTEN, DatasourcePurgeScope.OFFERS, false);

		assertThat(report.scannedProducts()).isEqualTo(2);
		assertThat(report.touchedProducts()).isZero();
		assertThat(report.savedProducts()).isZero();
		assertThat(report.sampleGtins()).isEmpty();
		verify(repository, never()).bulkUpdateDocument(anyCollection());
		verify(repository, never()).store(anyCollection());
		assertThat(sheetOnly.getDatasourceCodes()).containsKey(RAKUTEN);
		assertThat(sheetOnly.getAttributes().getAll()).containsKey("TYPE DE PRODUIT");
		assertThat(codesOnly.getDatasourceCodes()).containsOnlyKeys(RAKUTEN);
	}

	@Test
	void offersScopeClearsAStaleBestOfferOfTheDatasource() {
		Product stale = product(1L, offer(OTHER, 12.0, ProductCondition.NEW));
		// Best offer still pointing to an offer of the datasource no longer in price.offers
		stale.getPrice().setMinPrice(offer(RAKUTEN, 8.0, ProductCondition.NEW));
		index.add(stale);

		DatasourcePurgeReport report = service.purge(RAKUTEN, DatasourcePurgeScope.OFFERS, false);

		assertThat(report.touchedProducts()).isEqualTo(1);
		assertThat(report.removedOffers()).isZero();
		assertThat(report.savedProducts()).isEqualTo(1);
		assertThat(stale.getPrice().getMinPrice().getDatasourceName()).isEqualTo(OTHER);
		assertThat(stale.getOffersCount()).isEqualTo(1);
		assertThat(partialUpdates()).extracting(ProductPartialUpdateHolder::getProductId).containsExactly(1L);
	}

	@Test
	void secondOffersRunTouchesNoProduct() {
		index.addAll(List.of(mixedProduct(1L), product(2L, offer(OTHER, 5.0, ProductCondition.NEW))));

		service.purge(RAKUTEN, DatasourcePurgeScope.OFFERS, false);
		DatasourcePurgeReport second = service.purge(RAKUTEN, DatasourcePurgeScope.OFFERS, false);

		assertThat(second.scannedProducts()).isEqualTo(2);
		assertThat(second.touchedProducts()).isZero();
		assertThat(second.removedOffers()).isZero();
		assertThat(second.savedProducts()).isZero();
		verify(repository, times(1)).bulkUpdateDocument(anyCollection());
	}

	@Test
	void offersDryRunWritesNothing() {
		index.add(mixedProduct(1L));

		DatasourcePurgeReport report = service.purge(RAKUTEN, DatasourcePurgeScope.OFFERS, true);

		assertThat(report.touchedProducts()).isEqualTo(1);
		assertThat(report.removedOffers()).isEqualTo(2);
		assertThat(report.savedProducts()).isZero();
		verify(repository, never()).bulkUpdateDocument(anyCollection());
		verify(repository, never()).store(anyCollection());
	}

	@Test
	void failedDocumentsOfABulkAreCountedAndNotRetried() {
		int count = DatasourcePurgeService.SAVE_BATCH_SIZE + 1;
		for (long id = 1; id <= count; id++) {
			index.add(product(id, offer(RAKUTEN, 10.0 + id, ProductCondition.NEW)));
		}
		// The first bulk loses one document (deleted since the scroll read it), the second one succeeds
		doThrow(new BulkFailureException("1 failed", Map.of("7",
				new BulkFailureException.FailureDetails(404, "document_missing_exception"))))
				.doNothing()
				.when(repository).bulkUpdateDocument(anyCollection());

		DatasourcePurgeReport report = service.purge(RAKUTEN, DatasourcePurgeScope.OFFERS, false);

		assertThat(report.touchedProducts()).isEqualTo(count);
		assertThat(report.savedProducts()).isEqualTo(count - 1);
		assertThat(report.failedProducts()).isEqualTo(1);
		assertThat(partialUpdates()).hasSize(count);
	}

	@Test
	void backgroundRunReportsItsScope() throws InterruptedException {
		index.add(mixedProduct(1L));

		DatasourcePurgeStatus running = service.start(RAKUTEN, DatasourcePurgeScope.OFFERS, true);
		assertThat(running.scope()).isEqualTo(DatasourcePurgeScope.OFFERS);
		assertThat(running.report().scope()).isEqualTo(DatasourcePurgeScope.OFFERS);

		DatasourcePurgeStatus done = awaitStatus(service, s -> s.state() != DatasourcePurgeStatus.State.RUNNING);
		assertThat(done.state()).isEqualTo(DatasourcePurgeStatus.State.COMPLETED);
		assertThat(done.scope()).isEqualTo(DatasourcePurgeScope.OFFERS);
		assertThat(done.report().scope()).isEqualTo(DatasourcePurgeScope.OFFERS);
		assertThat(new DatasourcePurgeService(repository, statusFile, PROGRESS_INTERVAL).getStatus()).isEqualTo(done);
	}

	@Test
	void statusWrittenBeforeTheScopeIsReadBackAsAll() throws Exception {
		Files.createDirectories(statusFile.getParent());
		// Status file of the GOU-1593 dry run, written before GOU-2101
		Files.writeString(statusFile, """
				{"state":"COMPLETED","datasourceName":"rakuten.com","dryRun":true,
				 "startedAt":"2026-11-10T08:00:00Z","lastProgressAt":"2026-11-10T09:00:00Z",
				 "finishedAt":"2026-11-10T09:00:00Z",
				 "report":{"datasourceName":"rakuten.com","dryRun":true,"scannedProducts":3,"touchedProducts":2,
				  "removedOffers":1,"savedProducts":0,"failedProducts":0,"sampleGtins":["1"],"durationMs":5},
				 "error":null}
				""", StandardCharsets.UTF_8);

		DatasourcePurgeStatus readBack = new DatasourcePurgeService(repository, statusFile, PROGRESS_INTERVAL).getStatus();

		assertThat(readBack.state()).isEqualTo(DatasourcePurgeStatus.State.COMPLETED);
		assertThat(readBack.scope()).isEqualTo(DatasourcePurgeScope.ALL);
		assertThat(readBack.report().scope()).isEqualTo(DatasourcePurgeScope.ALL);
		assertThat(readBack.report().touchedProducts()).isEqualTo(2);
		assertThat(DatasourcePurgeStatus.idle().withLegacyScope().scope()).isNull();
	}

	@Test
	void nullScopeIsRejected() {
		assertThatThrownBy(() -> service.purge(RAKUTEN, null, true)).isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> service.start(RAKUTEN, null, true)).isInstanceOf(NullPointerException.class);
	}

	private static DatasourcePurgeStatus awaitStatus(DatasourcePurgeService purgeService,
			Predicate<DatasourcePurgeStatus> condition) throws InterruptedException {
		Instant deadline = Instant.now().plus(Duration.ofSeconds(10));
		while (!condition.test(purgeService.getStatus()) && Instant.now().isBefore(deadline)) {
			Thread.sleep(20);
		}
		return purgeService.getStatus();
	}

	/** Stream over the products which blocks, before the product of index {@code stallAt}, until released. */
	private static Stream<Product> stallingStream(List<Product> products, int stallAt, CountDownLatch blocked,
			CountDownLatch release) {
		Iterator<Product> iterator = new Iterator<>() {
			private int next;

			@Override
			public boolean hasNext() {
				if (next == stallAt) {
					blocked.countDown();
					try {
						release.await(10, TimeUnit.SECONDS);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				}
				return next < products.size();
			}

			@Override
			public Product next() {
				return products.get(next++);
			}
		};
		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, 0), false);
	}

	@SuppressWarnings("unchecked")
	private List<ProductPartialUpdateHolder> partialUpdates() {
		ArgumentCaptor<Collection<ProductPartialUpdateHolder>> captor = ArgumentCaptor.forClass(Collection.class);
		verify(repository, atLeastOnce()).bulkUpdateDocument(captor.capture());
		return captor.getAllValues().stream().flatMap(Collection::stream).collect(Collectors.toList());
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
