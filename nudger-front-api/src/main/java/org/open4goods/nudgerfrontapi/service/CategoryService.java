package org.open4goods.nudgerfrontapi.service;

import java.util.List;
import java.util.Map;

import org.open4goods.model.constants.CacheConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class CategoryService {



    public CategoryService() {

    }

    @Cacheable(cacheNames = CacheConstants.ONE_HOUR_LOCAL_CACHE_NAME, keyGenerator = CacheConstants.KEY_GENERATOR)
    public List<CategoryDto> listRootCategories(boolean includeChildren) {
        return null;
    }

    @Cacheable(cacheNames = CacheConstants.ONE_HOUR_LOCAL_CACHE_NAME, keyGenerator = CacheConstants.KEY_GENERATOR)
    public CategoryDto getCategory(int id, boolean includeChildren) {

    	 return null;
    }


    /**
     * Lightweight category representation used by API responses.
     */
    public record CategoryDto(
            @Schema(description = "Category identifier", example = "5")
            Integer id,

            @Schema(description = "Localized names", example = "{\"en\":\"Beverages\"}")
            Map<String, String> names,

            @Schema(description = "Associated vertical", example = "food")
            String vertical,

            @Schema(description = "Child categories", nullable = true)
            List<CategoryDto> children) {
    }
}
