package org.open4goods.datareference.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.open4goods.datareference.model.CanonicalClassId;
import org.open4goods.datareference.model.Gtin;
import org.open4goods.datareference.model.GtinMatchConfidence;
import org.open4goods.datareference.model.ProjectionSurface;
import org.open4goods.datareference.model.SourceAssertion;
import org.open4goods.datareference.model.SourceContentType;
import org.open4goods.datareference.model.SourceRecordHead;
import org.open4goods.datareference.model.SourceUsagePolicyRegistry;
import org.open4goods.datareference.model.evidence.ClassificationEvidence;
import org.open4goods.datareference.model.registry.RegistryExternalMapping;
import org.open4goods.datareference.port.CanonicalRegistryLookup;
import org.open4goods.datareference.port.ClassAssignmentPort;
import org.open4goods.datareference.port.SourceRecordHeadStore;

/**
 * Resolves the O4G class a GTIN belongs to from reviewed provider-taxonomy
 * mappings in the canonical registry (the Icecat-to-O4G mapping delivered in
 * GOU-47 and GOU-131), rather than from a new classification mechanism.
 *
 * <p>Only an unambiguous class confirms grouping (ADR-0010: "missing
 * brand/class/model prevents automatic exact grouping"): a GTIN with no
 * classification evidence, no reviewed mapping for the evidence it has, or
 * usable sources whose reviewed mappings disagree all suspend resolution
 * rather than guess one of several candidates.
 */
public final class RegistryClassAssignmentPort implements ClassAssignmentPort {

    private final SourceRecordHeadStore sourceHeads;
    private final CanonicalRegistryLookup registry;
    private final SourceUsagePolicyRegistry policies;
    private final Clock clock;

    /**
     * Creates a class resolver backed by the source heads, the canonical
     * registry and the usage policies in force.
     *
     * @param sourceHeads current heads of every provider record
     * @param registry read access to the authored O4G registry
     * @param policies source usage policies gating classification republication
     * @param clock clock used to evaluate head usability and mapping effectiveness
     */
    public RegistryClassAssignmentPort(SourceRecordHeadStore sourceHeads, CanonicalRegistryLookup registry,
            SourceUsagePolicyRegistry policies, Clock clock) {
        this.sourceHeads = Objects.requireNonNull(sourceHeads, "sourceHeads must not be null");
        this.registry = Objects.requireNonNull(registry, "registry must not be null");
        this.policies = Objects.requireNonNull(policies, "policies must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public Optional<CanonicalClassId> resolveClass(Gtin gtin, ProjectionSurface surface) {
        Objects.requireNonNull(gtin, "gtin must not be null");
        Objects.requireNonNull(surface, "surface must not be null");

        Instant at = clock.instant();
        LocalDate effectiveOn = at.atZone(ZoneOffset.UTC).toLocalDate();

        Set<CanonicalClassId> resolvedClasses = new LinkedHashSet<>();
        for (SourceRecordHead head : sourceHeads.findByGtin(gtin)) {
            if (!head.isUsableAt(at) || !linkedWithConfidence(head, gtin)) {
                continue;
            }
            for (SourceAssertion assertion : head.assertions()) {
                if (!(assertion.evidence() instanceof ClassificationEvidence classification)) {
                    continue;
                }
                if (!policies.allows(head.key().sourceId(), head.usagePolicyRef(), SourceContentType.CLASSIFICATION,
                        surface, at)) {
                    continue;
                }
                registry.findReviewedMapping(classification.scheme(), classification.code(), effectiveOn)
                        .map(RegistryExternalMapping::conceptId)
                        .filter(CanonicalClassId.class::isInstance)
                        .map(CanonicalClassId.class::cast)
                        .ifPresent(resolvedClasses::add);
            }
        }
        return resolvedClasses.size() == 1 ? Optional.of(resolvedClasses.iterator().next()) : Optional.empty();
    }

    private static boolean linkedWithConfidence(SourceRecordHead head, Gtin gtin) {
        return head.gtinLinks().stream()
                .filter(link -> gtin.equals(link.gtin()))
                .anyMatch(link -> link.confidence() != GtinMatchConfidence.UNVERIFIED);
    }
}
