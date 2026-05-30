package org.open4goods.services.feedservice.service;

import java.util.Collections;
import java.util.Set;

import org.open4goods.commons.config.yml.datasource.DataSourceProperties;
import org.open4goods.commons.services.DataSourceConfigService;
import org.open4goods.model.affiliation.AffiliationCapability;
import org.open4goods.services.feedservice.config.FeedConfiguration;
import org.open4goods.services.remotefilecaching.service.RemoteFileCachingService;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Feed service implementation for Webgains.
 * 
 * @author open4goods
 */
public class WebgainsFeedService extends AbstractFeedService
{
    private final String apiKey;

    public WebgainsFeedService(FeedConfiguration feedConfig,
                               RemoteFileCachingService remoteFileCachingService,
                               DataSourceConfigService dataSourceConfigService,
                               SerialisationService serialisationService,
                               String apiKey)
    {
        super(feedConfig, remoteFileCachingService, dataSourceConfigService, serialisationService);
        this.apiKey = apiKey;
    }

    @Scheduled(cron = "${feed.webgains.cron:-}")
    public void scheduledLoad()
    {
        if (!isEnabled())
        {
            logger.info("Webgains feed service is disabled. Skipping scheduled load.");
            return;
        }
        logger.info("Scheduled refresh of Webgains datasources initiated.");
        load();
    }

    @Override
    protected Set<DataSourceProperties> loadDatasources() throws Exception
    {
        logger.info("Webgains loadDatasources called.");
        return Collections.emptySet();
    }

    @Override
    public String getProviderName()
    {
        return "Webgains";
    }

    @Override
    public Set<AffiliationCapability> getCapabilities()
    {
        return Set.of(AffiliationCapability.FEEDS);
    }

    private boolean isEnabled()
    {
        return feedConfig.getWebgains() != null && feedConfig.getWebgains().isEnabled();
    }
}
