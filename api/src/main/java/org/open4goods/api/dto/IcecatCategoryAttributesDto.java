package org.open4goods.api.dto;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Attributes available for a given Icecat category, assembled from Elasticsearch metadata.
 */
public record IcecatCategoryAttributesDto(
        Integer categoryId,
        String categoryEnglishName,
        List<IcecatCategoryFeatureGroupDto> featureGroups,
        List<IcecatCategoryAttributeDto> attributes
) {

    /**
     * Feature-group relation attached to the category.
     */
    public record IcecatCategoryFeatureGroupDto(
            Integer id,
            List<Integer> featureGroupIds
    ) {}

    /**
     * Feature metadata in both category context and global Icecat feature context.
     */
    public record IcecatCategoryAttributeDto(
            Integer id,
            String englishName,
            String globalType,
            String categoryType,
            Integer categoryFeatureGroupId,
            Integer categoryFeatureId,
            String no,
            String clazz,
            String defaultDisplayUnit,
            Integer limitDirection,
            Integer mandatory,
            Integer searchable,
            String useDropdownInput,
            Integer valueSorting,
            Set<String> normalizedNames,
            List<String> langNames,
            Map<String, String> localizedNames
    ) {}
}
