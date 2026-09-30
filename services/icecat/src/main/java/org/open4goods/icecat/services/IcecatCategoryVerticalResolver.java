package org.open4goods.icecat.services;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

import org.open4goods.datareference.model.CanonicalClassId;
import org.open4goods.datareference.model.registry.RegistryRuntimeIndex;
import org.open4goods.datareference.model.registry.RegistryVerticalView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Resolves an editorial vertical id for an Icecat category, using only reviewed
 * ({@code REVIEWED}, i.e. approved) mappings from the Git-authored O4G registry.
 *
 * <p>This is the single bridge between the Icecat-to-O4G mapping registry and real
 * product normalization. An Icecat category with no reviewed mapping, or whose
 * mapped canonical class is not included in any editorial vertical view, resolves
 * to {@link Optional#empty()} : callers must not fall back to a heuristic guess,
 * since only an approved mapping may assign a vertical to a product.
 */
@Service
public class IcecatCategoryVerticalResolver {

    private final IcecatRegistryProjectionService projectionService;

    @Autowired
    public IcecatCategoryVerticalResolver(IcecatRegistryProjectionService projectionService) {
        this.projectionService = Objects.requireNonNull(projectionService, "projectionService must not be null");
    }

    /**
     * Resolves the editorial vertical id for the given Icecat category, if a reviewed
     * mapping is effective on the given date and the mapped class belongs to a
     * registered vertical view.
     *
     * @param icecatCategoryId numeric Icecat category id
     * @param effectiveOn      date the mapping must be effective on
     * @return the vertical id, or empty when no approved mapping resolves it
     */
    public Optional<String> resolveVerticalId(Integer icecatCategoryId, LocalDate effectiveOn) {
        if (icecatCategoryId == null) {
            return Optional.empty();
        }
        Objects.requireNonNull(effectiveOn, "effectiveOn must not be null");

        RegistryRuntimeIndex index = projectionService.current();
        return index.registry().findReviewedMapping("icecat", "category:" + icecatCategoryId, effectiveOn)
                .map(mapping -> mapping.conceptId() instanceof CanonicalClassId classId ? classId : null)
                .flatMap(classId -> verticalIdFor(index, classId));
    }

    private Optional<String> verticalIdFor(RegistryRuntimeIndex index, CanonicalClassId classId) {
        if (classId == null) {
            return Optional.empty();
        }
        return index.registry().verticalViews().stream()
                .filter(view -> view.includedClasses().contains(classId))
                .map(RegistryVerticalView::verticalId)
                .findFirst();
    }
}
