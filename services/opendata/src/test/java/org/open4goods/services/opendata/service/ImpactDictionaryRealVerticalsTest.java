package org.open4goods.services.opendata.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.open4goods.model.vertical.AttributeConfig;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.opendata.model.ColumnManifest;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.open4goods.verticals.GoogleTaxonomyService;
import org.open4goods.verticals.VerticalsConfigService;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * The impact CSV headers built from the real vertical YAML must match the published dictionary
 * ({@code docs/architecture/opendata-impact-dictionary.md}), and no criterion may lose its unit (GOU-2308).
 */
class ImpactDictionaryRealVerticalsTest {

    private static final Path DICTIONARY = Path.of("../../docs/architecture/opendata-impact-dictionary.md");

    /** Tokens that a parser strips because they are the unit of the value. */
    private static final Set<String> UNIT_TOKENS = Set.of("W", "KW", "KWH", "KWH/H", "KG", "G", "DB", "L", "CM", "MM", "KM",
            "A", "V", "HZ", "WH", "MAH", "GB", "TB", "MB", "ANS", "AN", "YEARS", "YEAR");

    private static List<VerticalConfig> realVerticals() {
        VerticalsConfigService service = new VerticalsConfigService(new SerialisationService(),
                mock(GoogleTaxonomyService.class), new PathMatchingResourcePatternResolver());
        return service.getConfigsWithoutDefault().stream()
                .filter(config -> !ImpactDataset.criteriaOf(config).isEmpty()).toList();
    }

    /** @return per vertical, the dictionary rows (column to unit) in file order */
    private static Map<String, Map<String, String>> dictionary() throws IOException {
        Map<String, Map<String, String>> sections = new LinkedHashMap<>();
        Map<String, String> current = null;
        for (String line : Files.readAllLines(DICTIONARY, StandardCharsets.UTF_8)) {
            if (line.startsWith("## ")) {
                String title = line.substring(3).trim();
                int paren = title.indexOf(" (`impact-");
                current = paren > 0 ? sections.computeIfAbsent(title.substring(0, paren), k -> new LinkedHashMap<>()) : null;
            } else if (current != null && line.startsWith("| `")) {
                String[] cells = line.split("\\|");
                String unit = cells[2].trim();
                current.put(cells[1].trim().replace("`", ""), "-".equals(unit) ? "" : unit);
            }
        }
        return sections;
    }

    @Test
    void impactHeadersShouldMatchTheDictionaryForEveryRealVertical() throws IOException {
        List<VerticalConfig> verticals = realVerticals();
        Map<String, Map<String, String>> dictionary = dictionary();
        assertThat(verticals).hasSize(7);
        for (VerticalConfig config : verticals) {
            ImpactDataset impact = new ImpactDataset(config, "https://nudger.fr");
            Map<String, String> actual = new LinkedHashMap<>();
            List<ColumnManifest> docs = impact.columnDocs();
            for (ColumnManifest column : docs.subList(ImpactDataset.COMMON_HEADER.length, docs.size())) {
                actual.put(column.name(), column.unit() == null ? "" : column.unit());
            }
            assertThat(dictionary).as("dictionary section of %s", config.getId()).containsKey(config.getId());
            assertThat(new ArrayList<>(dictionary.get(config.getId()).entrySet()))
                    .as("columns and units of %s", config.getId())
                    .isEqualTo(new ArrayList<>(actual.entrySet()));
        }
        assertThat(dictionary.keySet()).containsExactlyInAnyOrderElementsOf(
                verticals.stream().map(VerticalConfig::getId).toList());
    }

    @Test
    void criterionWhoseParserRemovesTheUnitShouldPublishItsUnit() {
        List<String> missing = new ArrayList<>();
        for (VerticalConfig config : realVerticals()) {
            for (String key : ImpactDataset.criteriaOf(config)) {
                AttributeConfig attribute = config.getAttributesConfig() == null ? null
                        : config.getAttributesConfig().getAttributeConfigByKey(key);
                if (attribute == null) {
                    missing.add(config.getId() + ":" + key + " (no attribute config)");
                    continue;
                }
                boolean parserRemovesUnit = attribute.getParser() != null && attribute.getParser().getDeleteTokens() != null
                        && attribute.getParser().getDeleteTokens().stream()
                                .anyMatch(token -> UNIT_TOKENS.contains(token.trim().toUpperCase(Locale.ROOT)));
                boolean hasUnit = (attribute.getSuffix() != null && !attribute.getSuffix().isEmpty())
                        || (attribute.getUnit() != null && !attribute.getUnit().isEmpty());
                if (parserRemovesUnit && !hasUnit) {
                    missing.add(config.getId() + ":" + key);
                }
            }
        }
        assertThat(missing).isEmpty();
    }
}
