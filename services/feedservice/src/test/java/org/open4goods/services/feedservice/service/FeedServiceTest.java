package org.open4goods.services.feedservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.open4goods.commons.config.yml.datasource.DataSourceProperties;
import org.open4goods.commons.services.DataSourceConfigService;
import org.open4goods.model.affiliation.AffiliationProgram;
import org.open4goods.model.affiliation.AffiliationPromotion;
import org.open4goods.model.affiliation.AffiliationTransaction;

/**
 * Tests for FeedService datasource matching.
 */
class FeedServiceTest
{

    @Test
    void getFeedsByDatasourceNameMatchesDatasourceConfigName()
    {
        DataSourceProperties datasource = datasource("Partner", "provider-42");
        FeedService feedService = buildFeedService(Set.of(datasource));

        Set<DataSourceProperties> result = feedService.getFeedsByDatasourceName("provider 42");

        assertThat(result).containsExactly(datasource);
    }

    @Test
    void getFeedsByDatasourceNameMatchesDatasourceName()
    {
        DataSourceProperties datasource = datasource("Acme Partner", "acme-partner");
        FeedService feedService = buildFeedService(Set.of(datasource));

        Set<DataSourceProperties> result = feedService.getFeedsByDatasourceName("acme partner");

        assertThat(result).containsExactly(datasource);
    }

    @Test
    void getProgramsAggregatesFromAllProviders() {
        AbstractFeedService provider = mock(AbstractFeedService.class);
        DataSourceConfigService dataSourceConfigService = mock(DataSourceConfigService.class);
        
        AffiliationProgram program = new AffiliationProgram();
        program.setProgramId("123");
        
        when(provider.getPrograms()).thenReturn(List.of(program));
        when(dataSourceConfigService.datasourceConfigs()).thenReturn(Map.of());
        
        FeedService feedService = new FeedService(List.of(provider), dataSourceConfigService);
        Collection<AffiliationProgram> result = feedService.getPrograms();
        
        assertThat(result).containsExactly(program);
    }

    @Test
    void getPromotionsAggregatesFromAllProviders() {
        AbstractFeedService provider = mock(AbstractFeedService.class);
        DataSourceConfigService dataSourceConfigService = mock(DataSourceConfigService.class);
        
        AffiliationPromotion promotion = new AffiliationPromotion();
        promotion.setProgramId("123");
        
        when(provider.getPromotions()).thenReturn(List.of(promotion));
        when(dataSourceConfigService.datasourceConfigs()).thenReturn(Map.of());
        
        FeedService feedService = new FeedService(List.of(provider), dataSourceConfigService);
        Collection<AffiliationPromotion> result = feedService.getPromotions();
        
        assertThat(result).containsExactly(promotion);
    }

    @Test
    void getTransactionsAggregatesFromAllProviders() {
        AbstractFeedService provider = mock(AbstractFeedService.class);
        DataSourceConfigService dataSourceConfigService = mock(DataSourceConfigService.class);
        
        AffiliationTransaction transaction = new AffiliationTransaction();
        transaction.setProgramId("123");
        
        Instant from = Instant.now();
        Instant to = Instant.now();
        when(provider.getTransactions(from, to)).thenReturn(List.of(transaction));
        when(dataSourceConfigService.datasourceConfigs()).thenReturn(Map.of());
        
        FeedService feedService = new FeedService(List.of(provider), dataSourceConfigService);
        Collection<AffiliationTransaction> result = feedService.getTransactions(from, to);
        
        assertThat(result).containsExactly(transaction);
    }

    @Test
    void buildTrackingLinkDelegatesToMatchingProvider() {
        AbstractFeedService provider = mock(AbstractFeedService.class);
        DataSourceConfigService dataSourceConfigService = mock(DataSourceConfigService.class);
        
        when(provider.getProviderName()).thenReturn("Effiliation");
        when(provider.buildTrackingLink("123", "https://target", Map.of("sub1", "val1"))).thenReturn("https://track-link");
        when(dataSourceConfigService.datasourceConfigs()).thenReturn(Map.of());
        
        FeedService feedService = new FeedService(List.of(provider), dataSourceConfigService);
        String link = feedService.buildTrackingLink("Effiliation", "123", "https://target", Map.of("sub1", "val1"));
        
        assertThat(link).isEqualTo("https://track-link");
    }

    private FeedService buildFeedService(Set<DataSourceProperties> datasources)
    {
        AbstractFeedService feedProvider = mock(AbstractFeedService.class);
        DataSourceConfigService dataSourceConfigService = mock(DataSourceConfigService.class);
        when(feedProvider.getDatasources()).thenReturn(datasources);
        when(dataSourceConfigService.datasourceConfigs()).thenReturn(Map.of());
        return new FeedService(List.of(feedProvider), dataSourceConfigService);
    }

    private DataSourceProperties datasource(String name, String configName)
    {
        DataSourceProperties properties = new DataSourceProperties();
        properties.setName(name);
        properties.setDatasourceConfigName(configName);
        return properties;
    }
}
