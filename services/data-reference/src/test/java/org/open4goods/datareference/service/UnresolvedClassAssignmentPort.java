package org.open4goods.datareference.service;

import java.util.Optional;

import org.open4goods.datareference.model.CanonicalClassId;
import org.open4goods.datareference.model.Gtin;
import org.open4goods.datareference.model.ProjectionSurface;
import org.open4goods.datareference.port.ClassAssignmentPort;

/**
 * Test-only {@link ClassAssignmentPort} double reserved for fixtures that do
 * not care about class resolution.
 *
 * <p>Always reports no confirmed class, which correctly suppresses automatic
 * model grouping (ADR-0010: "missing brand/class/model prevents automatic
 * exact grouping") rather than guessing one. {@link RegistryClassAssignmentPort}
 * is the production resolver; this type lives under {@code src/test} so it
 * cannot be wired into a production path (GOU-204).
 */
public final class UnresolvedClassAssignmentPort implements ClassAssignmentPort {

    @Override
    public Optional<CanonicalClassId> resolveClass(Gtin gtin, ProjectionSurface surface) {
        return Optional.empty();
    }
}
