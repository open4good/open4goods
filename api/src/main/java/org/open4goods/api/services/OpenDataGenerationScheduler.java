package org.open4goods.api.services;

import org.open4goods.services.opendata.service.OpenDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Weekly open data generation, in the api only (GOU-2248): ui restarted at each release and shared its scheduler with
 * the sitemaps. The pass runs on its own thread; {@code open-data-config.generation-enabled} gates it.
 */
@Component
public class OpenDataGenerationScheduler {

	private static final Logger LOGGER = LoggerFactory.getLogger(OpenDataGenerationScheduler.class);

	private final OpenDataService openDataService;

	public OpenDataGenerationScheduler(OpenDataService openDataService) {
		this.openDataService = openDataService;
	}

	/**
	 * Starts the weekly pass, by default Sunday 01:30 Europe/Paris ({@code open-data-config.generation-cron}).
	 */
	@Scheduled(cron = "${open-data-config.generation-cron:0 30 1 * * SUN}",
			zone = "${open-data-config.generation-zone:Europe/Paris}")
	public void generateWeekly() {
		if (openDataService.launchGeneration()) {
			LOGGER.info("Weekly open data generation started");
		}
	}
}
