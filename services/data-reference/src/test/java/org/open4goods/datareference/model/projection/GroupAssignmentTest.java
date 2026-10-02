package org.open4goods.datareference.model.projection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.open4goods.datareference.model.grouping.GroupId;
import org.open4goods.datareference.model.grouping.GroupType;

/** Tests group-type discipline and deduplication on a group assignment. */
class GroupAssignmentTest {

    @Test
    void rejectsAModelGroupIdThatIsNotTypeModel() {
        GroupId familyTypedId = new GroupId(GroupType.FAMILY, "acme-tv-xr");

        assertThatThrownBy(() -> new GroupAssignment(familyTypedId, List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MODEL");
    }

    @Test
    void rejectsAFamilyGroupIdThatIsNotTypeFamily() {
        GroupId modelTypedId = new GroupId(GroupType.MODEL, "acme-tv-xr500");

        assertThatThrownBy(() -> new GroupAssignment(null, List.of(modelTypedId), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("FAMILY");
    }

    @Test
    void deduplicatesFamilyGroupIdsAndSearchTokensPreservingOrder() {
        GroupId family = new GroupId(GroupType.FAMILY, "acme-tv-xr");

        GroupAssignment assignment = new GroupAssignment(null, List.of(family, family), List.of("xr", "500", "xr"));

        assertThat(assignment.familyGroupIds()).containsExactly(family);
        assertThat(assignment.searchTokens()).containsExactly("xr", "500");
    }

    @Test
    void modelGroupReturnsEmptyWhenNoneConfirmed() {
        assertThat(GroupAssignment.NONE.modelGroup()).isEmpty();
        assertThat(GroupAssignment.NONE.familyGroupIds()).isEmpty();
        assertThat(GroupAssignment.NONE.searchTokens()).isEmpty();
    }

    @Test
    void rejectsABlankSearchToken() {
        assertThatThrownBy(() -> new GroupAssignment(null, List.of(), List.of(" ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blanks");
    }
}
