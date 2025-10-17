package org.open4goods.nudgerfrontapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.open4goods.model.vertical.ProductCategory;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.nudgerfrontapi.config.properties.ApiProperties;
import org.open4goods.nudgerfrontapi.dto.category.CategoryBreadcrumbItemDto;
import org.open4goods.nudgerfrontapi.dto.category.VerticalConfigFullDto;
import org.open4goods.nudgerfrontapi.localization.DomainLanguage;
import org.open4goods.verticals.GoogleTaxonomyService;

class CategoryMappingServiceFullDtoTest {

    private CategoryMappingService service;
    private GoogleTaxonomyService googleTaxonomyService;

    @BeforeEach
    void setUp() {
        ApiProperties apiProperties = new ApiProperties();
        googleTaxonomyService = mock(GoogleTaxonomyService.class);
        service = new CategoryMappingService(apiProperties, googleTaxonomyService);
    }

    @Test
    void toVerticalConfigFullDtoBuildsFullBreadcrumbPaths() {
        ProductCategory electronics = new ProductCategory(1, "Appareils électroniques", "fr");

        ProductCategory video = new ProductCategory(2, "Vidéo", "fr");
        video.setParent(electronics);
        electronics.addChild(video);

        ProductCategory televisions = new ProductCategory(3, "Téléviseurs", "fr");
        televisions.setParent(video);
        video.addChild(televisions);

        when(googleTaxonomyService.byId(3)).thenReturn(televisions);

        VerticalConfig config = new VerticalConfig();
        config.setId("tv");
        config.setEnabled(true);
        config.setGoogleTaxonomyId(3);

        VerticalConfigFullDto dto = service.toVerticalConfigFullDto(config, DomainLanguage.fr, List.of());

        assertThat(dto.breadCrumb())
                .extracting(CategoryBreadcrumbItemDto::title)
                .containsExactly("Appareils électroniques", "Vidéo", "Téléviseurs");
        assertThat(dto.breadCrumb())
                .extracting(CategoryBreadcrumbItemDto::link)
                .containsExactly(
                        "/categories/appareils-electroniques",
                        "/categories/appareils-electroniques/video",
                        "/categories/appareils-electroniques/video/televiseurs");
    }
}
