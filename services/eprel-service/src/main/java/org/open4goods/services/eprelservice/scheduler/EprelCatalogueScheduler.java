package org.open4goods.services.eprelservice.scheduler;

import java.io.IOException;

import org.open4goods.services.eprelservice.service.EprelCatalogueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically refreshes the EPREL catalogue. The refresh follows a cron expression so that
 * frequent deployments cannot postpone it, unlike a delay counted from application start.
 */
@Component
public class EprelCatalogueScheduler
{
    private static final Logger LOGGER = LoggerFactory.getLogger(EprelCatalogueScheduler.class);

    private final EprelCatalogueService catalogueService;

    /**
     * Creates the scheduler.
     *
     * @param catalogueService service executing the refresh
     */
    public EprelCatalogueScheduler(EprelCatalogueService catalogueService)
    {
        this.catalogueService = catalogueService;
    }

    /**
     * Triggers the catalogue refresh job.
     *
     * @throws IOException if at least one EPREL group could not be refreshed
     */
    @Scheduled(cron = "${eprel.refresh-cron:0 0 3 */2 * *}")
    public void schedule() throws IOException
    {
        LOGGER.info("Starting scheduled EPREL catalogue synchronisation");
        catalogueService.refreshCatalogue();
    }
}
