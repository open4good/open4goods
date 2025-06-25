package org.open4goods.nudgerfrontapi.service;

import java.util.List;
import java.util.Map;

import org.open4goods.model.constants.CacheConstants;
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


    public record CategoryDto(Integer id, Map<String, String> names, String vertical, List<CategoryDto> children) {
    }
}
