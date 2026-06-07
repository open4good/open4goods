
package org.open4goods.api.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.open4goods.api.config.yml.ApiProperties;
import org.open4goods.embedding.config.DjlEmbeddingProperties;
import org.open4goods.embedding.service.TextEmbeddingService;
import org.open4goods.api.services.aggregation.AbstractAggregationService;
import org.open4goods.api.services.aggregation.aggregator.ScoringBatchedAggregator;
import org.open4goods.api.services.aggregation.aggregator.StandardAggregator;
import org.open4goods.api.services.aggregation.services.batch.scores.Attribute2ScoreAggregationService;
import org.open4goods.api.services.aggregation.services.batch.scores.CleanScoreAggregationService;
import org.open4goods.api.services.aggregation.services.batch.scores.DataCompletion2ScoreAggregationService;
import org.open4goods.api.services.aggregation.services.batch.scores.EcoScoreAggregationService;
import org.open4goods.api.services.aggregation.services.batch.scores.ParticipatingScoresAggregationService;
import org.open4goods.api.services.aggregation.services.batch.scores.SustainalyticsAggregationService;
import org.open4goods.api.services.aggregation.services.realtime.AttributeRealtimeAggregationService;
import org.open4goods.api.services.aggregation.services.realtime.IdentityAggregationService;
import org.open4goods.api.services.aggregation.services.realtime.MediaAggregationService;
import org.open4goods.api.services.aggregation.services.realtime.NamesAggregationService;
import org.open4goods.api.services.aggregation.services.realtime.PriceAggregationService;
import org.open4goods.api.services.aggregation.services.realtime.ReviewMetadataAggregationService;
import org.open4goods.api.services.aggregation.services.realtime.TaxonomyRealTimeAggregationService;
import org.open4goods.api.services.aggregation.services.realtime.UsageCostAggregationService;
import org.open4goods.commons.exceptions.AggregationSkipException;
import org.open4goods.commons.helper.GenericFileLogger;
import org.open4goods.brand.service.BrandScoreService;
import org.open4goods.brand.service.BrandService;
import org.open4goods.commons.services.BarcodeValidationService;
import org.open4goods.commons.services.DataSourceConfigService;
import org.open4goods.commons.services.Gs1PrefixService;
import org.open4goods.commons.services.textgen.BlablaService;
import org.open4goods.icecat.services.IcecatFeatureResolver;
import org.open4goods.icecat.services.IcecatService;
import org.open4goods.model.StandardiserService;
import org.open4goods.model.datafragment.DataFragment;
import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.evaluation.service.EvaluationService;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.open4goods.verticals.GoogleTaxonomyService;
import org.open4goods.verticals.VerticalsConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;

import jakarta.annotation.PreDestroy;

/**
 * Central factory and orchestrator for the product aggregation pipeline.
 *
 * <p>Assembles and invokes two kinds of aggregators:
 * <ul>
 *   <li><strong>StandardAggregator</strong> — used in realtime mode
 *       ({@link #updateOne(DataFragment, Product)}) and during batch sanitisation
 *       ({@link #sanitizeAll()}, {@link #sanitizeVertical(VerticalConfig)},
 *       {@link #aggregateProducts(VerticalConfig, Set)}).</li>
 *   <li><strong>ScoringBatchedAggregator</strong> — used for scoring
 *       ({@link #score(VerticalConfig)}, {@link #scoreAll()}).</li>
 * </ul>
 *
 * <p>A single {@link StandardAggregator} instance ({@code realtimeAggregator}) is
 * kept alive for the lifetime of this service to handle incoming DataFragments
 * without re-building the service chain on every request. All batch methods
 * create a fresh aggregator per invocation to avoid cross-run state leakage.
 *
 * <p>TODO: Maintain a state machine to prevent concurrent batch launches on the
 * same vertical. See docs/adr/ for the proposed design.
 */
public class AggregationFacadeService {

	protected static final Logger logger = LoggerFactory.getLogger(AggregationFacadeService.class);

	private final EvaluationService evaluationService;
	private final StandardiserService standardiserService;
	private final AutowireCapableBeanFactory autowireBeanFactory;
	private final ProductRepository dataRepository;
	private final ApiProperties apiProperties;
	private final Gs1PrefixService gs1prefixService;
	private final DataSourceConfigService dataSourceConfigService;
	private final VerticalsConfigService verticalConfigService;
	private final BarcodeValidationService barcodeValidationService;
	private final BrandService brandService;
	private final BrandScoreService brandScoreService;
	private final GoogleTaxonomyService taxonomyService;
	private final BlablaService blablaService;
	private final IcecatService icecatFeatureService;
	private final IcecatFeatureResolver icecatFeatureResolver;
	private final TextEmbeddingService embeddingService;
	private final DjlEmbeddingProperties embeddingProperties;
	private final SerialisationService serialisationService;

	/** Shared realtime aggregator — assembled once and reused across all DataFragment ingestion calls. */
	private final StandardAggregator realtimeAggregator;

