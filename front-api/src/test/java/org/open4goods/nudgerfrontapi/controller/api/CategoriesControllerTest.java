package org.open4goods.nudgerfrontapi.controller.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.open4goods.model.localization.DomainLanguage;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.nudgerfrontapi.dto.category.VerticalConfigDto;
import org.open4goods.nudgerfrontapi.service.CategoryMappingService;
import org.open4goods.verticals.VerticalsConfigService;
import org.springframework.http.ResponseEntity;

/** GOU-2621: {@code onlyEnabled} must exclude disabled verticals. */
class CategoriesControllerTest {

    @Test
    void categories_shouldHonourOnlyEnabled() {
        VerticalsConfigService verticals = mock(VerticalsConfigService.class);
        CategoryMappingService mapping = mock(CategoryMappingService.class);
        VerticalConfig enabled = new VerticalConfig();
        enabled.setId("tv");
        enabled.setEnabled(true);
        VerticalConfig disabled = new VerticalConfig();
        disabled.setId("frz");
        when(verticals.getConfigsWithoutDefault(true)).thenReturn(List.of(enabled));
        when(verticals.getConfigsWithoutDefault(false)).thenReturn(List.of(enabled, disabled));
        when(mapping.toVerticalConfigDto(any(), eq(DomainLanguage.fr))).thenReturn(mock(VerticalConfigDto.class));

        CategoriesController controller = new CategoriesController(verticals, mapping, null, null, null, null);

        ResponseEntity<List<VerticalConfigDto>> onlyEnabled = controller.categories(DomainLanguage.fr, true);
        ResponseEntity<List<VerticalConfigDto>> all = controller.categories(DomainLanguage.fr, false);

        assertThat(onlyEnabled.getBody()).hasSize(1);
        assertThat(all.getBody()).hasSize(2);
    }
}
