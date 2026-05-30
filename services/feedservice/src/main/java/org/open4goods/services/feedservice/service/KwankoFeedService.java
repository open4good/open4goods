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
 * Feed service implementation for Kwanko.
 * 
 * @author open4goods
 */
public class KwankoFeedService extends AbstractFeedService
{
    private final String token;

    public KwankoFeedService(FeedConfiguration feedConfig,
                             RemoteFileCachingService remoteFileCachingService,
                             DataSourceConfigService dataSourceConfigService,
                             SerialisationService serialisationService,
                             String token)
    {
        super(feedConfig, remoteFileCachingService, dataSourceConfigService, serialisationService);
        this.token = token;
    }

    @Scheduled(cron = "${feed.kwanko.cron:-}")
    public void scheduledLoad()
    {
        if (!isEnabled())
        {
            logger.info("Kwanko feed service is disabled. Skipping scheduled load.");
            return;
        }
        logger.info("Scheduled refresh of Kwanko datasources initiated.");
        load();
    }

    @Override
    protected Set<DataSourceProperties> loadDatasources() throws Exception
    {
        logger.info("Kwanko loadDatasources called.");
        return Collections.emptySet();
    }

    @Override
    public String getProviderName()
    {
        return "Kwanko";
    }

    @Override
    public Set<AffiliationCapability> getCapabilities()
    {
        return Set.of(AffiliationCapability.FEEDS);
    }

    private boolean isEnabled()
    {
        return feedConfig.getKwanko() != null && feedConfig.getKwanko().isEnabled();
    }
}
