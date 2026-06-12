package org.open4goods.brand.service;

/**
 * Functional interface to load company specific JSON metadata.
 */
@FunctionalInterface
public interface CompanyLoader {
    /**
     * Loads the company JSON content for a given company-id.
     *
     * @param companyId the company slug/id
     * @return the JSON content as a string
     * @throws Exception if loading fails
     */
    String loadCompany(String companyId) throws Exception;
}
