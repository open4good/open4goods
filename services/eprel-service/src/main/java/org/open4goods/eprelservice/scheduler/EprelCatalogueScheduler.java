package org.open4goods.eprelservice.scheduler;

import org.open4goods.eprelservice.config.EprelServiceProperties;
import org.open4goods.eprelservice.service.EprelCatalogueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically refreshes the EPREL catalogue.
 */
@Component
public class EprelCatalogueScheduler
{
    private static final Logger LOGGER = LoggerFactory.getLogger(EprelCatalogueScheduler.class);

    private final EprelCatalogueService catalogueService;
    private final EprelServiceProperties properties;

    /**
     * Creates the scheduler.
     *
     * @param catalogueService service executing the refresh
     * @param properties       configuration defining the frequency
     */
    public EprelCatalogueScheduler(EprelCatalogueService catalogueService, EprelServiceProperties properties)
    {
        this.catalogueService = catalogueService;
        this.properties = properties;
    }

    /**
     * Triggers the catalogue refresh job.
     */
    @Scheduled(fixedDelayString = "#{${open4goods.eprel.scheduling-frequency-days:2} * 24 * 60 * 60 * 1000}")
    public void schedule()
    {
        LOGGER.info("Starting scheduled EPREL catalogue synchronisation every {} day(s)", properties.getSchedulingFrequencyDays());
        catalogueService.refreshCatalogue();
    }
}
