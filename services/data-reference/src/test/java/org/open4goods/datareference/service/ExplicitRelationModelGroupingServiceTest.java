package org.open4goods.datareference.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.open4goods.datareference.model.Gtin;
import org.open4goods.datareference.model.GtinLink;
import org.open4goods.datareference.model.GtinMatchConfidence;
import org.open4goods.datareference.model.GtinMatchMethod;
import org.open4goods.datareference.model.LanguageTag;
import org.open4goods.datareference.model.PayloadHash;
import org.open4goods.datareference.model.ProjectionSurface;
import org.open4goods.datareference.model.SourceFieldId;
import org.open4goods.datareference.model.SourceRecordCompleteness;
import org.open4goods.datareference.model.SourceRecordHead;
import org.open4goods.datareference.model.SourceRecordKey;
import org.open4goods.datareference.model.SourceRecordMutation;
import org.open4goods.datareference.model.SourceRecordState;
import org.open4goods.datareference.model.SourceRecordTransition;
import org.open4goods.datareference.model.SourceAssertion;
import org.open4goods.datareference.model.SourceContentType;
import org.open4goods.datareference.model.SourceUsagePolicyRef;
import org.open4goods.datareference.model.evidence.RelationEvidence;
import org.open4goods.datareference.model.grouping.GroupId;
import org.open4goods.datareference.model.grouping.GroupType;
import org.open4goods.datareference.model.projection.GroupAssignment;
import org.open4goods.datareference.port.SourceRecordHeadStore;

/**
 * Tests the remaining part of GOU-49 AC2 and the explicit-relation part of
 * AC3: an explicit provider relation is as strong evidence as the exact
 * normalized tuple, resolved from source heads rather than resolved values.
 */
class ExplicitRelationModelGroupingServiceTest {

    private static final Gtin OWN_GTIN = new Gtin("4006381333931");
    private static final Gtin TARGET_GTIN = new Gtin("5901234123457");
    private static final Instant AT = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void aVariantOfRelationToAKnownRecordConfirmsAModelGroup() {
        StubStore store = new StubStore();
        store.put(targetHead("REC-41", TARGET_GTIN, GtinMatchConfidence.EXACT));
        ExplicitRelationModelGroupingService service = new ExplicitRelationModelGroupingService(store);

        SourceRecordHead ownHead = headWithRelation("REC-42", "variant-of", "icecat-record", "REC-41");

        GroupAssignment assignment = service.assignGroups(OWN_GTIN, ProjectionSurface.NUDGER_WEB,
                List.of(ownHead), List.of(), Optional.empty());

        assertThat(assignment.modelGroup()).contains(
                new GroupId(GroupType.MODEL, "4006381333931-5901234123457"));
    }

    @Test
    void theConfirmedModelGroupIsTheSameRegardlessOfWhichSideIsRebuilt() {
        StubStore store = new StubStore();
        store.put(targetHead("REC-41", TARGET_GTIN, GtinMatchConfidence.EXACT));
        ExplicitRelationModelGroupingService service = new ExplicitRelationModelGroupingService(store);

        SourceRecordHead ownHead = headWithRelation("REC-42", "variant-of", "icecat-record", "REC-41");
        GroupAssignment fromOwnSide = service.assignGroups(OWN_GTIN, ProjectionSurface.NUDGER_WEB,
                List.of(ownHead), List.of(), Optional.empty());

        StubStore reverseStore = new StubStore();
        reverseStore.put(targetHead("REC-42", OWN_GTIN, GtinMatchConfidence.EXACT));
        SourceRecordHead reverseHead = headWithRelation("REC-41", "variant-of", "icecat-record", "REC-42");
        GroupAssignment fromTargetSide = new ExplicitRelationModelGroupingService(reverseStore)
                .assignGroups(TARGET_GTIN, ProjectionSurface.NUDGER_WEB, List.of(reverseHead), List.of(),
                        Optional.empty());

        assertThat(fromOwnSide.modelGroup()).isEqualTo(fromTargetSide.modelGroup());
    }

    @Test
    void aFamilyOfRelationConfirmsAFamilyGroupNotAModelGroup() {
        StubStore store = new StubStore();
        store.put(targetHead("REC-41", TARGET_GTIN, GtinMatchConfidence.EXACT));
        ExplicitRelationModelGroupingService service = new ExplicitRelationModelGroupingService(store);

        SourceRecordHead ownHead = headWithRelation("REC-42", "family-of", "icecat-record", "REC-41");

        GroupAssignment assignment = service.assignGroups(OWN_GTIN, ProjectionSurface.NUDGER_WEB,
                List.of(ownHead), List.of(), Optional.empty());

        assertThat(assignment.modelGroup()).isEmpty();
        assertThat(assignment.familyGroupIds()).containsExactly(
                new GroupId(GroupType.FAMILY, "4006381333931-5901234123457"));
    }

