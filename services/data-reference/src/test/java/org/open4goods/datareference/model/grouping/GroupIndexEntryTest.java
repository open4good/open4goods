package org.open4goods.datareference.model.grouping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.open4goods.datareference.model.Gtin;
import org.open4goods.datareference.model.RuleVersion;

/** Tests the compact group index entry's invariants (GOU-199). */
class GroupIndexEntryTest {

    private static final GroupId MODEL_ID = new GroupId(GroupType.MODEL, "acme-tv-xr500");
    private static final RuleVersion VERSION = new RuleVersion("exact-tuple", 1);

    @Test
    void rejectsMoreRepresentativeGtinsThanTheCap() {
        List<Gtin> tooMany = List.of(
                new Gtin("1111111111111"), new Gtin("2222222222222"), new Gtin("3333333333333"),
                new Gtin("4444444444444"), new Gtin("5555555555555"), new Gtin("6666666666666"));

        assertThatThrownBy(() -> new GroupIndexEntry(MODEL_ID, GroupConfirmation.CONFIRMED, List.of(), List.of(),
                Optional.empty(), Optional.empty(), VERSION, 6, tooMany, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(String.valueOf(GroupIndexEntry.MAX_REPRESENTATIVE_GTINS));
    }

    @Test
    void rejectsAMemberCountSmallerThanTheRepresentativeSample() {
        List<Gtin> representatives = List.of(new Gtin("1111111111111"), new Gtin("2222222222222"));

        assertThatThrownBy(() -> new GroupIndexEntry(MODEL_ID, GroupConfirmation.CONFIRMED, List.of(), List.of(),
                Optional.empty(), Optional.empty(), VERSION, 1, representatives, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("memberCount");
    }

    @Test
    void rejectsAPredecessorAliasOfADifferentGroupType() {
        GroupId familyAlias = new GroupId(GroupType.FAMILY, "acme-tv-xr");

        assertThatThrownBy(() -> new GroupIndexEntry(MODEL_ID, GroupConfirmation.CONFIRMED, List.of(), List.of(),
                Optional.empty(), Optional.empty(), VERSION, 0, List.of(), List.of(familyAlias)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("type");
    }

    @Test
    void rejectsItsOwnGroupIdAsAPredecessorAlias() {
        assertThatThrownBy(() -> new GroupIndexEntry(MODEL_ID, GroupConfirmation.CONFIRMED, List.of(), List.of(),
                Optional.empty(), Optional.empty(), VERSION, 0, List.of(), List.of(MODEL_ID)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("own groupId");
    }

    @Test
    void deduplicatesLabelsAndSearchTokensPreservingOrder() {
        GroupIndexEntry entry = new GroupIndexEntry(MODEL_ID, GroupConfirmation.CONFIRMED,
                List.of("Acme XR500", "Acme XR500"), List.of("xr500", "xr", "xr500"), Optional.empty(),
                Optional.empty(), VERSION, 0, List.of(), List.of());

        assertThat(entry.labels()).containsExactly("Acme XR500");
        assertThat(entry.searchTokens()).containsExactly("xr500", "xr");
    }
}