	public AggregationFacadeService(final EvaluationService evaluationService,
			final StandardiserService standardiserService,
			final AutowireCapableBeanFactory autowireBeanFactory,
			final ProductRepository aggregatedDataRepository,
			final ApiProperties apiProperties,
			final Gs1PrefixService gs1prefixService,
			final DataSourceConfigService dataSourceConfigService,
			final VerticalsConfigService configService,
			final BarcodeValidationService barcodeValidationService,
			final BrandService brandService,
			final GoogleTaxonomyService taxonomyService,
			final BlablaService blablaService,
			final IcecatService icecatFeatureService,
			final IcecatFeatureResolver icecatFeatureResolver,
			final SerialisationService serialisationService,
			final BrandScoreService brandScoreService,
			final TextEmbeddingService embeddingService,
			final DjlEmbeddingProperties embeddingProperties) {
		this.evaluationService = evaluationService;
		this.standardiserService = standardiserService;
		this.autowireBeanFactory = autowireBeanFactory;
		this.dataRepository = aggregatedDataRepository;
		this.apiProperties = apiProperties;
		this.gs1prefixService = gs1prefixService;
		this.dataSourceConfigService = dataSourceConfigService;
		this.verticalConfigService = configService;
		this.brandService = brandService;
		this.barcodeValidationService = barcodeValidationService;
		this.taxonomyService = taxonomyService;
		this.blablaService = blablaService;
		this.icecatFeatureService = icecatFeatureService;
		this.icecatFeatureResolver = icecatFeatureResolver;
		this.embeddingService = embeddingService;
		this.embeddingProperties = embeddingProperties;
		this.serialisationService = serialisationService;
		this.brandScoreService = brandScoreService;
		this.realtimeAggregator = getStandardAggregator("realtime");
	}

	/**
	 * Scores all enabled verticals in sequence using the batch scoring aggregator.
	 */
	public void scoreAll() {
		for (VerticalConfig vertical : verticalConfigService.getConfigsWithoutDefault()) {
			score(vertical);
		}
	}

	/**
	 * Scores a single vertical: loads all products with a valid date, runs the
	 * {@link ScoringBatchedAggregator}, then queues the full product set for
	 * Elasticsearch re-indexation.
	 *
	 * @param vertical vertical configuration to score
	 */
	public void score(final VerticalConfig vertical) {
		logger.info("Score batching for {}", vertical.getId());

		ScoringBatchedAggregator batchAgg = getScoringAggregator();
		List<Product> productBag = dataRepository.exportVerticalWithValidDate(vertical, false).toList();
		batchAgg.score(productBag, verticalConfigService.getConfigByIdOrDefault(vertical.getId()));
		logger.info("Score batching : indexing {} products", productBag.size());
		dataRepository.addToFullindexationQueue(productBag);
	}

	/**
	 * Scores a pre-filtered set of products within the given vertical, without
	 * persisting results. Useful for triggered re-scoring of a subset.
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
	 * Re-sanitises every product in the repository and persists the result.
	 * Products that raise {@link AggregationSkipException} are logged and skipped.
	 */
	public void sanitizeAll() {
		logger.info("started : Sanitisation batching for all items");
		StandardAggregator batchAgg = getStandardAggregator();

		dataRepository.exportAll().forEach(p -> {
			try {
				batchAgg.onProduct(p);
				dataRepository.index(p);
			} catch (AggregationSkipException e) {
				logger.error("Skipping product during batched sanitisation : ", e);
			}
		});
		logger.info("done: Sanitisation batching for all items");
	}

	/**
	 * Re-sanitises all products in all enabled verticals. Delegates to
	 * {@link #sanitizeVertical(VerticalConfig)} per vertical.
	 */
	public void sanitizeAllVerticals() {
		for (VerticalConfig vertical : verticalConfigService.getConfigsWithoutDefault()) {
			sanitizeVertical(vertical);
		}
	}

	/**
	 * Re-sanitises all products matching the given vertical's categories, then
	 * persists the results. This relies on category matching rather than the
	 * pre-assigned vertical field, so products that have drifted from their
	 * vertical assignment are also corrected.
	 *
	 * @param vertical vertical configuration to process
	 */
	public void sanitizeVertical(final VerticalConfig vertical) {
		logger.info("started : Sanitisation batching for vertical : {}", vertical);
		StandardAggregator batchAgg = getStandardAggregator();

		dataRepository.getProductsMatchingCategoriesOrVerticalId(vertical).forEach(p -> {
			try {
				batchAgg.onProduct(p);
				dataRepository.index(p);
			} catch (AggregationSkipException e) {
				logger.error("Skipping product during batched sanitisation : ", e);
			}
		});
		logger.info("done: Sanitisation batching for all items");
	}

	/**
	 * Applies the standard aggregation pipeline to the given product set without
	 * persisting results. Called by the batch pipeline before scoring.
	 *
	 * @param vertical vertical configuration (used only for logging)
	 * @param products products to aggregate; modified in-place
	 */
	public void aggregateProducts(final VerticalConfig vertical, final Set<Product> products) {
		logger.info("started : Sanitisation batching for {} products in vertical : {}", products.size(), vertical);
		StandardAggregator batchAgg = getStandardAggregator();

		products.forEach(p -> {
			try {
				batchAgg.onProduct(p);
			} catch (AggregationSkipException e) {
				logger.error("Skipping product during batched sanitisation : ", e);
			}
		});
		logger.info("done: Sanitisation batching for all items");
	}

