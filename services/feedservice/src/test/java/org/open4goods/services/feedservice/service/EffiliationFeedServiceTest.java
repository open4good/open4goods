package org.open4goods.services.feedservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.open4goods.commons.config.yml.datasource.CsvDataSourceProperties;
import org.open4goods.commons.config.yml.datasource.DataSourceProperties;
import org.open4goods.commons.services.DataSourceConfigService;
import org.open4goods.services.feedservice.config.FeedConfiguration;
import org.open4goods.services.remotefilecaching.service.RemoteFileCachingService;
import org.open4goods.services.serialisation.service.SerialisationService;

/**
 * Tests for {@link EffiliationFeedService} pagination, enablement and cache fallback behavior.
 */
class EffiliationFeedServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void retrieveEffiliationFeedsShouldThrowWhenDisabled() {
        EffiliationFeedService service = buildService(false, mock(RemoteFileCachingService.class));

        assertThatThrownBy(() -> service.retrieveEffiliationFeeds("api-key", "fr", null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("disabled");
    }

    @Test
    void retrieveEffiliationFeedsShouldMergePaginatedPayloads() throws Exception {
        RemoteFileCachingService cacheService = mock(RemoteFileCachingService.class);
        File page1 = jsonFile("""
                {"total_pages":2,"feeds":[{"nom":"Feed One","code":"https://one.test/feed.csv","id_affilieur":"11"}]}
                """);
        File page2 = jsonFile("""
                {"total_pages":2,"feeds":[{"nom":"Feed Two","code":"https://two.test/feed.csv","id_affilieur":"22"}]}
                """);

        when(cacheService.getResource(contains("page=1"), anyInt())).thenReturn(page1);
        when(cacheService.getResource(contains("page=2"), anyInt())).thenReturn(page2);

        EffiliationFeedService service = buildService(true, cacheService);

        var merged = service.retrieveEffiliationFeeds("api-key", "fr", null);

        assertThat(merged.path("feeds")).hasSize(2);
        assertThat(merged.path("feeds").get(0).path("nom").asText()).isEqualTo("Feed One");
        assertThat(merged.path("feeds").get(1).path("nom").asText()).isEqualTo("Feed Two");
    }

    @Test
    void getDatasourcesShouldUseStaleCacheWhenRefreshFails() throws Exception {
        RemoteFileCachingService cacheService = mock(RemoteFileCachingService.class);

        File programs = jsonFile("""
                {"programs":[{"id_affilieur":101,"url_tracke":"track.example","urllo":"logo.example","etat":"actif","pays":"fr"}]}
                """);
        File feeds = jsonFile("""
                {"feeds":[{"nom":"Merchant","code":"merchant.example/feed.csv","url_affilieur":"merchant.example","id_affilieur":"101"}]}
                """);

        when(cacheService.getResource(contains("programs.json"), anyInt())).thenReturn(programs);
        when(cacheService.getResource(contains("productfeeds.json"), eq(1))).thenThrow(new IOException("network down"));
        when(cacheService.getResource(contains("productfeeds.json"), eq(Integer.MAX_VALUE))).thenReturn(feeds);

        EffiliationFeedService service = buildService(true, cacheService);
        Set<DataSourceProperties> datasources = service.getDatasources();

        assertThat(datasources).hasSize(1);
        DataSourceProperties datasource = datasources.iterator().next();
        assertThat(datasource.getPortalUrl()).isEqualTo("https://merchant.example");
        assertThat(datasource.getAffiliatedPortalUrl()).isEqualTo("https://track.example");
    }

    @Test
    void getDatasourcesShouldSkipInvalidIdAndInactiveProgram() throws Exception {
        RemoteFileCachingService cacheService = mock(RemoteFileCachingService.class);

        File programs = jsonFile("""
                {"programs":[
                  {"id_affilieur":101,"url_tracke":"https://active.example/track","urllo":"https://active.example/logo.png","etat":"actif","pays":"fr"},
                  {"id_affilieur":303,"url_tracke":"https://inactive.example/track","urllo":"https://inactive.example/logo.png","etat":"suspended","pays":"fr"}
                ]}
                """);
        File feeds = jsonFile("""
                {"feeds":[
                  {"nom":"Valid Merchant","code":"https://active.example/feed.csv","url_affilieur":"https://active.example","id_affilieur":"101"},
                  {"nom":"Invalid Id Merchant","code":"https://invalid.example/feed.csv","url_affilieur":"https://invalid.example","id_affilieur":"invalid"},
                  {"nom":"Inactive Merchant","code":"https://inactive.example/feed.csv","url_affilieur":"https://inactive.example","id_affilieur":"303"}
                ]}
                """);

        when(cacheService.getResource(contains("programs.json"), anyInt())).thenReturn(programs);
        when(cacheService.getResource(contains("productfeeds.json"), anyInt())).thenReturn(feeds);

        EffiliationFeedService service = buildService(true, cacheService);

        Set<DataSourceProperties> datasources = service.getDatasources();

        assertThat(datasources).hasSize(1);
        assertThat(datasources.iterator().next().getDatasourceConfigName()).isEqualTo("Valid Merchant");
    }

    private EffiliationFeedService buildService(boolean enabled, RemoteFileCachingService cacheService) {
        FeedConfiguration config = new FeedConfiguration();
        FeedConfiguration.EffiliationConfig effiliation = new FeedConfiguration.EffiliationConfig();
        effiliation.setEnabled(enabled);
        effiliation.setCacheTtlDays(1);
        effiliation.setMaxJitterSeconds(0);
        config.setEffiliation(effiliation);

        CsvDataSourceProperties csvDataSourceProperties = new CsvDataSourceProperties();
        config.setDefaultCsvProperties(csvDataSourceProperties);

        DataSourceConfigService dataSourceConfigService = mock(DataSourceConfigService.class);
        when(dataSourceConfigService.getDatasourcePropertiesForFeed(org.mockito.ArgumentMatchers.anyString())).thenReturn(null);
        when(dataSourceConfigService.datasourceConfigs()).thenReturn(Map.of());

        return new EffiliationFeedService(
                config,
                cacheService,
                dataSourceConfigService,
                new SerialisationService(),
                "api-key");
    }

    private File jsonFile(String content) throws IOException {
        Path file = Files.createTempFile(tempDir, "effiliation", ".json");
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file.toFile();
    }
}
