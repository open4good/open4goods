package org.open4goods.datareference.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.open4goods.datareference.model.AttributionRequirement;
import org.open4goods.datareference.model.CanonicalAttributeId;
import org.open4goods.datareference.model.CanonicalClassId;
import org.open4goods.datareference.model.DerivativeLicence;
import org.open4goods.datareference.model.Gtin;
import org.open4goods.datareference.model.GtinLink;
import org.open4goods.datareference.model.GtinMatchConfidence;
import org.open4goods.datareference.model.GtinMatchMethod;
import org.open4goods.datareference.model.LanguageTag;
import org.open4goods.datareference.model.MediaCachePolicy;
import org.open4goods.datareference.model.PayloadHash;
import org.open4goods.datareference.model.PolicyReviewState;
import org.open4goods.datareference.model.ProjectionSurface;
import org.open4goods.datareference.model.RedistributionPolicy;
import org.open4goods.datareference.model.SourceAssertion;
import org.open4goods.datareference.model.SourceContentType;
import org.open4goods.datareference.model.SourceFieldId;
import org.open4goods.datareference.model.SourceId;
import org.open4goods.datareference.model.SourceRecordCompleteness;
import org.open4goods.datareference.model.SourceRecordHead;
import org.open4goods.datareference.model.SourceRecordKey;
import org.open4goods.datareference.model.SourceRecordState;
import org.open4goods.datareference.model.SourceUsagePolicy;
import org.open4goods.datareference.model.SourceUsagePolicyDocument;
import org.open4goods.datareference.model.SourceUsagePolicyRef;
import org.open4goods.datareference.model.SourceUsagePolicyRegistry;
import org.open4goods.datareference.model.evidence.ClassificationEvidence;
import org.open4goods.datareference.model.grouping.GroupId;
import org.open4goods.datareference.model.grouping.GroupType;
import org.open4goods.datareference.model.projection.GroupAssignment;
import org.open4goods.datareference.model.registry.CanonicalAttributeDefinition;
import org.open4goods.datareference.model.registry.CanonicalClassDefinition;
import org.open4goods.datareference.model.registry.ExternalMapping;
import org.open4goods.datareference.model.registry.ExternalMappingCoordinate;
import org.open4goods.datareference.model.registry.ExternalMappingStatus;
import org.open4goods.datareference.model.registry.RegistryExternalMapping;
import org.open4goods.datareference.model.registry.RegistryVerticalView;
import org.open4goods.datareference.model.registry.RegistryVersion;
import org.open4goods.datareference.model.resolution.ResolutionReason;
import org.open4goods.datareference.model.resolution.ResolvedValue;
import org.open4goods.datareference.model.value.LocalizedTextValue;
import org.open4goods.datareference.port.CanonicalRegistryLookup;
import org.open4goods.datareference.port.SourceRecordHeadStore;

/**
 * Tests GOU-204: {@link RegistryClassAssignmentPort} is the real resolver
 * behind {@link org.open4goods.datareference.port.ClassAssignmentPort}, so
 * AC2 of GOU-49 is no longer inert.
 */
class RegistryClassAssignmentPortTest {