    @Test
    void anUnrecognizedRelationTypeCarriesNoGroupingWeight() {
        StubStore store = new StubStore();
        store.put(targetHead("REC-41", TARGET_GTIN, GtinMatchConfidence.EXACT));
        ExplicitRelationModelGroupingService service = new ExplicitRelationModelGroupingService(store);

        SourceRecordHead ownHead = headWithRelation("REC-42", "accessory-of", "icecat-record", "REC-41");

        GroupAssignment assignment = service.assignGroups(OWN_GTIN, ProjectionSurface.NUDGER_WEB,
                List.of(ownHead), List.of(), Optional.empty());

        assertThat(assignment.modelGroup()).isEmpty();
        assertThat(assignment.familyGroupIds()).isEmpty();
    }

    @Test
    void anUnresolvableSchemeIsIgnoredRatherThanGuessed() {
        StubStore store = new StubStore();
        ExplicitRelationModelGroupingService service = new ExplicitRelationModelGroupingService(store);

        SourceRecordHead ownHead = headWithRelation("REC-42", "variant-of", "mpn", "XR-500");

        GroupAssignment assignment = service.assignGroups(OWN_GTIN, ProjectionSurface.NUDGER_WEB,
                List.of(ownHead), List.of(), Optional.empty());

        assertThat(assignment.modelGroup()).isEmpty();
    }

    @Test
    void aGtinSchemeTargetIsResolvedDirectlyWithoutAStoreLookup() {
        StubStore store = new StubStore();
        ExplicitRelationModelGroupingService service = new ExplicitRelationModelGroupingService(store);

        SourceRecordHead ownHead = headWithRelation("REC-42", "variant-of", "gtin", TARGET_GTIN.value());

        GroupAssignment assignment = service.assignGroups(OWN_GTIN, ProjectionSurface.NUDGER_WEB,
                List.of(ownHead), List.of(), Optional.empty());

        assertThat(assignment.modelGroup()).contains(
                new GroupId(GroupType.MODEL, "4006381333931-5901234123457"));
    }

    @Test
    void anUnknownTargetRecordIsIgnoredRatherThanFailing() {
        StubStore store = new StubStore();
        ExplicitRelationModelGroupingService service = new ExplicitRelationModelGroupingService(store);

        SourceRecordHead ownHead = headWithRelation("REC-42", "variant-of", "icecat-record", "REC-999");

        GroupAssignment assignment = service.assignGroups(OWN_GTIN, ProjectionSurface.NUDGER_WEB,
                List.of(ownHead), List.of(), Optional.empty());

        assertThat(assignment.modelGroup()).isEmpty();
    }

    private static SourceRecordHead headWithRelation(String ownRecordId, String relationType, String targetScheme,
            String targetIdentifier) {
        SourceRecordKey key = SourceRecordKey.of("icecat", ownRecordId);
        SourceFieldId field = new SourceFieldId("icecat", "relation", "v1");
        return new SourceRecordHead(key, "1", null, AT, AT, null, SourceRecordCompleteness.FULL,
                SourceRecordState.ACTIVE, new PayloadHash("SHA-256", "9f86d081884c7d659a2feaa0c55ad015"),
                URI.create("urn:o4g:evidence:" + key.externalForm()), new SourceUsagePolicyRef("icecat-standard", "1"),
                List.of(),
                List.of(SourceAssertion.of(key, field, 0, SourceContentType.RELATION,
                        new RelationEvidence(relationType, targetScheme, targetIdentifier, LanguageTag.UND))));
    }

    private static SourceRecordHead targetHead(String recordId, Gtin linkedGtin, GtinMatchConfidence confidence) {
        SourceRecordKey key = SourceRecordKey.of("icecat", recordId);
        return new SourceRecordHead(key, "1", null, AT, AT, null, SourceRecordCompleteness.FULL,
                SourceRecordState.ACTIVE, new PayloadHash("SHA-256", "9f86d081884c7d659a2feaa0c55ad015"),
                URI.create("urn:o4g:evidence:" + key.externalForm()), new SourceUsagePolicyRef("icecat-standard", "1"),
                List.of(new GtinLink(linkedGtin, confidence, GtinMatchMethod.DECLARED_IDENTIFIER, null)),
                List.of());
    }

    private static final class StubStore implements SourceRecordHeadStore {
        private final java.util.Map<SourceRecordKey, SourceRecordHead> heads = new java.util.HashMap<>();

        void put(SourceRecordHead head) {
            heads.put(head.key(), head);
        }

        @Override
        public SourceRecordTransition apply(SourceRecordMutation mutation) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<SourceRecordHead> find(SourceRecordKey key) {
            return Optional.ofNullable(heads.get(key));
        }

        @Override
        public List<SourceRecordHead> findByGtin(Gtin gtin) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean storeIfNewer(SourceRecordHead head) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean delete(SourceRecordKey key) {
            throw new UnsupportedOperationException();
        }
    }
}
