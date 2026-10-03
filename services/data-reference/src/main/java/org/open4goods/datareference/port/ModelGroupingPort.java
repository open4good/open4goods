package org.open4goods.datareference.port;

import java.util.List;
import java.util.Optional;

import org.open4goods.datareference.model.CanonicalClassId;
import org.open4goods.datareference.model.Gtin;
import org.open4goods.datareference.model.ProjectionSurface;
import org.open4goods.datareference.model.SourceRecordHead;
import org.open4goods.datareference.model.projection.GroupAssignment;
import org.open4goods.datareference.model.resolution.ResolvedValue;

/**
 * Assigns the queryable model/family grouping for one GTIN leaf.
 *
 * <p>Follows the typed-input-port pattern of {@link OfferSummaryPort} and
 * {@link SearchSummaryPort}: the assembler supplies already-resolved values
 * and the confirmed class, and this port returns a fully formed
 * {@link GroupAssignment} that {@link DomainSliceComposer} only assembles,
 * never derives.
 *
 * <p>{@code sourceHeads} is carried alongside the resolved values because an
 * explicit provider relation (GOU-49 AC2/AC3) is evidence on the raw head's
 * assertions, not a canonical-attribute value a {@link ResolvedValue} could
 * carry.
 */
public interface ModelGroupingPort {

    /**
     * Assigns the model/family grouping for one surface component.
     *
     * @param gtin product identity
     * @param surface surface the grouping applies to
     * @param sourceHeads current heads of every provider record attached to the GTIN
     * @param resolvedValues resolved reference values for that surface
     * @param resolvedClass confirmed O4G class, when one is resolved
     * @return the assignment; never {@code null}, use {@link GroupAssignment#NONE}
     *     when nothing is confirmed
     */
    GroupAssignment assignGroups(Gtin gtin, ProjectionSurface surface, List<SourceRecordHead> sourceHeads,
            List<ResolvedValue> resolvedValues, Optional<CanonicalClassId> resolvedClass);
}
