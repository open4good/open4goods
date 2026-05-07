package org.open4goods.services.reviewgeneration.service;

import org.open4goods.model.product.Product;
import org.open4goods.services.googleindexation.service.GoogleIndexationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Hook that submits generated product URLs to the Google Indexing API once AI review generation succeeds.
 */
@Component
public class GoogleIndexationReviewGenerationHook implements ReviewGenerationHook {

    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleIndexationReviewGenerationHook.class);

    private final GoogleIndexationService googleIndexationService;

    /**
     * Build the hook with the Google indexation service dependency.
     *
     * @param googleIndexationService service used to submit URL updates
     */
    public GoogleIndexationReviewGenerationHook(GoogleIndexationService googleIndexationService) {
        this.googleIndexationService = googleIndexationService;
    }

    /**
     * Trigger asynchronous URL submission after successful review generation.
     *
     * @param product product that has just received an AI review
     */
    @Override
    public void onReviewGenerated(Product product) {
        String url = product == null ? null : product.url("fr");
        if (!StringUtils.hasText(url)) {
            LOGGER.debug("Skipping Google indexation hook because product URL is blank");
            return;
        }
        googleIndexationService.submitUrl(url);
    }
}
