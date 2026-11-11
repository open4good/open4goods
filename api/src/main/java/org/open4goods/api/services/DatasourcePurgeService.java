package org.open4goods.api.services;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.model.price.AggregatedPrice;
import org.open4goods.model.price.AggregatedPrices;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.ProductCondition;
import org.open4goods.model.product.ProductPartialUpdateField;
import org.open4goods.model.product.ProductPartialUpdateHolder;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.BulkFailureException;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Removes from the product index the data contributed by a retired datasource (GOU-1592, Rakuten France closed its
 * marketplace on 2026-09-30), within a {@link DatasourcePurgeScope} (GOU-2101).
 *
 * <p>For each product of the index, read by pages of {@link ProductRepository#FULL_PRODUCT_PAGE_SIZE} through a
 * scroll (never the whole index in memory), the purge:
 * <ol>
 *   <li>in scope {@link DatasourcePurgeScope#ALL} only, calls {@link Product#removeDatasourceData(String)}
 *       (datasource codes, brand aliases, categories, descriptions, attribute sources);</li>
 *   <li>removes the offers of {@code price.offers} whose {@code datasourceName} equals the datasource;</li>
 *   <li>recomputes the best offer ({@code price.minPrice}), {@code offersCount} and {@code price.conditions}
 *       from the remaining offers, without filtering them: the offers of other datasources are kept as is;</li>
 *   <li>writes the product back, by bulks of {@link #SAVE_BATCH_SIZE}, unless in dry run.</li>
 * </ol>
 * Products holding no data of the datasource in the scope are never written, so a second run touches nothing. Price
 * history and trends are left untouched: they are a daily best price, not an offer.
 *
 * <p>Scope {@link DatasourcePurgeScope#OFFERS} (the default) reads only {@link #OFFERS_FIELDS} and writes only
 * {@code price} and {@code offersCount}, by partial updates ({@link ProductRepository#bulkUpdateDocument}): the
 * embeddings and the other fields are neither read nor rewritten, and a concurrent update of those fields is kept.
 * Scope {@link DatasourcePurgeScope#ALL} reads and stores complete products (embeddings included), so nothing else
 * is lost. In both scopes, a price updated by the ingestion while the purge runs may be overwritten with the version
 * read by the scroll: run it outside the nightly batch window.
 *
 * <p>Progress (counters and sample GTINs) is logged every {@code progressInterval} scanned products, and at least
 * every {@link #PROGRESS_MAX_DELAY}. The background run ({@link #start(String, DatasourcePurgeScope, boolean)}) also writes its status to a
 * JSON file at the start, at each progress step and at the end (GOU-1815). At startup the file is read back: a run
 * left RUNNING by a stopped api becomes INTERRUPTED, with its last counters. It is never resumed.
 */
public class DatasourcePurgeService {

	private static final Logger logger = LoggerFactory.getLogger(DatasourcePurgeService.class);

	/** Products written per bulk request. */
	static final int SAVE_BATCH_SIZE = 200;

	/**
	 * Source fields read in scope {@link DatasourcePurgeScope#OFFERS}: the prices, the offers count and the
	 * referential GTIN of the sample GTINs. The id comes with every hit.
	 */
	static final String[] OFFERS_FIELDS = { "price", "offersCount", "attributes.referentielAttributes.GTIN" };

	/** Number of GTINs reported as examples. */
	static final int SAMPLE_GTINS_SIZE = 20;

	/** A progress step is also taken when the last one is older than this, whatever the number of products. */
	static final Duration PROGRESS_MAX_DELAY = Duration.ofMinutes(5);

	private static final JsonMapper JSON = JsonMapper.builder()
			.enable(SerializationFeature.INDENT_OUTPUT)
			.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
			.build();

	private final ProductRepository productRepository;

	/** JSON file holding the status of the current or last background run. */
	private final Path statusFile;

	/** Number of scanned products between two progress steps. */
	private final long progressInterval;

	/** Single threaded: two purges must never overlap. */
	private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
		Thread t = new Thread(r, "purge-datasource");
		t.setDaemon(true);
		return t;
	});

	private final AtomicReference<DatasourcePurgeStatus> status = new AtomicReference<>(DatasourcePurgeStatus.idle());

	/**
	 * Creates the service and reads back the status file: a run left RUNNING is reported (and rewritten) as
	 * INTERRUPTED.
	 *
	 * @param productRepository product index
	 * @param statusFile        JSON file of the status, its folder is created when missing
	 * @param progressInterval  number of scanned products between two progress steps
	 * @throws IllegalArgumentException when the progress interval is not positive
	 */
	public DatasourcePurgeService(ProductRepository productRepository, Path statusFile, long progressInterval) {
		if (progressInterval < 1) {
			throw new IllegalArgumentException("progressInterval must be positive");
		}
		this.productRepository = productRepository;
		this.statusFile = Objects.requireNonNull(statusFile, "statusFile");
		this.progressInterval = progressInterval;
		status.set(readPersistedStatus());
	}

	/**
	 * Starts {@link #purge(String, DatasourcePurgeScope, boolean)} in the background.
	 *
	 * @param datasourceName exact datasource name (for instance {@code rakuten.com})
	 * @param scope          what to remove
	 * @param dryRun         when true, only counts what would be removed
	 * @return the RUNNING status of the new run, or the current status unchanged when a run is already in progress
	 * @throws IllegalArgumentException when the datasource name is blank
	 * @throws NullPointerException     when the scope is null
	 */
	public DatasourcePurgeStatus start(String datasourceName, DatasourcePurgeScope scope, boolean dryRun) {
		String datasource = requireDatasource(datasourceName);
		Objects.requireNonNull(scope, "scope");
		Instant startedAt = Instant.now();
		DatasourcePurgeStatus running = new DatasourcePurgeStatus(DatasourcePurgeStatus.State.RUNNING, datasource,
				scope, dryRun, startedAt, startedAt, null, emptyReport(datasource, scope, dryRun), null);
		while (true) {
			DatasourcePurgeStatus current = status.get();
			if (current.state() == DatasourcePurgeStatus.State.RUNNING) {
				return current;
			}
			if (status.compareAndSet(current, running)) {
				break;
			}
		}
		persist(running);
		executor.execute(() -> {
			DatasourcePurgeStatus done;
			try {
				DatasourcePurgeReport report = purge(datasource, scope, dryRun, progress -> {
					DatasourcePurgeStatus step = status.get().withProgress(progress, Instant.now());
					status.set(step);
					persist(step);
				});
				done = status.get().finish(DatasourcePurgeStatus.State.COMPLETED, report, Instant.now(), null);
			} catch (Throwable t) {
				logger.error("Purge of datasource {} failed", datasource, t);
				DatasourcePurgeStatus last = status.get();
				done = last.finish(DatasourcePurgeStatus.State.FAILED, last.report(), Instant.now(), String.valueOf(t));
			}
			// Written before it is published: whoever sees the final state can read it back from the file
			persist(done);
			status.set(done);
			logger.info("Purge of datasource finished: {}", done);
		});
		return running;
	}

	/** @return the status of the current or last background purge */
	public DatasourcePurgeStatus getStatus() {
		return status.get();
	}

	/**
	 * Scans the whole product index and removes the data of the given datasource within the scope.
	 *
	 * @param datasourceName exact datasource name
	 * @param scope          what to remove
	 * @param dryRun         when true, nothing is written
	 * @return the purge report
	 * @throws IllegalArgumentException when the datasource name is blank
	 * @throws NullPointerException     when the scope is null
	 */
	public DatasourcePurgeReport purge(String datasourceName, DatasourcePurgeScope scope, boolean dryRun) {
		return purge(requireDatasource(datasourceName), Objects.requireNonNull(scope, "scope"), dryRun, progress -> { });
	}

	/**
	 * Scan of {@link #purge(String, DatasourcePurgeScope, boolean)}, reporting each progress step.
	 *
	 * @param onProgress receives the counters of each progress step, after its log line
	 */
	private DatasourcePurgeReport purge(String datasource, DatasourcePurgeScope scope, boolean dryRun,
			Consumer<DatasourcePurgeReport> onProgress) {
		long start = System.currentTimeMillis();
		long lastProgress = start;
		logger.info("Purge of datasource {} started (scope={}, dryRun={})", datasource, scope, dryRun);

		long scanned = 0;
		long touched = 0;
		long removedOffers = 0;
		long saved = 0;
		long failed = 0;
		List<String> samples = new ArrayList<>();
		List<Product> pending = new ArrayList<>(SAVE_BATCH_SIZE);

		try (Stream<Product> products = scan(scope)) {
			Iterator<Product> it = products.iterator();
			while (it.hasNext()) {
				Product product = it.next();
				scanned++;
				try {
					int removed = purgeProduct(product, datasource, scope);
					if (removed >= 0) {
						touched++;
						removedOffers += removed;
						if (samples.size() < SAMPLE_GTINS_SIZE) {
							samples.add(product.gtin());
						}
						if (!dryRun) {
							pending.add(product);
						}
					}
				} catch (RuntimeException e) {
					failed++;
					logger.error("Cannot purge datasource {} from product {}", datasource, product.getId(), e);
				}
				if (pending.size() >= SAVE_BATCH_SIZE) {
					int size = pending.size();
					long written = flush(pending, datasource, scope);
					saved += written;
					failed += size - written;
				}
				long now = System.currentTimeMillis();
				if (scanned % progressInterval == 0 || now - lastProgress >= PROGRESS_MAX_DELAY.toMillis()) {
					lastProgress = now;
					logger.info("Purge of datasource {} (scope={}, dryRun={}): {} products scanned, {} touched, "
							+ "{} offers removed, {} saved, {} failed, sample GTINs {}", datasource, scope, dryRun,
							scanned, touched, removedOffers, saved, failed, samples);
					onProgress.accept(new DatasourcePurgeReport(datasource, scope, dryRun, scanned, touched,
							removedOffers, saved, failed, List.copyOf(samples), now - start));
				}
			}
		}
		if (!pending.isEmpty()) {
			int size = pending.size();
			long written = flush(pending, datasource, scope);
			saved += written;
			failed += size - written;
		}

		DatasourcePurgeReport report = new DatasourcePurgeReport(datasource, scope, dryRun, scanned, touched,
				removedOffers, saved, failed, List.copyOf(samples), System.currentTimeMillis() - start);
		logger.info("Purge of datasource {} done: {}", datasource, report);
		return report;
	}

	/**
	 * Removes in memory the data of the datasource within the scope from the product, and recomputes the aggregated
	 * prices when an offer or the best offer of the datasource was removed. Data of other datasources is never
	 * removed.
	 *
	 * @param product        product to clean, modified in place
	 * @param datasourceName exact datasource name
	 * @param scope          OFFERS: offers and best offer only; ALL: also {@link Product#removeDatasourceData(String)}
	 * @return the number of offers removed, or -1 when the product holds no data of the datasource in the scope
	 *         (left unchanged)
	 */
	static int purgeProduct(Product product, String datasourceName, DatasourcePurgeScope scope) {
		boolean changed = scope == DatasourcePurgeScope.ALL && product.removeDatasourceData(datasourceName);

		AggregatedPrices prices = product.getPrice();
		int removedOffers = 0;
		if (prices != null) {
			Set<AggregatedPrice> offers = prices.getOffers() == null ? new HashSet<>() : new HashSet<>(prices.getOffers());
			int before = offers.size();
			offers.removeIf(offer -> datasourceName.equals(offer.getDatasourceName()));
			removedOffers = before - offers.size();

			AggregatedPrice minPrice = prices.getMinPrice();
			boolean staleMinPrice = minPrice != null && datasourceName.equals(minPrice.getDatasourceName());
			if (removedOffers > 0 || staleMinPrice) {
				changed = true;
				recomputePrices(product, prices, offers);
			}
		}
		return changed ? removedOffers : -1;
	}

	/**
	 * Product stream of the scope: complete products for ALL, only {@link #OFFERS_FIELDS} for OFFERS.
	 */
	private Stream<Product> scan(DatasourcePurgeScope scope) {
		return scope == DatasourcePurgeScope.ALL
				? productRepository.exportAll()
				: productRepository.exportAll(OFFERS_FIELDS, ProductRepository.FULL_PRODUCT_PAGE_SIZE);
	}

	/**
	 * Same rules as {@code PriceAggregationService} for the best offer, the offers count and the conditions, but
	 * applied to the offers as they are (no freshness filter, no deduplication).
	 */
	private static void recomputePrices(Product product, AggregatedPrices prices, Set<AggregatedPrice> offers) {
		prices.setOffers(offers);
		prices.setMinPrice(offers.stream()
				.filter(offer -> offer.getPrice() != null)
				.min(Comparator.comparing(AggregatedPrice::getPrice))
				.orElse(null));
		product.setOffersCount(offers.size());

		Set<ProductCondition> conditions = new HashSet<>();
		offers.stream().map(AggregatedPrice::getProductState).filter(Objects::nonNull).forEach(conditions::add);
		prices.setConditions(conditions);
	}

	/**
	 * Writes the status to {@link #statusFile} through a temporary file renamed over it, so a reader (or a restart)
	 * never sees a partial file. A write failure is logged and never stops the purge.
	 */
	private synchronized void persist(DatasourcePurgeStatus snapshot) {
		Path tmp = statusFile.resolveSibling(statusFile.getFileName() + ".tmp");
		try {
			Path folder = statusFile.toAbsolutePath().getParent();
			if (folder != null) {
				Files.createDirectories(folder);
			}
			Files.writeString(tmp, JSON.writeValueAsString(snapshot), StandardCharsets.UTF_8);
			Files.move(tmp, statusFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException | JacksonException e) {
			logger.error("Cannot write the datasource purge status to {}", statusFile, e);
		}
	}

	/**
	 * Reads back the status file at startup. A run still RUNNING was stopped with the api: it becomes INTERRUPTED,
	 * and the file is rewritten so. A missing or unreadable file gives IDLE.
	 */
	private DatasourcePurgeStatus readPersistedStatus() {
		DatasourcePurgeStatus persisted;
		try {
			persisted = JSON.readValue(Files.readString(statusFile, StandardCharsets.UTF_8), DatasourcePurgeStatus.class);
		} catch (NoSuchFileException e) {
			return DatasourcePurgeStatus.idle();
		} catch (IOException | JacksonException e) {
			logger.warn("Cannot read the datasource purge status from {}, starting IDLE", statusFile, e);
			return DatasourcePurgeStatus.idle();
		}
		if (persisted == null || persisted.state() == null) {
			return DatasourcePurgeStatus.idle();
		}
		persisted = persisted.withLegacyScope();
		if (persisted.state() == DatasourcePurgeStatus.State.RUNNING) {
			DatasourcePurgeStatus interrupted = persisted.interrupted();
			logger.warn("Purge of datasource {} was interrupted by a restart: {}", interrupted.datasourceName(), interrupted);
			persist(interrupted);
			return interrupted;
		}
		logger.info("Last purge of datasource read back from {}: {}", statusFile, persisted);
		return persisted;
	}

	private static DatasourcePurgeReport emptyReport(String datasource, DatasourcePurgeScope scope, boolean dryRun) {
		return new DatasourcePurgeReport(datasource, scope, dryRun, 0, 0, 0, 0, 0, List.of(), 0);
	}

	/**
	 * Writes the pending products and empties the list, even on failure: a failed bulk is counted, never retried.
	 * ALL stores the complete products; OFFERS updates only {@code price} and {@code offersCount}.
	 *
	 * @return the number of products written; the others failed (see the logs)
	 */
	private long flush(List<Product> pending, String datasource, DatasourcePurgeScope scope) {
		int size = pending.size();
		try {
			if (scope == DatasourcePurgeScope.ALL) {
				productRepository.store(new ArrayList<>(pending));
			} else {
				productRepository.bulkUpdateDocument(pending.stream().map(DatasourcePurgeService::offersUpdate).toList());
			}
			return size;
		} catch (BulkFailureException e) {
			// A product deleted since the scroll read it cannot be updated: only the failed documents are lost
			int failedDocuments = e.getFailedDocuments() == null ? size : Math.min(size, e.getFailedDocuments().size());
			logger.error("Purge of datasource {}: {} of {} products not written: {}", datasource, failedDocuments,
					size, e.getFailedDocuments());
			return size - failedDocuments;
		} catch (RuntimeException e) {
			logger.error("Purge of datasource {}: bulk of {} products not written", datasource, size, e);
			return 0;
		} finally {
			pending.clear();
		}
	}

	/** Partial update of a product purged in scope OFFERS: its whole {@code price} and its {@code offersCount}. */
	private static ProductPartialUpdateHolder offersUpdate(Product product) {
		ProductPartialUpdateHolder update = new ProductPartialUpdateHolder(product.getId());
		update.addChange(ProductPartialUpdateField.PRICE, product.getPrice());
		update.addChange(ProductPartialUpdateField.OFFERS_COUNT, product.getOffersCount());
		return update;
	}

	private static String requireDatasource(String datasourceName) {
		if (StringUtils.isBlank(datasourceName)) {
			throw new IllegalArgumentException("datasource is required");
		}
		return datasourceName.trim();
	}
}
