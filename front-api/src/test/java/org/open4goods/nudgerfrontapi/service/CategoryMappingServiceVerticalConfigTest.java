package org.open4goods.nudgerfrontapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.open4goods.model.vertical.AttributeConfig;
import org.open4goods.model.vertical.AttributesConfig;
import org.open4goods.model.vertical.ProductCategory;
import org.open4goods.model.vertical.ProductI18nElements;
import org.open4goods.model.vertical.SubsetCriteria;
import org.open4goods.model.vertical.SubsetCriteriaOperator;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.model.vertical.VerticalSubCategory;
import org.open4goods.model.vertical.VerticalSubCategoryHeroBlock;
import org.open4goods.nudgerfrontapi.config.properties.ApiProperties;
import org.open4goods.nudgerfrontapi.dto.category.CategoryBreadcrumbItemDto;
import org.open4goods.nudgerfrontapi.dto.category.VerticalConfigDto;
import org.open4goods.nudgerfrontapi.dto.category.VerticalConfigFullDto;
import org.open4goods.nudgerfrontapi.localization.DomainLanguage;
import org.open4goods.verticals.GoogleTaxonomyService;

class CategoryMappingServiceVerticalConfigTest {

    private GoogleTaxonomyService googleTaxonomyService;
    private CategoryMappingService service;

    @BeforeEach
    void setUp() {
        ApiProperties apiProperties = new ApiProperties();
        googleTaxonomyService = mock(GoogleTaxonomyService.class);
        service = new CategoryMappingService(apiProperties, googleTaxonomyService);
    }

    @Test
    void toVerticalConfigFullDtoUsesVerticalLandingPageForTerminalBreadcrumb() {
        VerticalConfig verticalConfig = createVerticalConfig("televisions", 2);
        ProductCategory category = buildCategoryHierarchy(verticalConfig);
        when(googleTaxonomyService.byId(2)).thenReturn(category);

        VerticalConfigFullDto dto = service.toVerticalConfigFullDto(verticalConfig, DomainLanguage.fr, List.of());

        assertThat(dto).isNotNull();
        assertThat(dto.breadCrumb()).isNotEmpty();

        CategoryBreadcrumbItemDto terminal = dto.breadCrumb().get(dto.breadCrumb().size() - 1);
        assertThat(terminal.title()).isEqualTo("Téléviseurs");
        assertThat(terminal.link()).isEqualTo("/televisions-url");
    }

    @Test
    void toVerticalConfigDtoIgnoresMissingVerticalImages() {
        ApiProperties properties = new ApiProperties();
        properties.setResourceRootPath("https://static.example");

        CategoryMappingService serviceWithResources = new CategoryMappingService(properties, googleTaxonomyService);
        VerticalConfig config = createVerticalConfig("washing-machines", 3);
        config.setVerticalImage("   ");

        VerticalConfigDto dto = serviceWithResources.toVerticalConfigDto(config, DomainLanguage.fr);

        assertThat(dto).isNotNull();
        assertThat(dto.imageSmall()).isNull();
        assertThat(dto.imageMedium()).isNull();
        assertThat(dto.imageLarge()).isNull();
    }

    @Test
    void toVerticalConfigDtoExposesAggregatedScores() {
        VerticalConfig config = createVerticalConfig("washing-machines", 3);

        AttributeConfig efficiency = new AttributeConfig();
        efficiency.setKey("EFFICIENCY");
        efficiency.setAsScore(true);
        efficiency.setParticipateInScores(Set.of("GLOBAL", "ECO"));

        AttributeConfig noise = new AttributeConfig();
        noise.setKey("NOISE");
        noise.setAsScore(true);
        noise.setParticipateInScores(Set.of("GLOBAL"));

        config.setAttributesConfig(new AttributesConfig(List.of(efficiency, noise)));

        VerticalConfigDto dto = service.toVerticalConfigDto(config, DomainLanguage.fr);

        assertThat(dto.aggregatedScores()).containsExactly("ECO", "GLOBAL");
    }

    @Test
    void toVerticalConfigFullDtoExposesLocalizedSubCategories() {
        VerticalConfig config = createVerticalConfig("dishwasher", 3);

        VerticalSubCategory subCategory = new VerticalSubCategory();
        subCategory.setId("under-sink");
        subCategory.getSlug().put("fr", "lave-vaisselle-sous-lavabo");
        subCategory.getSlug().put("en", "under-sink-dishwasher");
        subCategory.getH1Title().put("fr", "Lave-vaisselle sous lavabo");
        subCategory.getDescription().put("fr", "Comparez les **lave-vaisselles** sous lavabo.");
        VerticalSubCategoryHeroBlock heroBlock = new VerticalSubCategoryHeroBlock();
        heroBlock.getTitle().put("fr", "Le saviez-vous :");
        heroBlock.getBody().put("fr", "Les formats compacts gardent une **installation flexible**.");
        subCategory.setHeroBlock(heroBlock);
        subCategory.setImage("/images/verticals/dishwasher-under-sink.webp");
        subCategory.setActivatedFilters(List.of(
                new SubsetCriteria("attributes.indexed.INSTALLATION_TYPE.value",
                        SubsetCriteriaOperator.EQUALS,
                        "ENCASTRABLE")));
        config.setSubCategories(List.of(subCategory));

        VerticalConfigFullDto dto = service.toVerticalConfigFullDto(config, DomainLanguage.fr, List.of());

        assertThat(dto.subCategories()).hasSize(1);
        assertThat(dto.subCategories().get(0).slug()).isEqualTo("lave-vaisselle-sous-lavabo");
        assertThat(dto.subCategories().get(0).h1Title()).isEqualTo("Lave-vaisselle sous lavabo");
        assertThat(dto.subCategories().get(0).description()).isEqualTo("Comparez les **lave-vaisselles** sous lavabo.");
        assertThat(dto.subCategories().get(0).heroBlock()).isNotNull();
        assertThat(dto.subCategories().get(0).heroBlock().title()).isEqualTo("Le saviez-vous :");
        assertThat(dto.subCategories().get(0).heroBlock().body())
                .isEqualTo("Les formats compacts gardent une **installation flexible**.");
        assertThat(dto.subCategories().get(0).image()).isEqualTo("/images/verticals/dishwasher-under-sink.webp");
        assertThat(dto.subCategories().get(0).activatedFilters()).hasSize(1);
    }

    private VerticalConfig createVerticalConfig(String id, int googleTaxonomyId) {
        VerticalConfig config = new VerticalConfig();
        config.setId(id);
        config.setEnabled(true);
        config.setGoogleTaxonomyId(googleTaxonomyId);
        config.setOrder(1);

        ProductI18nElements elements = new ProductI18nElements();
        elements.setVerticalHomeTitle("Téléviseurs");
        elements.setVerticalHomeUrl("televisions-url");

        Map<String, ProductI18nElements> i18n = new HashMap<>();
        i18n.put("fr", elements);
        config.setI18n(i18n);

        return config;
    }

    private ProductCategory buildCategoryHierarchy(VerticalConfig terminalVertical) {
        ProductCategory root = new ProductCategory(0, "Appareils", "fr");

        ProductCategory electronics = new ProductCategory(1, "Appareils électroniques", "fr");
        electronics.setParent(root);
        root.addChild(electronics);

        ProductCategory televisions = new ProductCategory(2, "Téléviseurs", "fr");
        televisions.setParent(electronics);
        electronics.addChild(televisions);

        televisions.vertical(terminalVertical);

        return televisions;
    }
}
