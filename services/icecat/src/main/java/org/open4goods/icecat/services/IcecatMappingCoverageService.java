package org.open4goods.icecat.services;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.StreamSupport;

import org.open4goods.datareference.model.CanonicalClassId;
import org.open4goods.datareference.model.registry.RegistryRuntimeIndex;
import org.open4goods.icecat.model.IcecatCategoryDocument;
import org.open4goods.icecat.model.IcecatMappingCoverage;
import org.open4goods.icecat.model.IcecatUnmappedCategory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Reports coverage of the indexed Icecat catalogue by the Git-authored O4G registry.
 *
 * <p>This service never writes mappings or Icecat data. Its only mapping source is
 * the packaged registry; fuzzy category names remain unmapped review candidates.
 */
@Service
public class IcecatMappingCoverageService {

    private final IcecatIndexService icecatIndexService;
    private final IcecatRegistryProjectionService projectionService;

    @Autowired
    public IcecatMappingCoverageService(
            IcecatIndexService icecatIndexService, IcecatRegistryProjectionService projectionService) {
        this.icecatIndexService = Objects.requireNonNull(icecatIndexService, "icecatIndexService must not be null");
        this.projectionService = Objects.requireNonNull(projectionService, "projectionService must not be null");
    }


    /** Returns category and editorial-vertical coverage at the requested registry date. */
    public IcecatMappingCoverage coverage(LocalDate effectiveOn) {
        RegistryRuntimeIndex index = loadRegistry();
        List<IcecatCategoryDocument> categories = categories();
        Predicate<IcecatCategoryDocument> mapped = category -> mappedClass(index, category, effectiveOn) != null;
        long mappedCount = categories.stream().filter(mapped).count();
        Map<String, Long> byVertical = index.registry().verticalViews().stream().collect(java.util.stream.Collectors.toMap(
                view -> view.verticalId(), view -> categories.stream()
                        .map(category -> mappedClass(index, category, effectiveOn))
                        .filter(Objects::nonNull)
                        .filter(view.includedClasses()::contains)
                        .count(), (left, right) -> left, LinkedHashMap::new));
        return new IcecatMappingCoverage(index.registry().version().value(), index.contentHash(), categories.size(),
                mappedCount, categories.size() - mappedCount,
                Collections.unmodifiableMap(new LinkedHashMap<>(byVertical)));
    }

    /** Returns a bounded, deterministic page of honestly unmapped category candidates. */
    public List<IcecatUnmappedCategory> unmappedCategories(LocalDate effectiveOn, int limit) {
        RegistryRuntimeIndex index = loadRegistry();
        return categories().stream()
                .filter(category -> mappedClass(index, category, effectiveOn) == null)
                .sorted(Comparator.comparing(IcecatCategoryDocument::getId,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(limit)
                .map(category -> new IcecatUnmappedCategory(category.getId(), category.getEnglishName(),
                        category.getParentId(), category.getScore()))
                .toList();
    }

    private RegistryRuntimeIndex loadRegistry() {
        return projectionService.current();
    }

    private List<IcecatCategoryDocument> categories() {
        return StreamSupport.stream(icecatIndexService.findAllCategories().spliterator(), false).toList();
    }

    private CanonicalClassId mappedClass(RegistryRuntimeIndex index, IcecatCategoryDocument category, LocalDate effectiveOn) {
        if (category.getId() == null) {
            return null;
        }
        return index.registry().findReviewedMapping("icecat", "category:" + category.getId(), effectiveOn)
                .map(mapping -> mapping.conceptId() instanceof CanonicalClassId classId ? classId : null)
                .orElse(null);
    }
}
