package org.open4goods.commons.services;

import org.open4goods.commons.helper.GenericFileLogger;
import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.verticals.VerticalsConfigService;
import org.slf4j.Logger;

import ch.qos.logback.classic.Level;

/**
 * Base class for all product completion services.
 *
 * <p>A completion service enriches products by fetching data from an external source
 * (e.g. Icecat live API, EPREL, resource crawling). Each service implements
 * {@link #processProduct(VerticalConfig, Product)} and {@link #shouldProcess(VerticalConfig, Product)}
 * to define when and how enrichment is applied.
 *
 * <p>Subclasses are registered in {@code CompletionFacadeService} and called per-product
 * during the completion pipeline.
 */
public abstract class AbstractCompletionService {

    protected final ProductRepository dataRepository;
    protected final VerticalsConfigService verticalConfigService;
    protected final Logger logger;

    protected AbstractCompletionService(
            ProductRepository dataRepository,
            VerticalsConfigService verticalConfigService,
            String logFolder,
            Level logLevel) {
        this.dataRepository = dataRepository;
        this.verticalConfigService = verticalConfigService;
        this.logger = GenericFileLogger.initLogger(
                "completion-" + getClass().getSimpleName().toLowerCase(), logLevel, logFolder);
    }

    /**
     * Runs completion for all verticals and all products in each vertical.
     *
     * @param withExcluded whether to include excluded products
     */
    public void completeAll(boolean withExcluded) {
        completeAll(null, withExcluded);
    }

    /**
     * Runs completion for all verticals, up to {@code max} products per vertical.
     *
     * @param max          maximum number of products per vertical (null = unlimited)
     * @param withExcluded whether to include excluded products
     */
    public void completeAll(Integer max, boolean withExcluded) {
        logger.info("Completion for all verticals");
        for (VerticalConfig vConf : verticalConfigService.getConfigsWithoutDefault()) {
            complete(vConf, max, withExcluded);
        }
    }

    /**
     * Runs completion for all products in the given vertical.
     *
     * @param vertical     the vertical to process
     * @param withExcluded whether to include excluded products
     */
    public void complete(VerticalConfig vertical, boolean withExcluded) {
        complete(vertical, null, withExcluded);
    }

    /**
     * Runs completion for up to {@code limit} products in the given vertical.
     *
     * @param vertical     the vertical to process
     * @param limit        maximum number of products (null = unlimited)
     * @param withExcluded whether to include excluded products
     */
    public void complete(VerticalConfig vertical, Integer limit, boolean withExcluded) {
        logger.info("Completing {} products {}", limit == null ? "all" : limit, vertical.getId());
        var products = dataRepository.exportVerticalWithValidDate(vertical, withExcluded);
        if (limit != null) {
            products = products.limit(limit);
        }
        products.forEach(data -> {
            completeAndIndexProduct(vertical, data);
        });
    }

    /**
     * Processes a single product and immediately re-indexes it.
     *
     * @param vertical the vertical context
     * @param data     the product to process
     */
    public void completeAndIndexProduct(VerticalConfig vertical, Product data) {
        processProduct(vertical, data);
        dataRepository.forceIndex(data);
    }

    /**
     * Calls {@link #processProduct(VerticalConfig, Product)} only if
     * {@link #shouldProcess(VerticalConfig, Product)} returns {@code true}.
     *
     * @param vertical the vertical context
     * @param data     the product to conditionally process
     */
    public void process(VerticalConfig vertical, Product data) {
        if (shouldProcess(vertical, data)) {
            logger.info("Completing {} with {}", data, this.getClass().getSimpleName());
            processProduct(vertical, data);
        } else {
            logger.info("Skipping completion of {} with {}", data, this.getClass().getSimpleName());
        }
    }

    /**
     * Performs the actual enrichment of the product.
     *
     * @param vertical the vertical context
     * @param data     the product to enrich
     */
    public abstract void processProduct(VerticalConfig vertical, Product data);

    /**
     * Determines whether this product should be processed now.
     * Typically checks the datasource timestamp in {@code data.getDatasourceCodes()}.
     *
     * @param vertical the vertical context
     * @param data     the product to evaluate
     * @return {@code true} if enrichment should run
     */
    public abstract boolean shouldProcess(VerticalConfig vertical, Product data);

    /**
     * Returns the datasource name used as key in {@code datasourceCodes} to track
     * the last completion timestamp.
     *
     * @return datasource name (e.g. "icecat.biz", "eprel")
     */
    public abstract String getDatasourceName();
}
