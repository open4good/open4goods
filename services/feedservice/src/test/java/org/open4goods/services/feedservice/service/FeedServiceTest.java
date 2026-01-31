package org.open4goods.services.feedservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.open4goods.commons.config.yml.datasource.DataSourceProperties;
import org.open4goods.commons.services.DataSourceConfigService;

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
