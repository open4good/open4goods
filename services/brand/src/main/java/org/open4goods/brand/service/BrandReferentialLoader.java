package org.open4goods.brand.service;

/**
 * Loads the raw JSON brand referential.
 */
@FunctionalInterface
public interface BrandReferentialLoader {

    /**
     * @return raw JSON content for the brand referential
     * @throws Exception when the referential cannot be loaded
     */
    String load() throws Exception;
}
