package org.open4goods.services.productalert.config;

import java.time.Clock;

import org.open4goods.commons.services.BarcodeValidationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Infrastructure configuration for the product alert service.
 */
@Configuration
public class ProductAlertConfiguration
{
    /**
     * Creates the shared GTIN validation service.
     *
     * @return barcode validation service
     */
    @Bean
    public BarcodeValidationService barcodeValidationService()
    {
        return new BarcodeValidationService();
    }

    /**
     * Creates the service clock.
     *
     * @return UTC system clock
     */
    @Bean
    public Clock productAlertClock()
    {
        return Clock.systemUTC();
    }
}
