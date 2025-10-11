package org.open4goods.nudgerfrontapi.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.open4goods.model.vertical.ProductCategory;
import org.open4goods.nudgerfrontapi.dto.category.GoogleCategoryDto;
import org.open4goods.nudgerfrontapi.dto.category.GoogleCategorySummaryDto;
import org.open4goods.nudgerfrontapi.localization.DomainLanguage;
import org.open4goods.verticals.GoogleTaxonomyService;
import org.springframework.stereotype.Service;

/**
 * Service exposing navigation helpers over the Google taxonomy tree for the frontend API.
 */
@Service
public class GoogleCategoryNavigationService {

    private final GoogleTaxonomyService googleTaxonomyService;
    private final CategoryMappingService categoryMappingService;

    public GoogleCategoryNavigationService(GoogleTaxonomyService googleTaxonomyService,
                                           CategoryMappingService categoryMappingService) {
        this.googleTaxonomyService = googleTaxonomyService;
        this.categoryMappingService = categoryMappingService;
    }

    /**
     * Resolve a taxonomy node by its Google taxonomy identifier and convert it to a DTO.
     *
     * @param taxonomyId     identifier of the taxonomy node
     * @param domainLanguage requested localisation context
     * @return optional DTO representing the taxonomy node
     */
    public Optional<GoogleCategoryDto> getCategoryById(Integer taxonomyId, DomainLanguage domainLanguage) {
        return resolveById(taxonomyId)
                .map(category -> categoryMappingService.toGoogleCategoryDto(category, domainLanguage));
    }

    /**
     * Resolve a taxonomy node by its slug path and convert it to a DTO.
     *
     * @param path           slug path relative to the taxonomy root
     * @param domainLanguage requested localisation context
     * @return optional DTO representing the taxonomy node
     */
    public Optional<GoogleCategoryDto> getCategoryByPath(String path, DomainLanguage domainLanguage) {
        return resolveByPath(path, domainLanguage)
                .map(category -> categoryMappingService.toGoogleCategoryDto(category, domainLanguage));
    }

    /**
     * Retrieve the immediate children of a taxonomy node identified by its taxonomy id.
     *
     * @param taxonomyId     identifier of the parent taxonomy node
     * @param domainLanguage requested localisation context
     * @return optional list of child summaries
     */
    public Optional<List<GoogleCategorySummaryDto>> getChildrenById(Integer taxonomyId,
                                                                    DomainLanguage domainLanguage) {
        return resolveById(taxonomyId)
                .map(category -> categoryMappingService.toGoogleCategoryChildren(category, domainLanguage));
    }

    /**
     * Retrieve the immediate children of a taxonomy node identified by its slug path.
     *
     * @param path           slug path relative to the taxonomy root
     * @param domainLanguage requested localisation context
     * @return optional list of child summaries
     */
    public Optional<List<GoogleCategorySummaryDto>> getChildrenByPath(String path,
                                                                      DomainLanguage domainLanguage) {
        return resolveByPath(path, domainLanguage)
                .map(category -> categoryMappingService.toGoogleCategoryChildren(category, domainLanguage));
    }

    private Optional<ProductCategory> resolveById(Integer taxonomyId) {
        if (taxonomyId == null || taxonomyId == 0) {
            return Optional.of(googleTaxonomyService.getCategories().asRootNode());
        }
        return Optional.ofNullable(googleTaxonomyService.byId(taxonomyId));
    }

    private Optional<ProductCategory> resolveByPath(String path, DomainLanguage domainLanguage) {
        String normalized = normalizePath(path);
        if (normalized.isEmpty()) {
            return Optional.of(googleTaxonomyService.getCategories().asRootNode());
        }

        for (String languageKey : candidateLanguageKeys(domainLanguage)) {
            Map<String, ProductCategory> nodes = googleTaxonomyService.getCategories().paths(languageKey);
            if (nodes == null || nodes.isEmpty()) {
                continue;
            }
            ProductCategory category = nodes.get(normalized);
            if (category != null) {
                return Optional.of(category);
            }
        }
        return Optional.empty();
    }

    private String normalizePath(String path) {
        if (path == null) {
            return "";
        }
        String normalized = path.trim();
        if (normalized.isEmpty()) {
            return "";
        }
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private Set<String> candidateLanguageKeys(DomainLanguage domainLanguage) {
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        String iso = domainLanguage.name();
        String tag = domainLanguage.languageTag();
        keys.add(iso);
        keys.add(iso.toLowerCase(Locale.ROOT));
        keys.add(tag);
        keys.add(tag.replace('_', '-'));
        keys.add(tag.replace('-', '_'));
        int separator = tag.indexOf('-');
        if (separator > 0) {
            keys.add(tag.substring(0, separator));
        }
        return keys;
    }
}
