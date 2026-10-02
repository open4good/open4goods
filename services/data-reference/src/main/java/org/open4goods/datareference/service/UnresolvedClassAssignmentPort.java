package org.open4goods.datareference.service;

import java.util.Optional;

import org.open4goods.datareference.model.CanonicalClassId;
import org.open4goods.datareference.model.Gtin;
import org.open4goods.datareference.model.ProjectionSurface;
import org.open4goods.datareference.port.ClassAssignmentPort;

/**
 * Placeholder {@link ClassAssignmentPort} for use before a dedicated
 * class-resolution capability exists.
 *
 * <p>Always reports no confirmed class, which correctly suppresses automatic
 * model grouping (ADR-0010: "missing brand/class/model prevents automatic
 * exact grouping") rather than guessing one. Replace with a real resolver once
 * GTIN-to-class resolution is implemented.
 */
public final class UnresolvedClassAssignmentPort implements ClassAssignmentPort {

    @Override
    public Optional<CanonicalClassId> resolveClass(Gtin gtin, ProjectionSurface surface) {
        return Optional.empty();
    }
}
