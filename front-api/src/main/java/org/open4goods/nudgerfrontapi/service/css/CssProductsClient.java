package org.open4goods.nudgerfrontapi.service.css;

import com.google.shopping.css.v1.CssProductInput;

/**
 * Port towards the Google CSS API (GOU-2081). Implementations: {@link GoogleCssProductsClient} (real calls) and
 * {@link SimulatedCssProductsClient} (tests and dry run).
 */
public interface CssProductsClient {

    /**
     * Inserts a product input; Google treats it as an upsert on {@code contentLanguage + feedLabel + rawProvidedId}.
     *
     * @param input input to send
     * @throws CssClientException when Google refuses or the call fails
     */
    void insert(CssProductInput input);

    /**
     * Deletes a product input.
     *
     * @param rawProvidedId   identifier given at insert time (the GTIN)
     * @param contentLanguage content language of the input
     * @param feedLabel       feed label of the input
     * @throws CssClientException when Google refuses or the call fails
     */
    void delete(String rawProvidedId, String contentLanguage, String feedLabel);
}
