package org.open4goods.services.opendata.url;

import java.util.Locale;

import org.open4goods.model.product.Product;

/**
 * Strategy used to compute the public URL exposed in the OpenData exports for a given product.
 */
@FunctionalInterface
public interface OpenDataUrlResolver {

    /**
     * Resolve the public URL for the provided product and locale.
     *
     * @param product the product being exported
     * @param locale  the target locale
     * @return the public URL or {@code null} when none can be computed
     */
    String resolve(Product product, Locale locale);
}
