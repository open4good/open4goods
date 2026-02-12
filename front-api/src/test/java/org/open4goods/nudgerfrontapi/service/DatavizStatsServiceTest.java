package org.open4goods.nudgerfrontapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.nudgerfrontapi.dto.stats.VerticalDatavizPlanDto;
import org.open4goods.nudgerfrontapi.localization.DomainLanguage;
import org.open4goods.verticals.VerticalsConfigService;

/**
 * Unit tests for {@link DatavizStatsService}.
 */
class DatavizStatsServiceTest {

    /**
     * Verify the service returns a plan containing defaults and the full chart catalog.
     */
    @Test
    void getVerticalPlanReturnsCatalogAndDefaultFilters() {
        VerticalsConfigService verticalsConfigService = mock(VerticalsConfigService.class);
        SearchService searchService = mock(SearchService.class);
        VerticalConfig verticalConfig = new VerticalConfig();
        verticalConfig.setId("televisions");
        given(verticalsConfigService.getConfigById("televisions")).willReturn(verticalConfig);

        DatavizStatsService service = new DatavizStatsService(verticalsConfigService, searchService);

        VerticalDatavizPlanDto result = service.getVerticalPlan("televisions", DomainLanguage.fr);

        assertThat(result).isNotNull();
        assertThat(result.verticalId()).isEqualTo("televisions");
        assertThat(result.defaultFilters()).hasSize(2);
        assertThat(result.defaultFilters().get(0).field()).isEqualTo("lastChange");
        assertThat(result.defaultFilters().get(0).minRelative()).isEqualTo("now-2d");
        assertThat(result.defaultFilters().get(1).field()).isEqualTo("offersCount");
        assertThat(result.defaultFilters().get(1).min()).isEqualTo(1d);
        assertThat(result.charts()).hasSize(24);
        assertThat(result.charts().get(0).title()).isEqualTo("Produits actifs");
    }

    /**
     * Ensure unknown vertical identifiers return {@code null}.
     */
    @Test
    void getVerticalPlanReturnsNullWhenVerticalIsUnknown() {
        VerticalsConfigService verticalsConfigService = mock(VerticalsConfigService.class);
        SearchService searchService = mock(SearchService.class);
        given(verticalsConfigService.getConfigById("unknown")).willReturn(null);

        DatavizStatsService service = new DatavizStatsService(verticalsConfigService, searchService);

        assertThat(service.getVerticalPlan("unknown", DomainLanguage.en)).isNull();
    }
}
