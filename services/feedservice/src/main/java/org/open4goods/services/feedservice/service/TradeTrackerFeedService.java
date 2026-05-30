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
 * Feed service implementation for TradeTracker.
 * 
 * @author open4goods
 */
public class TradeTrackerFeedService extends AbstractFeedService
{
    private final String customerId;
    private final String apiKey;

    public TradeTrackerFeedService(FeedConfiguration feedConfig,
                                   RemoteFileCachingService remoteFileCachingService,
                                   DataSourceConfigService dataSourceConfigService,
                                   SerialisationService serialisationService,
                                   String customerId,
                                   String apiKey)
    {
        super(feedConfig, remoteFileCachingService, dataSourceConfigService, serialisationService);
        this.customerId = customerId;
        this.apiKey = apiKey;
    }

    @Scheduled(cron = "${feed.tradetracker.cron:-}")
    public void scheduledLoad()
    {
        if (!isEnabled())
        {
            logger.info("TradeTracker feed service is disabled. Skipping scheduled load.");
            return;
        }
        logger.info("Scheduled refresh of TradeTracker datasources initiated.");
        load();
    }

    @Override
    protected Set<DataSourceProperties> loadDatasources() throws Exception
    {
        logger.info("TradeTracker loadDatasources called.");
        return Collections.emptySet();
    }

    @Override
    public String getProviderName()
    {
        return "TradeTracker";
    }

    @Override
    public Set<AffiliationCapability> getCapabilities()
    {
        return Set.of(AffiliationCapability.FEEDS);
    }

    private boolean isEnabled()
    {
        return feedConfig.getTradetracker() != null && feedConfig.getTradetracker().isEnabled();
    }
}
