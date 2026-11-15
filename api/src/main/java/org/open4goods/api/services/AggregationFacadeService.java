
package org.open4goods.api.services;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.open4goods.api.config.yml.ApiProperties;
import org.open4goods.api.services.aggregation.AbstractAggregationService;
import org.open4goods.api.services.aggregation.aggregator.ScoringBatchedAggregator;
import org.open4goods.api.services.aggregation.aggregator.StandardAggregator;
import org.open4goods.api.services.aggregation.services.batch.scores.Attribute2ScoreAggregationService;
import org.open4goods.api.services.aggregation.services.batch.scores.BrandScoresAggregationService;
import org.open4goods.api.services.aggregation.services.batch.scores.CleanScoreAggregationService;
import org.open4goods.api.services.aggregation.services.batch.scores.DataCompletion2ScoreAggregationService;
import org.open4goods.api.services.aggregation.services.batch.scores.EcoScoreAggregationService;
import org.open4goods.api.services.aggregation.services.batch.scores.ParticipatingScoresAggregationService;
import org.open4goods.api.services.aggregation.services.realtime.AttributeRealtimeAggregationService;
import org.open4goods.api.services.aggregation.services.realtime.IdentityAggregationService;
import org.open4goods.api.services.aggregation.services.realtime.MediaAggregationService;
import org.open4goods.api.services.aggregation.services.realtime.NamesAggregationService;
import org.open4goods.api.services.aggregation.services.realtime.PriceAggregationService;
import org.open4goods.api.services.aggregation.services.realtime.TaxonomyRealTimeAggregationService;
import org.open4goods.api.services.aggregation.services.realtime.UsageCostAggregationService;
import org.open4goods.brand.service.BrandService;
import org.open4goods.commons.exceptions.AggregationSkipException;
import org.open4goods.commons.helper.GenericFileLogger;
import org.open4goods.commons.services.BarcodeForensicsService;
import org.open4goods.commons.services.BarcodeValidationService;
import org.open4goods.commons.services.Gs1PrefixService;
import org.open4goods.commons.services.textgen.BlablaService;
import org.open4goods.icecat.services.IcecatFeatureResolver;
import org.open4goods.model.datafragment.DataFragment;
import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.verticals.VerticalsConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Central factory and orchestrator for the product aggregation pipeline.
 *
 * <p>Assembles and invokes two kinds of aggregators:
 * <ul>
 *   <li><strong>StandardAggregator</strong> — the full chain of the 7 realtime services, built once and shared
 *       ({@link #getStandardAggregator()}) by the realtime ingestion ({@link #updateOne(DataFragment, Product)}),
 *       the completion services (Icecat, EPREL, Wikidata, Amazon), the batch
 *       ({@link #aggregateProducts(VerticalConfig, Set)}) and the on-demand sanitisation
 *       ({@link #sanitizeVertical(VerticalConfig)}). A second instance, also built once, runs the identity and
 *       taxonomy services only ({@link #classificationAggregator(VerticalConfig, Set)}).</li>
 *   <li><strong>ScoringBatchedAggregator</strong> — used by the batch for scoring
 *       ({@link #score(VerticalConfig, Set)}), built again for each run because the scoring services hold
 *       run-scoped state.</li>
 * </ul>
 *
 * <p>The standard chain is shared between threads (the fragment worker, up to {@code api.completion-concurrency}
 * completion threads, the batch): it and its services hold no mutable state (GOU-2280). Its services write to the
 * {@code realtime} aggregation log, whatever the caller.
 *
 * <p>TODO: Maintain a state machine to prevent concurrent batch launches on the
 * same vertical. See docs/adr/ for the proposed design.
 */
public class AggregationFacadeService {

	protected static final Logger logger = LoggerFactory.getLogger(AggregationFacadeService.class);

	/** Aggregation log of the standard chain ({@code <logs>/aggregation/realtime.log}). */
	private static final String STANDARD_LOG = "realtime";

	/** Aggregation log of the classification-only chain. */
	private static final String CLASSIFICATION_LOG = "categores_classif";

	/** Aggregation log of the scoring chain, read by the release routine ({@code aggregation/score.log}). */
	private static final String SCORING_LOG = "score";

	private final ProductRepository dataRepository;
	private final ApiProperties apiProperties;
	private final VerticalsConfigService verticalConfigService;
	private final BrandService brandService;

	/** The full chain of the 7 realtime services, shared by every caller (GOU-2280). */
	private final StandardAggregator standardAggregator;

	/** Identity and taxonomy services only, to (re-)classify products. */
	private final StandardAggregator classificationOnlyAggregator;

	public AggregationFacadeService(final ProductRepository aggregatedDataRepository,
			final ApiProperties apiProperties,
			final Gs1PrefixService gs1prefixService,
			final VerticalsConfigService configService,
			final BarcodeValidationService barcodeValidationService,
			final BrandService brandService,
			final BlablaService blablaService,
			final IcecatFeatureResolver icecatFeatureResolver,
			final BarcodeForensicsService barcodeForensicsService) {
		this.dataRepository = aggregatedDataRepository;
		this.apiProperties = apiProperties;
		this.verticalConfigService = configService;
		this.brandService = brandService;

		Logger standardLog = aggregationLogger(STANDARD_LOG);
		this.standardAggregator = new StandardAggregator(List.of(
				new IdentityAggregationService(standardLog, gs1prefixService, barcodeValidationService, barcodeForensicsService),
				new TaxonomyRealTimeAggregationService(standardLog, configService),
				new AttributeRealtimeAggregationService(brandService, standardLog, icecatFeatureResolver),
				new UsageCostAggregationService(standardLog),
				new NamesAggregationService(standardLog, blablaService),
				new PriceAggregationService(standardLog),
				new MediaAggregationService(standardLog)), configService);

		Logger classificationLog = aggregationLogger(CLASSIFICATION_LOG);
		this.classificationOnlyAggregator = new StandardAggregator(List.of(
				new IdentityAggregationService(classificationLog, gs1prefixService, barcodeValidationService, barcodeForensicsService),
				new TaxonomyRealTimeAggregationService(classificationLog, configService)), configService);
	}

	/**
	 * Scores a pre-filtered set of products within the given vertical, without
	 * persisting results. Called by {@link BatchService#batch(VerticalConfig)}, which
	 * applies the ImpactScore checks and persists the products.
	 *
	 * @param vertical vertical configuration
	 * @param products products to score; modified in-place
	 */
	public void score(final VerticalConfig vertical, final Set<Product> products) {
		logger.info("Score batching for {} products in {}", products.size(), vertical.getId());

		ScoringBatchedAggregator batchAgg = getScoringAggregator();
		batchAgg.score(products, verticalConfigService.getConfigByIdOrDefault(vertical.getId()));
	}

	/**
	 * Re-sanitises all products in all verticals, one vertical after the other. Delegates to
	 * {@link #sanitizeVertical(VerticalConfig)} per vertical. A vertical that fails is logged (ERROR) and the next
	 * ones still run, as in {@link BatchService#batch()} (GOU-2280).
	 */
	public void sanitizeAllVerticals() {
		List<String> failed = new ArrayList<>();
		for (VerticalConfig vertical : verticalConfigService.getConfigsWithoutDefault()) {
			try {
				sanitizeVertical(vertical);
			} catch (RuntimeException e) {
				failed.add(vertical.getId());
				logger.error("Sanitisation of vertical {} failed, moving on to the next vertical", vertical.getId(), e);
			}
		}
		if (!failed.isEmpty()) {
			logger.error("Sanitisation of all verticals done, {} failed: {}", failed.size(), failed);
		}
	}

	/**
	 * Re-sanitises all products matching the given vertical's categories, then
	 * queues them for full indexation. This relies on category matching rather than the
	 * pre-assigned vertical field, so products that have drifted from their vertical
	 * assignment are also corrected (runbook {@code docs/operations/product-recreation-recovery.md}).
	 *
	 * <p>Only the standard aggregation chain runs: no completion, no scoring. The products
	 * are streamed and written like the batch does (GOU-1709, GOU-2274): by pages of
	 * {@link BatchService#INDEXATION_PAGE_SIZE}, each page gets the newest realtime offers
	 * ({@link ProductRepository#applyNewerOfferStates}) just before being queued, so an offer
	 * indexed or queued since the product was read is never overwritten by the one it was
	 * read with. This holds because the standard chain never moves {@code lastChange} on
	 * {@link StandardAggregator#onProduct(Product)}: only a data fragment does.
	 *
	 * <p>Queueing a page blocks while the full indexation queue is full; the scroll is kept
	 * alive long enough between two requests ({@code ProductRepository.CATEGORY_MATCH_SCROLL_KEEP_ALIVE}).
	 * The method returns once the products are queued, not once they are indexed.
	 *
	 * @param vertical vertical configuration to process
	 */
	public void sanitizeVertical(final VerticalConfig vertical) {
		logger.info("started : Sanitisation batching for vertical : {}", vertical.getId());
		StandardAggregator batchAgg = getStandardAggregator();

		long queued = 0;
		long refreshed = 0;
		List<Product> page = new ArrayList<>(BatchService.INDEXATION_PAGE_SIZE);
		try (Stream<Product> products = dataRepository.getProductsMatchingCategoriesOrVerticalId(vertical)) {
			Iterator<Product> iterator = products.iterator();
			while (iterator.hasNext()) {
				Product p = iterator.next();
				if (aggregateOrSkip(batchAgg, p)) {
					page.add(p);
				}
				if (page.size() == BatchService.INDEXATION_PAGE_SIZE) {
					refreshed += queueWithNewestOffers(page);
					queued += page.size();
					page = new ArrayList<>(BatchService.INDEXATION_PAGE_SIZE);
				}
			}
		}
		if (!page.isEmpty()) {
			refreshed += queueWithNewestOffers(page);
			queued += page.size();
		}
		logger.info("done: Sanitisation batching for vertical {} : {} products queued, {} got offers updated in realtime during the run",
				vertical.getId(), queued, refreshed);
	}

	/**
	 * Gives a page of products the newest realtime offers, then queues it for full indexation.
	 *
	 * @param page products about to be written
	 * @return number of products whose offers were replaced
	 */
	private int queueWithNewestOffers(final List<Product> page) {
		int refreshed = dataRepository.applyNewerOfferStates(page);
		dataRepository.addToFullindexationQueue(page);
		return refreshed;
	}

	/**
	 * Applies the standard aggregation pipeline to the given product set without
	 * persisting results. Called by the batch pipeline before scoring.
	 *
	 * @param vertical vertical configuration (used only for logging)
	 * @param products products to aggregate; modified in-place
	 */
	public void aggregateProducts(final VerticalConfig vertical, final Set<Product> products) {
		logger.info("started : Sanitisation batching for {} products in vertical : {}", products.size(), idOf(vertical));
		int skipped = aggregateAll(getStandardAggregator(), products);
		logger.info("done: Sanitisation batching for {} products in vertical {}, {} skipped", products.size(),
				idOf(vertical), skipped);
	}

	/**
	 * Runs only the identity and taxonomy services against the given product set to
	 * (re-)assign or remove vertical membership. Does not modify other product data.
	 *
	 * @param vertical vertical configuration (used only for logging)
	 * @param products products to classify; modified in-place
	 */
	public void classificationAggregator(final VerticalConfig vertical, final Set<Product> products) {
		logger.info("started : Classification batching for {} products in vertical : {}", products.size(), idOf(vertical));
		int skipped = aggregateAll(classificationOnlyAggregator, products);
		logger.info("done: Classification batching for {} products in vertical {}, {} skipped", products.size(),
				idOf(vertical), skipped);
	}

	/**
	 * Runs {@code aggregator} on every product.
	 *
	 * @return number of products a service asked to skip
	 */
	private static int aggregateAll(final StandardAggregator aggregator, final Collection<Product> products) {
		int skipped = 0;
		for (Product p : products) {
			if (!aggregateOrSkip(aggregator, p)) {
				skipped++;
			}
		}
		return skipped;
	}

	/**
	 * Runs {@code aggregator} on a product.
	 *
	 * @return false when a service asked to skip the product, which is then logged
	 */
	private static boolean aggregateOrSkip(final StandardAggregator aggregator, final Product product) {
		try {
			aggregator.onProduct(product);
			return true;
		} catch (AggregationSkipException e) {
			logger.error("Skipping product {} during batched sanitisation : ", product.getId(), e);
			return false;
		}
	}

	private static String idOf(final VerticalConfig vertical) {
		return vertical == null ? null : vertical.getId();
	}

	/**
	 * Aggregates a single product and force-indexes it immediately.
	 *
	 * <p>Convenience method for on-demand single-product refresh triggered via
	 * the admin API. For bulk operations prefer the batch methods above.
	 *
	 * @param product product to refresh
	 * @throws AggregationSkipException if the product fails validation
	 */
	public void sanitizeAndSave(final Product product) throws AggregationSkipException {
		aggregate(product);
		dataRepository.forceIndex(product);
		logger.info("done : Sanitisation batching for {}", product);
	}

	/**
	 * Applies the standard aggregation pipeline to a single product (no persistence).
	 *
	 * @param product product to aggregate; modified in-place
	 * @throws AggregationSkipException if the product fails validation
	 */
	public void aggregate(final Product product) throws AggregationSkipException {
		logger.info("started : Sanitisation batching for {}", product);
		getStandardAggregator().onProduct(product);
		logger.info("done : Sanitisation batching for {}", product);
	}

	/**
	 * Applies the realtime aggregation pipeline to update {@code data} with the
	 * content of the incoming {@code df} fragment.
	 *
	 * @param df   incoming data fragment
	 * @param data existing product to enrich; modified in-place
	 * @return the updated product
	 * @throws AggregationSkipException if the product fails validation
	 */
	public Product updateOne(final DataFragment df, final Product data) throws AggregationSkipException {
		return getStandardAggregator().onDatafragment(df, data);
	}

	/**
	 * The full chain of the 7 realtime services (identity, taxonomy, attributes, usage cost, names, price, media),
	 * built once. It is shared by the realtime ingestion, the completion services, the batch and the on-demand
	 * sanitisation, from several threads: it holds no mutable state (GOU-2280).
	 *
	 * @return the shared standard aggregator
	 */
	public StandardAggregator getStandardAggregator() {
		return standardAggregator;
	}

	/**
	 * Builds a fresh {@link ScoringBatchedAggregator} for a scoring run.
	 * A new instance is created per invocation to guarantee isolated per-batch state
	 * in all score aggregation services.
	 *
	 * @return configured scoring aggregator
	 */
	public ScoringBatchedAggregator getScoringAggregator() {
		Logger aggLogger = aggregationLogger(SCORING_LOG);

		final List<AbstractAggregationService> services = new ArrayList<>();
		services.add(new CleanScoreAggregationService(aggLogger));
		services.add(new Attribute2ScoreAggregationService(aggLogger));
		services.add(new BrandScoresAggregationService(aggLogger, brandService));
		services.add(new DataCompletion2ScoreAggregationService(aggLogger));
		services.add(new EcoScoreAggregationService(aggLogger));
		services.add(new ParticipatingScoresAggregationService(aggLogger));

		return new ScoringBatchedAggregator(services);
	}

	/**
	 * @param name log-channel name, also the file name in {@code <logs>/aggregation}
	 * @return the dedicated aggregation logger of that channel, created on first use
	 */
	private Logger aggregationLogger(final String name) {
		return GenericFileLogger.initLogger(name, apiProperties.aggLogLevel(), apiProperties.logsFolder() + "/aggregation");
	}

}