	/**
	 * Runs only the identity and taxonomy services against the given product set to
	 * (re-)assign or remove vertical membership. Does not modify other product data.
	 *
	 * @param vertical vertical configuration (used only for logging)
	 * @param products products to classify; modified in-place
	 */
	public void classificationAggregator(final VerticalConfig vertical, final Set<Product> products) {
		logger.info("started : Classification batching for {} products in vertical : {}", products.size(), vertical);
		StandardAggregator batchAgg = getCategorieClassificationAggregator();

		products.forEach(p -> {
			try {
				batchAgg.onProduct(p);
			} catch (AggregationSkipException e) {
				logger.error("Skipping product during batched sanitisation : ", e);
			}
		});
		logger.info("done: classification");
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
		StandardAggregator batchAgg = getStandardAggregator();
		batchAgg.onProduct(product);
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
		return realtimeAggregator.onDatafragment(df, data);
	}

	/** Closes the shared realtime aggregator on application shutdown. */
	@PreDestroy
	public void shutdown() {
		realtimeAggregator.close();
	}

	/**
	 * Builds a fresh {@link StandardAggregator} containing the full realtime
	 * service chain. A dedicated aggregation log file is created for the given name.
	 *
	 * @param name log-channel name (e.g. {@code "realtime"}, {@code "sanitisation"})
	 * @return configured aggregator instance (Spring beans are not yet injected into services)
	 */
	public StandardAggregator getStandardAggregator(final String name) {
		Logger aggLogger = GenericFileLogger.initLogger(name, apiProperties.aggLogLevel(),
				apiProperties.logsFolder() + "/aggregation");

		final List<AbstractAggregationService> services = new ArrayList<>();
		services.add(new IdentityAggregationService(aggLogger, gs1prefixService, barcodeValidationService));
		services.add(new TaxonomyRealTimeAggregationService(aggLogger, verticalConfigService, taxonomyService));
		services.add(new AttributeRealtimeAggregationService(verticalConfigService, brandService, aggLogger, icecatFeatureResolver));
		services.add(new UsageCostAggregationService(aggLogger));
		services.add(new NamesAggregationService(aggLogger, verticalConfigService, blablaService,
				embeddingService, embeddingProperties));
		services.add(new PriceAggregationService(aggLogger));
		services.add(new MediaAggregationService(aggLogger));
		services.add(new ReviewMetadataAggregationService(aggLogger));

		final StandardAggregator ret = new StandardAggregator(services, verticalConfigService);
		autowireBeanFactory.autowireBean(ret);
		return ret;
	}

	/**
	 * Builds a {@link StandardAggregator} containing only the identity and taxonomy
	 * services, used to (re-)classify products without touching other fields.
	 *
	 * @return configured classification-only aggregator
	 */
	public StandardAggregator getCategorieClassificationAggregator() {
		Logger aggLogger = GenericFileLogger.initLogger("categores_classif", apiProperties.aggLogLevel(),
				apiProperties.logsFolder() + "/aggregation");

		final List<AbstractAggregationService> services = new ArrayList<>();
		services.add(new IdentityAggregationService(aggLogger, gs1prefixService, barcodeValidationService));
		services.add(new TaxonomyRealTimeAggregationService(aggLogger, verticalConfigService, taxonomyService));

		final StandardAggregator ret = new StandardAggregator(services, verticalConfigService);
		autowireBeanFactory.autowireBean(ret);
		return ret;
	}

	/**
	 * Builds a fresh {@link ScoringBatchedAggregator} for a scoring run.
	 * A new instance is created per invocation to guarantee isolated per-batch state
	 * in all score aggregation services.
	 *
	 * @return configured scoring aggregator
	 */
	public ScoringBatchedAggregator getScoringAggregator() {
		Logger aggLogger = GenericFileLogger.initLogger("score", apiProperties.aggLogLevel(),
				apiProperties.logsFolder() + "/aggregation");

		final List<AbstractAggregationService> services = new ArrayList<>();
		services.add(new CleanScoreAggregationService(aggLogger));
		services.add(new Attribute2ScoreAggregationService(aggLogger));
		services.add(new SustainalyticsAggregationService(aggLogger, brandService, brandScoreService));
		services.add(new DataCompletion2ScoreAggregationService(aggLogger));
		services.add(new EcoScoreAggregationService(aggLogger));
		services.add(new ParticipatingScoresAggregationService(aggLogger));

		final ScoringBatchedAggregator ret = new ScoringBatchedAggregator(services);
		autowireBeanFactory.autowireBean(ret);
		return ret;
	}

	/**
	 * Shorthand for {@link #getStandardAggregator(String)} using the
	 * {@code "sanitisation"} log-channel name.
	 *
	 * @return configured sanitisation aggregator
	 */
	public StandardAggregator getStandardAggregator() {
		return getStandardAggregator("sanitisation");
	}

}
