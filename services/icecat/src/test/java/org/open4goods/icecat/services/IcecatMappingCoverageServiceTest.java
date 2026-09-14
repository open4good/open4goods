package org.open4goods.icecat.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.open4goods.datareference.model.registry.GitRegistryRuntimeImporter;
import org.open4goods.icecat.model.IcecatCategoryDocument;
import org.open4goods.icecat.model.IcecatMappingCoverage;

class IcecatMappingCoverageServiceTest {

    private static final LocalDate EFFECTIVE_ON = LocalDate.of(2026, 9, 12);

    @Test
    void reportsOnlyReviewedCategoryMappingsAndAllEditorialVerticals() {
        IcecatIndexService indexService = mock(IcecatIndexService.class);
        when(indexService.findAllCategories()).thenReturn(List.of(category(1584, "Televisions"),
                category(224, "Air conditioners"), category(999, "Unreviewed candidate")));

        IcecatMappingCoverageService service = new IcecatMappingCoverageService(indexService, projectionService());

        IcecatMappingCoverage coverage = service.coverage(EFFECTIVE_ON);

        assertThat(coverage.registryVersion()).isEqualTo(4);
        assertThat(coverage.registryHash()).isNotBlank();
        assertThat(coverage.categoryCount()).isEqualTo(3);
        assertThat(coverage.mappedCategoryCount()).isEqualTo(2);
        assertThat(coverage.unmappedCategoryCount()).isEqualTo(1);
        assertThat(coverage.mappedCategoriesByVertical()).containsOnlyKeys("air-conditioner", "dishwasher", "oven",
                "refrigerator", "smartphones", "tv", "washing-machine");
        assertThat(coverage.mappedCategoriesByVertical()).containsEntry("air-conditioner", 1L).containsEntry("tv", 1L);
    }

    @Test
    void exposesUnmappedCategoriesWithoutInferringAMappingFromTheirNames() {
        IcecatIndexService indexService = mock(IcecatIndexService.class);
        when(indexService.findAllCategories()).thenReturn(List.of(category(1584, "Televisions"),
                category(999, "TV-like but unreviewed"), category(998, "Candidate")));

        IcecatMappingCoverageService service = new IcecatMappingCoverageService(indexService, projectionService());

        assertThat(service.unmappedCategories(EFFECTIVE_ON, 1)).singleElement().satisfies(category -> {
            assertThat(category.id()).isEqualTo(998);
            assertThat(category.name()).isEqualTo("Candidate");
        });
    }

    private IcecatCategoryDocument category(int id, String name) {
        IcecatCategoryDocument category = new IcecatCategoryDocument();
        category.setId(id);
        category.setEnglishName(name);
        return category;
    }

    private IcecatRegistryProjectionService projectionService() {
        return new IcecatRegistryProjectionService(new GitRegistryRuntimeImporter());
    }
}
