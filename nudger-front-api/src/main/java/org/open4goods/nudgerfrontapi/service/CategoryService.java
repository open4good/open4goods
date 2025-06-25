package org.open4goods.nudgerfrontapi.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.open4goods.commons.model.ProductCategory;
import org.open4goods.commons.services.GoogleTaxonomyService;
import org.open4goods.commons.services.VerticalsConfigService;
import org.open4goods.model.constants.CacheConstants;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class CategoryService {

    private final GoogleTaxonomyService taxonomyService;
    private final VerticalsConfigService verticalsService;

    public CategoryService(GoogleTaxonomyService taxonomyService, VerticalsConfigService verticalsService) {
        this.taxonomyService = taxonomyService;
        this.verticalsService = verticalsService;
    }

    @Cacheable(cacheNames = CacheConstants.ONE_HOUR_LOCAL_CACHE_NAME, keyGenerator = CacheConstants.KEY_GENERATOR)
    public List<CategoryDto> listRootCategories(boolean includeChildren) {
        return taxonomyService.getCategories().getNodes().stream()
                .map(c -> toDto(c, includeChildren))
                .toList();
    }

    @Cacheable(cacheNames = CacheConstants.ONE_HOUR_LOCAL_CACHE_NAME, keyGenerator = CacheConstants.KEY_GENERATOR)
    public CategoryDto getCategory(int id, boolean includeChildren) {
        ProductCategory pc = taxonomyService.byId(id);
        if (pc == null) {
            return null;
        }
        return toDto(pc, includeChildren);
    }

    private CategoryDto toDto(ProductCategory pc, boolean includeChildren) {
        List<CategoryDto> children = includeChildren
                ? pc.getChildren().stream().map(c -> toDto(c, true)).toList()
                : List.of();
        String vertical = null;
        if (verticalsService.getVerticalForTaxonomy(pc.getGoogleCategoryId()) != null) {
            vertical = verticalsService.getVerticalForTaxonomy(pc.getGoogleCategoryId()).getId();
        }
        return new CategoryDto(pc.getGoogleCategoryId(), pc.getGoogleNames(), vertical, children);
    }

    public record CategoryDto(Integer id, Map<String, String> names, String vertical, List<CategoryDto> children) {
    }
}
