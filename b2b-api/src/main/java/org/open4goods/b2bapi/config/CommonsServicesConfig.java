package org.open4goods.b2bapi.config;

import java.io.IOException;

import org.open4goods.commons.services.BarcodeForensicsService;
import org.open4goods.commons.services.BarcodeValidationService;
import org.open4goods.commons.services.Gs1PrefixService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.ResourcePatternResolver;

/**
 * Commons module services required by the B2B API.
 */
@Configuration
public class CommonsServicesConfig {

    /**
     * Exposes the canonical open4goods barcode validator.
     *
     * @return barcode validation service
     */
    @Bean
    BarcodeValidationService barcodeValidationService() {
        return new BarcodeValidationService();
    }

    /**
     * Exposes the GS1 prefix-to-country lookup service loaded from the bundled CSV.
     *
     * @param resourceResolver Spring resource resolver for classpath lookup
     * @return GS1 prefix service
     * @throws IOException if the gs1-prefix.csv resource cannot be read
     */
    @Bean
    Gs1PrefixService gs1PrefixService(@Autowired final ResourcePatternResolver resourceResolver) throws IOException {
        return new Gs1PrefixService("classpath:/gs1-prefix.csv", resourceResolver);
    }

    /**
     * Exposes the barcode forensics service that enriches a raw barcode string
     * with type, GS1 class, country, and normalized forms.
     *
     * @param barcodeValidationService check-digit validator
     * @param gs1PrefixService GS1 prefix-to-country resolver
     * @return barcode forensics service
     */
    @Bean
    BarcodeForensicsService barcodeForensicsService(
            final BarcodeValidationService barcodeValidationService,
            final Gs1PrefixService gs1PrefixService) {
        return new BarcodeForensicsService(barcodeValidationService, gs1PrefixService);
    }

    @Bean
    org.open4goods.services.productrepository.services.ProductRepository productRepository(
            org.springframework.data.elasticsearch.core.ElasticsearchOperations elasticsearchOperations) {
        return new org.open4goods.services.productrepository.services.ProductRepository(elasticsearchOperations);
    }
}
