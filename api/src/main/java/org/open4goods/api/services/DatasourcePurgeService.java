package org.open4goods.api.services;

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
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.model.price.AggregatedPrice;
import org.open4goods.model.price.AggregatedPrices;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.ProductCondition;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 */
public class DatasourcePurgeService {

	private static final Logger logger = LoggerFactory.getLogger(DatasourcePurgeService.class);

	/** Products written per bulk request. */
	static final int SAVE_BATCH_SIZE = 200;

	/** Number of GTINs reported as examples. */
	static final int SAMPLE_GTINS_SIZE = 20;

	/** A progress line is logged every this many scanned products. */
	static final long PROGRESS_LOG_INTERVAL = 50_000;

	private final ProductRepository productRepository;

	/** Single threaded: two purges must never overlap. */
	private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
		Thread t = new Thread(r, "purge-datasource");
		t.setDaemon(true);
		return t;
	});

	private final AtomicReference<DatasourcePurgeStatus> status = new AtomicReference<>(DatasourcePurgeStatus.idle());

	public DatasourcePurgeService(ProductRepository productRepository) {
		this.productRepository = productRepository;
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
		DatasourcePurgeStatus running = new DatasourcePurgeStatus(DatasourcePurgeStatus.State.RUNNING, datasource,
				dryRun, Instant.now(), null, null, null);
		while (true) {
			DatasourcePurgeStatus current = status.get();
			if (current.state() == DatasourcePurgeStatus.State.RUNNING) {
				return current;
			}
			if (status.compareAndSet(current, running)) {
				break;
			}
		}
		executor.execute(() -> {
			DatasourcePurgeStatus done;
			try {
				DatasourcePurgeReport report = purge(datasource, dryRun);
				done = new DatasourcePurgeStatus(DatasourcePurgeStatus.State.COMPLETED, datasource, dryRun,
						running.startedAt(), Instant.now(), report, null);
			} catch (Throwable t) {
				logger.error("Purge of datasource {} failed", datasource, t);
				done = new DatasourcePurgeStatus(DatasourcePurgeStatus.State.FAILED, datasource, dryRun,
						running.startedAt(), Instant.now(), null, String.valueOf(t));
			}
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
	 * Scans the whole product index and removes the data of the given datasource.
	 *
	 * @param datasourceName exact datasource name
	 * @param dryRun         when true, nothing is written
	 * @return the purge report
	 * @throws IllegalArgumentException when the datasource name is blank
	 */
	public DatasourcePurgeReport purge(String datasourceName, boolean dryRun) {
		String datasource = requireDatasource(datasourceName);
		long start = System.currentTimeMillis();
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
				if (scanned % PROGRESS_LOG_INTERVAL == 0) {
					logger.info("Purge of datasource {} (dryRun={}): {} products scanned, {} touched, {} offers removed, "
							+ "{} saved, {} failed", datasource, dryRun, scanned, touched, removedOffers, saved, failed);
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
