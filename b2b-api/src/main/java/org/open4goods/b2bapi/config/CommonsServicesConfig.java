package org.open4goods.b2bapi.config;

import org.open4goods.commons.services.BarcodeValidationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
}
