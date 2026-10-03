package org.open4goods.datareference.port;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.open4goods.datareference.model.Gtin;
import org.open4goods.datareference.model.ProjectionSurface;
import org.open4goods.datareference.model.RuleVersion;
import org.open4goods.datareference.model.grouping.GroupConfirmation;
import org.open4goods.datareference.model.grouping.GroupId;
import org.open4goods.datareference.model.grouping.GroupIndexEntry;
import org.open4goods.datareference.model.grouping.GroupMember;
import org.open4goods.datareference.model.grouping.GroupType;

/**
 * Tests GOU-199 AC6/AC7: query port contract backed by the compact index --
 * exact lookup, batch filter by id, GTIN-to-groups, and paginated prefix
 * search distinguishing CONFIRMED from CANDIDATE.
 */
class InMemoryGroupIndexTest {

    private static final RuleVersion VERSION = new RuleVersion("exact-tuple", 1);
    private static final GroupId XR = new GroupId(GroupType.FAMILY, "acme-tv-xr");
    private static final GroupId XR_PLUS = new GroupId(GroupType.FAMILY, "acme-tv-xr-plus");
    private static final GroupId YR = new GroupId(GroupType.FAMILY, "acme-tv-yr");

    private static GroupMember member(String gtin, GroupId groupId, String... tokens) {
        return new GroupMember(new Gtin(gtin), groupId, Optional.empty(), List.of(tokens), Optional.empty(),
                Optional.empty());
    }

    @Test
    void exactLookupFindsAnIndexedGroupOnlyOnItsIndexedSurface() {
        InMemoryGroupIndex index = new InMemoryGroupIndex();
        index.index(ProjectionSurface.NUDGER_WEB, List.of(member("1111111111111", XR, "xr")), VERSION);

        assertThat(index.find(XR, ProjectionSurface.NUDGER_WEB)).isPresent();
        assertThat(index.find(XR, ProjectionSurface.B2B_API)).isEmpty();
        assertThat(index.find(YR, ProjectionSurface.NUDGER_WEB)).isEmpty();
    }

    @Test
    void findAllFiltersByGroupIdAndDropsMissingOnes() {
        InMemoryGroupIndex index = new InMemoryGroupIndex();
        index.index(ProjectionSurface.NUDGER_WEB, List.of(
                member("1111111111111", XR, "xr"),
                member("2222222222222", XR_PLUS, "xr", "plus")), VERSION);

        List<GroupIndexEntry> found =
                index.findAll(List.of(XR, YR, XR_PLUS), ProjectionSurface.NUDGER_WEB);

        assertThat(found).extracting(GroupIndexEntry::groupId).containsExactly(XR, XR_PLUS);
    }

    @Test
    void findByGtinReturnsEveryGroupThatGtinConfirmsEvenOutsideTheRepresentativeSample() {
        InMemoryGroupIndex index = new InMemoryGroupIndex();
        // Six members of the same group: only five fit in representativeGtins.
        List<GroupMember> members = List.of(
                member("1111111111111", XR, "xr"), member("2222222222222", XR, "xr"),
                member("3333333333333", XR, "xr"), member("4444444444444", XR, "xr"),
                member("5555555555555", XR, "xr"), member("6666666666666", XR, "xr"));
        index.index(ProjectionSurface.NUDGER_WEB, members, VERSION);

        Gtin sixth = new Gtin("6666666666666");
        assertThat(index.find(XR, ProjectionSurface.NUDGER_WEB).orElseThrow().representativeGtins())
                .doesNotContain(sixth);
        assertThat(index.findByGtin(sixth, ProjectionSurface.NUDGER_WEB))
                .extracting(GroupIndexEntry::groupId)
                .containsExactly(XR);
    }

    @Test
    void prefixSearchDistinguishesConfirmedFromCandidateEntries() {
        InMemoryGroupIndex index = new InMemoryGroupIndex();
        index.index(ProjectionSurface.NUDGER_WEB, List.of(member("1111111111111", XR, "xr")), VERSION);
        GroupIndexEntry candidate = new GroupIndexEntry(YR, GroupConfirmation.CANDIDATE, List.of(), List.of("xr"),
                Optional.empty(), Optional.empty(), VERSION, 1, List.of(), List.of());
        index.indexCandidates(ProjectionSurface.NUDGER_WEB, List.of(candidate));

        ScanPage<GroupIndexEntry> page = index.searchByPrefix(
                GroupSearchRequest.first("xr", GroupType.FAMILY, 10), ProjectionSurface.NUDGER_WEB);

        assertThat(page.elements()).extracting(GroupIndexEntry::confirmation)
                .containsExactlyInAnyOrder(GroupConfirmation.CONFIRMED, GroupConfirmation.CANDIDATE);
        assertThat(index.findByGtin(new Gtin("1111111111111"), ProjectionSurface.NUDGER_WEB))
                .extracting(GroupIndexEntry::groupId)
                .containsExactly(XR);
    }

    @Test
    void prefixSearchPaginatesStablyAcrossPages() {
        InMemoryGroupIndex index = new InMemoryGroupIndex();
        index.index(ProjectionSurface.NUDGER_WEB, List.of(
                member("1111111111111", XR, "xr"),
                member("2222222222222", XR_PLUS, "xr")), VERSION);

        ScanPage<GroupIndexEntry> firstPage = index.searchByPrefix(
                GroupSearchRequest.first("xr", GroupType.FAMILY, 1), ProjectionSurface.NUDGER_WEB);
        assertThat(firstPage.elements()).hasSize(1);
        assertThat(firstPage.hasMore()).isTrue();

        GroupSearchRequest nextRequest = GroupSearchRequest.first("xr", GroupType.FAMILY, 1)
                .resumeAt(firstPage.nextCursor().orElseThrow());
        ScanPage<GroupIndexEntry> secondPage = index.searchByPrefix(nextRequest, ProjectionSurface.NUDGER_WEB);

        assertThat(secondPage.elements()).hasSize(1);
        assertThat(secondPage.hasMore()).isFalse();
        assertThat(firstPage.elements().get(0).groupId())
                .isNotEqualTo(secondPage.elements().get(0).groupId());
    }
}