    private static final Instant NOW = Instant.parse("2026-09-16T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final Gtin GTIN = new Gtin("4006381333931");
    private static final CanonicalClassId TELEVISION = new CanonicalClassId("television");
    private static final CanonicalClassId REFRIGERATOR = new CanonicalClassId("refrigerator");

    @Test
    void resolvesTheO4gClassFromAReviewedIcecatMapping() {
        SourceRecordHead head = classifiedHead("icecat", "icecat-1", "icecat", "4321", GtinMatchConfidence.EXACT);
        RegistryClassAssignmentPort port = new RegistryClassAssignmentPort(headStore(head),
                registry(mapping("icecat", "4321", TELEVISION)), allowingPolicies("icecat"), CLOCK);

        Optional<CanonicalClassId> resolved = port.resolveClass(GTIN, ProjectionSurface.NUDGER_WEB);

        assertThat(resolved).contains(TELEVISION);
    }

    /**
     * AC1 of GOU-204: resolved brand/model plus a registry-resolvable GTIN
     * class produce a non-null model group equal to the exact tuple, proving
     * the port is wired to {@link ExactTupleModelGroupingService} and would
     * fail if a port that always returns empty were substituted back in.
     */
    @Test
    void endToEndResolvedClassProducesTheExactTupleModelGroup() {
        SourceRecordHead head = classifiedHead("icecat", "icecat-1", "icecat", "4321", GtinMatchConfidence.EXACT);
        RegistryClassAssignmentPort classAssignment = new RegistryClassAssignmentPort(headStore(head),
                registry(mapping("icecat", "4321", TELEVISION)), allowingPolicies("icecat"), CLOCK);
        CanonicalAttributeId brandAttribute = new CanonicalAttributeId("brand");
        CanonicalAttributeId modelAttribute = new CanonicalAttributeId("model");
        ExactTupleModelGroupingService grouping = new ExactTupleModelGroupingService(brandAttribute, modelAttribute);

        Optional<CanonicalClassId> resolvedClass = classAssignment.resolveClass(GTIN, ProjectionSurface.NUDGER_WEB);
        GroupAssignment assignment = grouping.assignGroups(GTIN, ProjectionSurface.NUDGER_WEB, List.of(),
                List.of(resolvedValue(brandAttribute, "Acme"), resolvedValue(modelAttribute, "XR-500")), resolvedClass);

        assertThat(assignment.modelGroup()).contains(new GroupId(GroupType.MODEL, "10-television-4-acme-6-xr-500"));
    }

    /**
     * AC2 of GOU-204/GOU-49: a GTIN whose classification evidence has no
     * reviewed registry mapping still suspends grouping rather than guessing.
     */
    @Test
    void anUnresolvableClassSuspendsGroupingRatherThanGuessing() {
        SourceRecordHead head = classifiedHead("icecat", "icecat-1", "icecat", "9999", GtinMatchConfidence.EXACT);
        RegistryClassAssignmentPort classAssignment = new RegistryClassAssignmentPort(headStore(head),
                registry(mapping("icecat", "4321", TELEVISION)), allowingPolicies("icecat"), CLOCK);
        CanonicalAttributeId brandAttribute = new CanonicalAttributeId("brand");
        CanonicalAttributeId modelAttribute = new CanonicalAttributeId("model");
        ExactTupleModelGroupingService grouping = new ExactTupleModelGroupingService(brandAttribute, modelAttribute);

        Optional<CanonicalClassId> resolvedClass = classAssignment.resolveClass(GTIN, ProjectionSurface.NUDGER_WEB);
        GroupAssignment assignment = grouping.assignGroups(GTIN, ProjectionSurface.NUDGER_WEB, List.of(),
                List.of(resolvedValue(brandAttribute, "Acme"), resolvedValue(modelAttribute, "XR-500")), resolvedClass);

        assertThat(resolvedClass).isEmpty();
        assertThat(assignment.modelGroup()).isEmpty();
    }

    @Test
    void disagreeingSourcesSuspendResolutionInsteadOfPickingOne() {
        SourceRecordHead first = classifiedHead("icecat", "icecat-1", "icecat", "4321", GtinMatchConfidence.EXACT);
        SourceRecordHead second = classifiedHead("other-feed", "other-1", "icecat", "9000", GtinMatchConfidence.EXACT);
        RegistryClassAssignmentPort port = new RegistryClassAssignmentPort(headStore(first, second),
                registry(mapping("icecat", "4321", TELEVISION), mapping("icecat", "9000", REFRIGERATOR)),
                allowingPolicies("icecat", "other-feed"), CLOCK);

        Optional<CanonicalClassId> resolved = port.resolveClass(GTIN, ProjectionSurface.NUDGER_WEB);

        assertThat(resolved).isEmpty();
    }

    @Test
    void anUnverifiedGtinLinkIsNotTrustedForClassResolution() {
        SourceRecordHead head = classifiedHead("icecat", "icecat-1", "icecat", "4321", GtinMatchConfidence.UNVERIFIED);
        RegistryClassAssignmentPort port = new RegistryClassAssignmentPort(headStore(head),
                registry(mapping("icecat", "4321", TELEVISION)), allowingPolicies("icecat"), CLOCK);

        Optional<CanonicalClassId> resolved = port.resolveClass(GTIN, ProjectionSurface.NUDGER_WEB);

        assertThat(resolved).isEmpty();
    }

    @Test
    void aSourceWhosePolicyDeniesClassificationOnThisSurfaceIsExcluded() {
        SourceRecordHead head = classifiedHead("icecat", "icecat-1", "icecat", "4321", GtinMatchConfidence.EXACT);
        RegistryClassAssignmentPort port = new RegistryClassAssignmentPort(headStore(head),
                registry(mapping("icecat", "4321", TELEVISION)), denyingPolicies("icecat"), CLOCK);

        Optional<CanonicalClassId> resolved = port.resolveClass(GTIN, ProjectionSurface.NUDGER_WEB);

        assertThat(resolved).isEmpty();
    }

    @Test
    void placeholderPortStillSuspendsGroupingAsARegressionGuardAgainstRemovingTheRealResolver() {
        UnresolvedClassAssignmentPort placeholder = new UnresolvedClassAssignmentPort();

        assertThat(placeholder.resolveClass(GTIN, ProjectionSurface.NUDGER_WEB)).isEmpty();
    }

    private static ResolvedValue resolvedValue(CanonicalAttributeId attribute, String text) {
        var assertionId = new org.open4goods.datareference.model.AssertionId("urn:o4g:assertion:" + attribute.slug());
        return new ResolvedValue(attribute, new LocalizedTextValue(text, LanguageTag.UND), assertionId,
                List.of(assertionId), new org.open4goods.datareference.model.RuleVersion("resolution", 1),
                ResolutionReason.CONFIGURED_SOURCE_RANK, false);
    }

    private static SourceRecordHead classifiedHead(String source, String recordId, String scheme, String code,
            GtinMatchConfidence confidence) {
        SourceRecordKey key = SourceRecordKey.of(source, recordId);
        SourceAssertion assertion = SourceAssertion.of(key, new SourceFieldId(source, "category", "1"), 0,
                SourceContentType.CLASSIFICATION, new ClassificationEvidence(scheme, code, null, LanguageTag.UND));
        return new SourceRecordHead(key, "1", null, NOW.minusSeconds(10), NOW.minusSeconds(5), null,
                SourceRecordCompleteness.FULL, SourceRecordState.ACTIVE, new PayloadHash("SHA-256", "aa"),
                URI.create("urn:o4g:evidence:" + source), new SourceUsagePolicyRef(source + "-policy", "1"),
                List.of(new GtinLink(GTIN, confidence, GtinMatchMethod.DECLARED_IDENTIFIER, null)), List.of(assertion));
    }

    private static SourceRecordHeadStore headStore(SourceRecordHead... heads) {
        return new SourceRecordHeadStore() {
            @Override
            public org.open4goods.datareference.model.SourceRecordTransition apply(
                    org.open4goods.datareference.model.SourceRecordMutation mutation) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Optional<SourceRecordHead> find(SourceRecordKey key) {
                return List.of(heads).stream().filter(head -> head.key().equals(key)).findFirst();
            }

            @Override
            public List<SourceRecordHead> findByGtin(Gtin gtin) {
                return List.of(heads);
            }

            @Override
            public boolean storeIfNewer(SourceRecordHead head) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean delete(SourceRecordKey key) {
                throw new UnsupportedOperationException();
            }
        };
    }

    private static Map.Entry<ExternalMappingCoordinate, CanonicalClassId> mapping(String system, String externalId,
            CanonicalClassId classId) {
        return Map.entry(new ExternalMappingCoordinate(system, externalId), classId);
    }

    @SafeVarargs
    private static CanonicalRegistryLookup registry(Map.Entry<ExternalMappingCoordinate, CanonicalClassId>... mappings) {
        Map<ExternalMappingCoordinate, CanonicalClassId> byCoordinate = new LinkedHashMap<>();
        for (Map.Entry<ExternalMappingCoordinate, CanonicalClassId> mapping : mappings) {
            byCoordinate.put(mapping.getKey(), mapping.getValue());
        }
        return new CanonicalRegistryLookup() {
            @Override
            public RegistryVersion version() {
                return new RegistryVersion(1);
            }

            @Override
            public Optional<CanonicalAttributeDefinition> findAttribute(CanonicalAttributeId id) {
                return Optional.empty();
            }

            @Override
            public Optional<CanonicalClassDefinition> findClass(CanonicalClassId id) {
                return Optional.empty();
            }

            @Override
            public Optional<RegistryExternalMapping> findReviewedMapping(String system, String externalId,
                    LocalDate effectiveOn) {
                CanonicalClassId classId = byCoordinate.get(new ExternalMappingCoordinate(system, externalId));
                if (classId == null) {
                    return Optional.empty();
                }
                return Optional.of(new RegistryExternalMapping(classId,
                        new ExternalMapping(system, externalId, ExternalMappingStatus.REVIEWED,
                                LocalDate.of(2000, 1, 1), null)));
            }

            @Override
            public Optional<RegistryVerticalView> findVerticalView(String verticalId) {
                return Optional.empty();
            }
        };
    }

    private static SourceUsagePolicyRegistry allowingPolicies(String... sources) {
        return policiesFor(true, sources);
    }

    private static SourceUsagePolicyRegistry denyingPolicies(String... sources) {
        return policiesFor(false, sources);
    }

    private static SourceUsagePolicyRegistry policiesFor(boolean allowed, String... sources) {
        List<SourceUsagePolicy> policies = List.of(sources).stream()
                .map(source -> new SourceUsagePolicy(source + "-policy", new SourceId(source), "1",
                        Map.of(SourceContentType.CLASSIFICATION, allowed ? Set.of(ProjectionSurface.NUDGER_WEB) : Set.of()),
                        NOW.minus(Duration.ofDays(1)), null,
                        Duration.ofDays(1), MediaCachePolicy.NONE, AttributionRequirement.NONE,
                        RedistributionPolicy.ALLOWED, DerivativeLicence.NONE, Set.of(), LocalDate.of(2026, 9, 1),
                        allowed ? PolicyReviewState.REVIEWED : PolicyReviewState.UNREVIEWED, null,
                        List.of(URI.create("urn:o4g:policy:" + source))))
                .toList();
        return new SourceUsagePolicyRegistry(
                new SourceUsagePolicyDocument(SourceUsagePolicyDocument.SCHEMA_VERSION, policies));
    }
}
