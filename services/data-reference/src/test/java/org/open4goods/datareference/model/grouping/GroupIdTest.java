package org.open4goods.datareference.model.grouping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** Tests the stable identity and serialized form of a group id. */
class GroupIdTest {

    @Test
    void externalFormCarriesTypeAndSlug() {
        GroupId modelGroup = new GroupId(GroupType.MODEL, "acme-tv-xr500");

        assertThat(modelGroup.externalForm()).isEqualTo("model:acme-tv-xr500");
        assertThat(modelGroup.toString()).isEqualTo("model:acme-tv-xr500");
    }

    @Test
    void parseRoundTripsTheExternalForm() {
        GroupId familyGroup = GroupId.parse("family:acme-tv-xr");

        assertThat(familyGroup).isEqualTo(new GroupId(GroupType.FAMILY, "acme-tv-xr"));
    }

    @Test
    void rejectsASlugThatIsNotLowerCaseKebabCase() {
        assertThatThrownBy(() -> new GroupId(GroupType.MODEL, "Acme_TV"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("kebab-case");
    }

    @Test
    void rejectsAnUnsupportedSerializedType() {
        assertThatThrownBy(() -> GroupId.parse("series:acme-tv"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported group type");
    }

    @Test
    void rejectsAMissingTypeSeparator() {
        assertThatThrownBy(() -> GroupId.parse("acme-tv"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("<type>:<slug>");
    }
}
