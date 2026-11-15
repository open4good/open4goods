package org.open4goods.model.eprel;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class EprelModelAliasTableTest {

    private static EprelModelAliasTable table() {
        EprelModelAliasTable table = new EprelModelAliasTable();
        table.setBrand("APPLE");
        table.setModels(List.of(
                new EprelModelAlias("A3090", List.of("A2846"), List.of("iPhone 15"), null),
                new EprelModelAlias("A3094", List.of("A2847"), List.of("iPhone 15 Plus"), List.of("MU163")),
                new EprelModelAlias("A3102", null, List.of("iPhone 15 Pro"), null),
                new EprelModelAlias("A3409", null, List.of("iPhone 16e"), null)));
        return table;
    }

    @Test
    void longestCommercialNameWins() {
        assertThat(table().resolveEprelModel("Apple", List.of("Apple iPhone 15 Plus 6Go/128Go - Noir")))
                .contains("A3094");
        assertThat(table().resolveEprelModel("APPLE", List.of("Apple iPhone 15 Pro 256 Go Titane"))).contains("A3102");
        assertThat(table().resolveEprelModel("APPLE", List.of("iPhone 15 128 Go Rose"))).contains("A3090");
    }

    @Test
    void gluedAndSuffixedNamesAreSplit() {
        assertThat(table().resolveEprelModel("APPLE", List.of("IPHONE15PLUS-128-BLK"))).contains("A3094");
        assertThat(table().resolveEprelModel("APPLE", List.of("Apple iPhone 16e 128 Go Blanc"))).contains("A3409");
        assertThat(table().resolveEprelModel("APPLE", List.of("Apple iPhone 16 E 128 Go Blanc"))).contains("A3409");
    }

    @Test
    void regulatoryNumbersAndPartNumbersResolve() {
        assertThat(table().resolveEprelModel("APPLE", List.of("A2847"))).contains("A3094");
        assertThat(table().resolveEprelModel("APPLE", List.of("MU163ZD/A"))).contains("A3094");
        assertThat(table().resolveEprelModel("APPLE", List.of("mu163sx/a"))).contains("A3094");
        // a part-number base must be the whole base, not a prefix of another code
        assertThat(table().resolveEprelModel("APPLE", List.of("MU1630ZD/A"))).isEmpty();
    }

    @Test
    void otherBrandsAndAccessoriesAreIgnored() {
        assertThat(table().resolveEprelModel("Samsung", List.of("iPhone 15 Plus"))).isEmpty();
        assertThat(table().resolveEprelModel(null, List.of("iPhone 15 Plus"))).isEmpty();
        assertThat(table().resolveEprelModel("APPLE", List.of("Coque pour iPhone 15 Plus"))).isEmpty();
        assertThat(table().resolveEprelModel("APPLE", List.of("Case compatible iPhone 15"))).isEmpty();
        assertThat(table().resolveEprelModel("APPLE", List.of("Apple iPhone 11 128 Go"))).isEmpty();
    }

    @Test
    void strictMajorityIsRequired() {
        assertThat(table().resolveEprelModel("APPLE",
                List.of("Apple iPhone 15 128 Go", "iPhone 15 - Noir", "iPhone 15 Plus 128 Go", "MHDQ3F/A")))
                .contains("A3090");
        assertThat(table().resolveEprelModel("APPLE", List.of("Apple iPhone 15 128 Go", "iPhone 15 Plus 128 Go")))
                .isEmpty();
    }
}
