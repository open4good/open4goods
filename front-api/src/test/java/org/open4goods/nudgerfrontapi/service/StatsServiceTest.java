package org.open4goods.nudgerfrontapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.open4goods.nudgerfrontapi.dto.stats.CategoriesStatsDto;
import org.open4goods.nudgerfrontapi.localization.DomainLanguage;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;

class StatsServiceTest {

    private static final String DEFAULT_YAML = "id: default\nenabled: false\n";
    private static final String ENABLED_YAML = "id: enabled\nenabled: true\n";
    private static final String DISABLED_YAML = "id: disabled\nenabled: false\n";

    @Test
    void categoriesCountsEnabledVerticalConfigs() throws Exception {
        SerialisationService serialisationService = new SerialisationService();
        ResourcePatternResolver resolver = mock(ResourcePatternResolver.class);
        AffiliationPartnerService partnerSrvice = mock(AffiliationPartnerService.class);

        Resource defaultResource = resource("_default.yml", DEFAULT_YAML);
        Resource enabledResource = resource("enabled.yml", ENABLED_YAML);
        Resource disabledResource = resource("disabled.yml", DISABLED_YAML);

        given(resolver.getResource("classpath:/verticals/_default.yml")).willReturn(defaultResource);
        given(resolver.getResources("classpath:/verticals/*.yml"))
                .willReturn(new Resource[]{defaultResource, enabledResource, disabledResource});

        StatsService service = new StatsService(serialisationService, resolver, partnerSrvice);

        CategoriesStatsDto dto = service.categories(DomainLanguage.fr);

        assertThat(dto.enabledVerticalConfigs()).isEqualTo(1);
    }

    private Resource resource(String filename, String yaml) {
        return new ByteArrayResource(yaml.getBytes()) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
    }
}
