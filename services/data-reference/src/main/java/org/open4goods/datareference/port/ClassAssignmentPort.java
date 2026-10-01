package org.open4goods.datareference.port;

import java.util.Optional;

import org.open4goods.datareference.model.CanonicalClassId;
import org.open4goods.datareference.model.Gtin;
import org.open4goods.datareference.model.ProjectionSurface;

/**
 * Resolves the O4G class a GTIN leaf belongs to.
 *
 * <p>Class resolution is a model-level fact, not a canonical-attribute value,
 * so it is not part of {@link ResolutionPort}. Model grouping depends on it
 * per ADR-0010 ("missing brand/class/model prevents automatic exact
 * grouping"); until a dedicated class-resolution capability exists, an
 * implementation may always return {@link Optional#empty()}, which correctly
 * suppresses automatic model grouping rather than guessing.
 */
public interface ClassAssignmentPort {

    /**
     * Resolves the confirmed O4G class for a GTIN on a surface.
     *
     * @param gtin product identity
     * @param surface surface the resolution applies to
     * @return the resolved class, or empty when none is confirmed
     */
    Optional<CanonicalClassId> resolveClass(Gtin gtin, ProjectionSurface surface);
}
