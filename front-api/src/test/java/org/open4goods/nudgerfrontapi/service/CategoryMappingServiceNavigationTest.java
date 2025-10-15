package org.open4goods.nudgerfrontapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.open4goods.model.vertical.ProductCategory;
import org.open4goods.model.vertical.ProductI18nElements;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.nudgerfrontapi.config.properties.ApiProperties;
import org.open4goods.nudgerfrontapi.dto.category.CategoryNavigationDto;
import org.open4goods.nudgerfrontapi.dto.category.GoogleCategoryDto;
import org.open4goods.nudgerfrontapi.localization.DomainLanguage;
import org.open4goods.verticals.GoogleTaxonomyService;

class CategoryMappingServiceNavigationTest {

    private CategoryMappingService service;

    @BeforeEach
    void setUp() {
        ApiProperties apiProperties = new ApiProperties();
        service = new CategoryMappingService(apiProperties, mock(GoogleTaxonomyService.class));
    }

    @Test
    void toCategoryNavigationDtoBuildsDeepNavigationStructure() {
        ProductCategory root = new ProductCategory(0, "Root", "default");

        ProductCategory electronics = new ProductCategory(1, "Électronique", "fr");
        electronics.setParent(root);
        root.addChild(electronics);

        ProductCategory tv = new ProductCategory(2, "Téléviseurs", "fr");
        tv.setParent(electronics);
        electronics.addChild(tv);

        ProductCategory audio = new ProductCategory(3, "Audio", "fr");
        audio.setParent(electronics);
        electronics.addChild(audio);

        ProductCategory headphones = new ProductCategory(4, "Casques", "fr");
        headphones.setParent(audio);
        audio.addChild(headphones);

        tv.vertical(createVerticalConfig("tv", 2));
        headphones.vertical(createVerticalConfig("headphones", 4));

        CategoryNavigationDto navigation = service.toCategoryNavigationDto(electronics, DomainLanguage.fr, true);

        assertThat(navigation).isNotNull();
        assertThat(navigation.category().googleCategoryId()).isEqualTo(1);
        assertThat(navigation.category().path()).isEqualTo("electronique");

        assertThat(navigation.breadcrumbs()).hasSize(2);
        assertThat(navigation.breadcrumbs().get(0).title()).isNull();
        assertThat(navigation.breadcrumbs().get(0).link()).isEmpty();
        assertThat(navigation.breadcrumbs().get(1).title()).isEqualTo("Électronique");
        assertThat(navigation.breadcrumbs().get(1).link()).isEqualTo("electronique");

        List<GoogleCategoryDto> children = navigation.childCategories();
        assertThat(children).hasSize(2);
        assertThat(children.get(0).googleCategoryId()).isEqualTo(2);
        assertThat(children.get(0).vertical()).isNotNull();
        assertThat(children.get(1).children()).hasSize(1);
        assertThat(children.get(1).children().get(0).googleCategoryId()).isEqualTo(4);

        assertThat(navigation.descendantVerticals())
                .hasSize(1)
                .first()
                .extracting(GoogleCategoryDto::googleCategoryId)
                .isEqualTo(4);
    }

    private VerticalConfig createVerticalConfig(String id, int googleTaxonomyId) {
        VerticalConfig config = new VerticalConfig();
        config.setId(id);
        config.setEnabled(true);
        config.setGoogleTaxonomyId(googleTaxonomyId);
        config.setIcecatTaxonomyId(googleTaxonomyId * 10);
        config.setOrder(1);

        ProductI18nElements elements = new ProductI18nElements();
        elements.setVerticalHomeTitle(id + " title");
        elements.setVerticalHomeDescription(id + " description");
        elements.setVerticalHomeUrl(id + "-url");

        Map<String, ProductI18nElements> i18n = new HashMap<>();
        i18n.put("fr", elements);
        config.setI18n(i18n);

        return config;
    }
}

