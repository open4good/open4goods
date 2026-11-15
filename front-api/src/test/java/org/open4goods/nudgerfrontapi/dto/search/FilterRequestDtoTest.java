package org.open4goods.nudgerfrontapi.dto.search;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.open4goods.nudgerfrontapi.dto.search.FilterRequestDto.FilterField;
import org.open4goods.nudgerfrontapi.dto.search.FilterRequestDto.FilterValueType;

/**
 * Tests for {@link FilterRequestDto}.
 */
class FilterRequestDtoTest {

    /** The "datasource" filter reads the indexed keyword {@code sources}: {@code datasourceCodes} is not indexed (GOU-2301). */
    @Test
    void datasourceFilterTargetsTheSearchableSourcesField() {
        assertThat(FilterField.datasource.fieldPath()).isEqualTo("sources");
        assertThat(FilterField.datasource.valueType()).isEqualTo(FilterValueType.keyword);
        assertThat(FilterField.fromFieldPath("sources")).contains(FilterField.datasource);
    }
}
