package org.open4goods.api.services;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import jakarta.annotation.PreDestroy;
import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.services.reviewgeneration.service.ReviewGenerationHook;
import org.open4goods.verticals.VerticalsConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * {@link ReviewGenerationHook} that re-triggers EPREL and Icecat completion after the
 * remote-source fetching stage succeeds for a product that previously lacked EPREL data.
 * <p>
 * Many products carry the {@code missing_eprel} exclusion cause because EPREL lookup ran
 * before any manufacturer-side data (official URL, PDF, specs) was available. Once the
 * scraping step has enriched the product, a fresh completion pass often resolves the EPREL
 * match or surfaces Icecat attributes that were previously missing.
 * </p>
 */
@Component
public class PostFetchEnrichmentHook implements ReviewGenerationHook {

    private static final Logger logger = LoggerFactory.getLogger(PostFetchEnrichmentHook.class);

    private final CompletionFacadeService completionFacadeService;
    private final VerticalsConfigService verticalsConfigService;
    private final ProductRepository productRepository;
    private final long timeoutSeconds;
    private final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

    public PostFetchEnrichmentHook(CompletionFacadeService completionFacadeService,
            VerticalsConfigService verticalsConfigService, ProductRepository productRepository,
            @Value("${review.generation.post-fetch-enrichment-timeout-seconds:45}") long timeoutSeconds) {
        this.completionFacadeService = completionFacadeService;
        this.verticalsConfigService = verticalsConfigService;
        this.productRepository = productRepository;
        this.timeoutSeconds = timeoutSeconds;
    }

    /**
     * No-op: this hook only reacts to the fetch stage, not to full review generation.
     *
     * @param product the product with the newly generated review
     */
    @Override
    public void onReviewGenerated(Product product) {
    }

    /**
     * Re-triggers EPREL and Icecat enrichment when the product previously lacked EPREL
     * data (indicated by the {@code missing_eprel} exclusion cause). The re-run is
     * fire-and-forget: failures are logged but do not propagate to the caller.
     *
     * @param product the product with newly persisted review facts
     */
    @Override
    public void onSourcesFetched(Product product) {
        onSourcesFetched(product, product != null && product.getExternalIds() != null
                && product.getExternalIds().getEprel() != null);
    }

    @Override
    public void onSourcesFetched(Product product, boolean hadEprelBeforeFetch) {
        if (product == null || hadEprelBeforeFetch) {
            return;
        }
        VerticalConfig vertical = verticalsConfigService.getConfigByIdOrDefault(product.getVertical());
        if (vertical == null) {
            logger.warn("Cannot trigger post-fetch enrichment for UPC {}: vertical '{}' not found",
                    product.getId(), product.getVertical());
            return;
        }
        if (vertical.getEprelGroupNames() == null || vertical.getEprelGroupNames().isEmpty()) {
            logger.debug("Skipping post-fetch enrichment for UPC {}: vertical '{}' has no EPREL groups",
                    product.getId(), product.getVertical());
            return;
        }
        logger.info("Post-fetch enrichment triggered for UPC {} (no EPREL before fetch)", product.getId());
        CompletableFuture<Void> completion = null;
        try {
            completion = CompletableFuture.runAsync(() -> {
                completionFacadeService.processAll(Set.of(product), vertical);
                productRepository.forceIndex(product);
            }, executorService);
            completion.get(timeoutSeconds, TimeUnit.SECONDS);
            logger.info("Post-fetch enrichment completed for UPC {}", product.getId());
        } catch (TimeoutException e) {
            if (completion != null) {
                completion.cancel(true);
            }
            logger.warn("Post-fetch enrichment timed out after {}s for UPC {}", timeoutSeconds, product.getId());
        } catch (Exception e) {
            logger.error("Post-fetch enrichment failed for UPC {}: {}", product.getId(), e.getMessage(), e);
        }
    }

    /**
     * Stops the timeout executor on API shutdown.
     */
    @PreDestroy
    public void shutdown() {
        executorService.shutdownNow();
    }
}
