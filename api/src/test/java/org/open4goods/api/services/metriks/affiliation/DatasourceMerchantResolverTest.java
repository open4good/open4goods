package org.open4goods.api.services.metriks.affiliation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.open4goods.commons.config.yml.datasource.DataSourceProperties;
import org.open4goods.commons.services.DataSourceConfigService;

/**
 * Tests the programme identifier extraction of {@link DatasourceMerchantResolver}.
 */
class DatasourceMerchantResolverTest {

    private static DataSourceProperties datasource(String name, String prefix, String portal) {
        DataSourceProperties properties = new DataSourceProperties();
        properties.setName(name);
        properties.setAffiliationLinkPrefix(prefix);
        properties.setAffiliatedPortalUrl(portal);
        return properties;
    }

    @Test
    void awinProgrammeOf_readsAwinmidOrMidOfAnAwinLinkOnly() {
        assertThat(DatasourceMerchantResolver.awinProgrammeOf("https://www.awin1.com/cread.php?awinmid=10001&awinaffid=2&ued="))
                .contains("10001");
        assertThat(DatasourceMerchantResolver.awinProgrammeOf("https://www.awin1.com/awclick.php?mid=10002&id=2"))
                .contains("10002");
        assertThat(DatasourceMerchantResolver.awinProgrammeOf("https://example.test/cread.php?awinmid=10001")).isEmpty();
        assertThat(DatasourceMerchantResolver.awinProgrammeOf("https://www.awin1.com/cread.php?awinmid=abc")).isEmpty();
        assertThat(DatasourceMerchantResolver.awinProgrammeOf("not a url %%")).isEmpty();
        assertThat(DatasourceMerchantResolver.awinProgrammeOf(null)).isEmpty();
    }

    @Test
    void index_mapsProgrammesToDatasourcesAndDropsAmbiguousOnes() {
        Map<String, String> index = DatasourceMerchantResolver.index(List.of(
                datasource("marchand-a.fr", "https://www.awin1.com/cread.php?awinmid=10001&awinaffid=2&ued=", null),
                datasource("marchand-b.fr", null, "https://www.awin1.com/awclick.php?mid=10002&id=2"),
                datasource("dup-1.fr", null, "https://www.awin1.com/awclick.php?mid=10009&id=2"),
                datasource("dup-2.fr", "https://www.awin1.com/cread.php?awinmid=10009", null),
                datasource("sans-awin.fr", "https://track.example.test/?x=1", null)));

        assertThat(index).containsOnly(Map.entry("10001", "marchand-a.fr"), Map.entry("10002", "marchand-b.fr"));
    }

    @Test
    void resolver_resolvesAwinProgrammesOnly() {
        DataSourceConfigService configs = org.mockito.Mockito.mock(DataSourceConfigService.class);
        org.mockito.Mockito.when(configs.datasourceConfigs()).thenReturn(Map.of("a.yml",
                datasource("marchand-a.fr", "https://www.awin1.com/cread.php?awinmid=10001&awinaffid=2&ued=", null)));
        DatasourceMerchantResolver resolver = new DatasourceMerchantResolver(configs);

        assertThat(resolver.datasourceOf("awin", "10001")).contains("marchand-a.fr");
        assertThat(resolver.datasourceOf("awin", "99999")).isEmpty();
        assertThat(resolver.datasourceOf("kwanko", "10001")).isEmpty();
        assertThat(resolver.hasProgramme("awin", "marchand-a.fr")).isTrue();
        assertThat(resolver.hasProgramme("awin", "autre.fr")).isFalse();
        assertThat(resolver.hasProgramme("kwanko", "marchand-a.fr")).isFalse();
    }
}
