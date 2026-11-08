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
import org.open4goods.services.productrepository.services.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Removes from the product index every piece of data contributed by a retired datasource (GOU-1592, Rakuten
 * France closed its marketplace on 2026-09-30).
 *
 * <p>For each product of the index, read by pages of {@link ProductRepository#FULL_PRODUCT_PAGE_SIZE} through a
 * scroll (never the whole index in memory), the purge:
 * <ol>
 *   <li>calls {@link Product#removeDatasourceData(String)} (datasource codes, brand aliases, categories,
 *       descriptions, attribute sources);</li>
 *   <li>removes the offers of {@code price.offers} whose {@code datasourceName} equals the datasource;</li>
 *   <li>recomputes the best offer ({@code price.minPrice}), {@code offersCount} and {@code price.conditions}
 *       from the remaining offers, without filtering them: the offers of other datasources are kept as is;</li>
 *   <li>writes the product back, by bulks of {@link #SAVE_BATCH_SIZE}, unless in dry run.</li>
 * </ol>
 * Products holding no data of the datasource are never written, so a second run touches nothing. Price history
 * and trends are left untouched: they are a daily best price, not an offer.
 *
 * <p>Complete products (embeddings included) are read and written back, so nothing else is lost. A product updated
 * by the ingestion while the purge runs may be overwritten with the version read by the scroll: run it outside the
 * nightly batch window.
 *
 * <p>Progress (counters and sample GTINs) is logged every {@code progressInterval} scanned products, and at least
 * every {@link #PROGRESS_MAX_DELAY}. The background run ({@link #start(String, boolean)}) also writes its status to a
 * JSON file at the start, at each progress step and at the end (GOU-1815). At startup the file is read back: a run
 * left RUNNING by a stopped api becomes INTERRUPTED, with its last counters. It is never resumed.
 */
public class DatasourcePurgeService {

	private static final Logger logger = LoggerFactory.getLogger(DatasourcePurgeService.class);

	/** Products written per bulk request. */
	static final int SAVE_BATCH_SIZE = 200;

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
	 * Starts {@link #purge(String, boolean)} in the background.
	 *
	 * @param datasourceName exact datasource name (for instance {@code rakuten.com})
	 * @param dryRun         when true, only counts what would be removed
	 * @return the RUNNING status of the new run, or the current status unchanged when a run is already in progress
	 * @throws IllegalArgumentException when the datasource name is blank
	 */
	public DatasourcePurgeStatus start(String datasourceName, boolean dryRun) {
		String datasource = requireDatasource(datasourceName);
		Instant startedAt = Instant.now();
		DatasourcePurgeStatus running = new DatasourcePurgeStatus(DatasourcePurgeStatus.State.RUNNING, datasource,
				dryRun, startedAt, startedAt, null, emptyReport(datasource, dryRun), null);
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
				DatasourcePurgeReport report = purge(datasource, dryRun, progress -> {
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
			status.set(done);
			persist(done);
			logger.info("Purge of datasource finished: {}", done);
		});
		return running;
	}

	/** @return the status of the current or last background purge */
	public DatasourcePurgeStatus getStatus() {
		return status.get();
	}

	/**
	 * Scans the whole product index and removes the data of the given datasource.
	 *
	 * @param datasourceName exact datasource name
	 * @param dryRun         when true, nothing is written
	 * @return the purge report
	 * @throws IllegalArgumentException when the datasource name is blank
	 */
	public DatasourcePurgeReport purge(String datasourceName, boolean dryRun) {
		return purge(requireDatasource(datasourceName), dryRun, progress -> { });
	}

	/**
	 * Scan of {@link #purge(String, boolean)}, reporting each progress step.
	 *
	 * @param onProgress receives the counters of each progress step, after its log line
	 */
	private DatasourcePurgeReport purge(String datasource, boolean dryRun, Consumer<DatasourcePurgeReport> onProgress) {
		long start = System.currentTimeMillis();
		long lastProgress = start;
		logger.info("Purge of datasource {} started (dryRun={})", datasource, dryRun);

		long scanned = 0;
		long touched = 0;
		long removedOffers = 0;
		long saved = 0;
		long failed = 0;
		List<String> samples = new ArrayList<>();
		List<Product> pending = new ArrayList<>(SAVE_BATCH_SIZE);

		try (Stream<Product> products = productRepository.exportAll()) {
			Iterator<Product> it = products.iterator();
			while (it.hasNext()) {
				Product product = it.next();
				scanned++;
				try {
					int removed = purgeProduct(product, datasource);
					if (removed >= 0) {
						touched++;
						removedOffers += removed;
						if (samples.size() < SAMPLE_GTINS_SIZE) {
							samples.add(product.gtin());
						}
						if (!dryRun) {
							pending.add(product);
							if (pending.size() >= SAVE_BATCH_SIZE) {
								saved += flush(pending);
							}
						}
					}
				} catch (RuntimeException e) {
					failed++;
					logger.error("Cannot purge datasource {} from product {}", datasource, product.getId(), e);
				}
				long now = System.currentTimeMillis();
				if (scanned % progressInterval == 0 || now - lastProgress >= PROGRESS_MAX_DELAY.toMillis()) {
					lastProgress = now;
					logger.info("Purge of datasource {} (dryRun={}): {} products scanned, {} touched, {} offers removed, "
							+ "{} saved, {} failed, sample GTINs {}", datasource, dryRun, scanned, touched,
							removedOffers, saved, failed, samples);
					onProgress.accept(new DatasourcePurgeReport(datasource, dryRun, scanned, touched, removedOffers,
							saved, failed, List.copyOf(samples), now - start));
				}
			}
		}
		if (!pending.isEmpty()) {
			saved += flush(pending);
		}

		DatasourcePurgeReport report = new DatasourcePurgeReport(datasource, dryRun, scanned, touched, removedOffers,
				saved, failed, List.copyOf(samples), System.currentTimeMillis() - start);
		logger.info("Purge of datasource {} done: {}", datasource, report);
		return report;
	}

	/**
	 * Removes in memory every piece of data of the datasource from the product, and recomputes the aggregated prices
	 * when offers were removed. Data of other datasources is never removed.
	 *
	 * @param product        product to clean, modified in place
	 * @param datasourceName exact datasource name
	 * @return the number of offers removed, or -1 when the product holds no data of the datasource (left unchanged)
	 */
	static int purgeProduct(Product product, String datasourceName) {
		boolean changed = product.removeDatasourceData(datasourceName);

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
		if (persisted.state() == DatasourcePurgeStatus.State.RUNNING) {
			DatasourcePurgeStatus interrupted = persisted.interrupted();
			logger.warn("Purge of datasource {} was interrupted by a restart: {}", interrupted.datasourceName(), interrupted);
			persist(interrupted);
			return interrupted;
		}
		logger.info("Last purge of datasource read back from {}: {}", statusFile, persisted);
		return persisted;
	}

	private static DatasourcePurgeReport emptyReport(String datasource, boolean dryRun) {
		return new DatasourcePurgeReport(datasource, dryRun, 0, 0, 0, 0, 0, List.of(), 0);
	}

	private long flush(List<Product> pending) {
		int size = pending.size();
		productRepository.store(new ArrayList<>(pending));
		pending.clear();
		return size;
	}

	private static String requireDatasource(String datasourceName) {
		if (StringUtils.isBlank(datasourceName)) {
			throw new IllegalArgumentException("datasource is required");
		}
		return datasourceName.trim();
	}
}
