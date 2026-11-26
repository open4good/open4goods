package org.open4goods.services.opendata.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.open4goods.model.attribute.IndexedAttribute;
import org.open4goods.model.attribute.SourcedAttribute;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.opendata.config.OpenDataConfig;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Guards the size of the Elasticsearch pages of a pass with impact datasets (GOU-2327: 104.86 MB for a 100 MB buffer).
 */
class ImpactDatasetProjectionTest {

    private static final long CLIENT_BUFFER_BYTES = 100L * 1024 * 1024;
    private static final String PREFIX = "attributes.indexed.";

    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void sourceFieldsShouldNeverContainABareCriterion() {
        ImpactDataset impact = impactDataset(List.of("WARRANTY", "WEIGHT"));

        assertThat(impact.sourceFields()).containsExactlyInAnyOrder(
                "attributes.indexed.WARRANTY.value",
                "attributes.indexed.WARRANTY.source.dataSourcename",
                "attributes.indexed.WARRANTY.source.value",
                "attributes.indexed.WARRANTY.source.cleanedValue",
                "attributes.indexed.WEIGHT.value",
                "attributes.indexed.WEIGHT.source.dataSourcename",
                "attributes.indexed.WEIGHT.source.value",
                "attributes.indexed.WEIGHT.source.cleanedValue");
        assertThat(impact.sourceFields()).noneMatch(field -> field.equals(PREFIX + "WARRANTY") || field.equals(PREFIX + "WEIGHT"));
    }

    @Test
    void projectedPageOfAManySourcesProductShouldStayBelowHalfTheClientBuffer() throws Exception {
        List<String> criteria = IntStream.rangeClosed(1, 20).mapToObj(i -> "CRITERION_" + i).toList();
        ImpactDataset impact = impactDataset(criteria);

        ObjectNode indexed = mapper.createObjectNode();
        for (String criterion : criteria) {
            IndexedAttribute attribute = new IndexedAttribute(criterion, "12.5");
            for (int i = 0; i < 40; i++) {
                SourcedAttribute source = new SourcedAttribute();
                source.setDataSourcename("datasource-" + i + ".example.com");
                source.setValue("12.5 kg");
                source.setCleanedValue("12.5");
                source.setLanguage("fr");
                source.setIcecatTaxonomyId(1234);
                source.setName("A rather long attribute name as written by the datasource " + i);
                attribute.getSource().add(source);
            }
            indexed.set(criterion, mapper.valueToTree(attribute));
        }

        ObjectNode projected = mapper.createObjectNode();
        for (String field : impact.sourceFields()) {
            copy(indexed, projected, field.substring(PREFIX.length()).split("\\."), 0);
        }

        long full = mapper.writeValueAsBytes(indexed).length;
        long document = mapper.writeValueAsBytes(projected).length;
        int page = new OpenDataConfig().getImpactExportPageSize();
        assertThat(document).isLessThan(full);
        assertThat(document * page).isLessThan(CLIENT_BUFFER_BYTES / 2);
        assertThat(projected.size()).isEqualTo(criteria.size());
        assertThat(mapper.writeValueAsString(projected)).doesNotContain("icecatTaxonomyId").doesNotContain("language");
        assertThat(new String(mapper.writeValueAsBytes(projected), StandardCharsets.UTF_8)).contains("datasource-39.example.com");
    }

    /** Copies the {@code path} of {@code from} into {@code to}, mapping over arrays like an Elasticsearch source filter. */
    private void copy(JsonNode from, ObjectNode to, String[] path, int depth) {
        JsonNode node = from.get(path[depth]);
        if (node == null) {
            return;
        }
        if (depth == path.length - 1) {
            to.set(path[depth], node);
        } else if (node.isArray()) {
            var array = to.has(path[depth]) ? (tools.jackson.databind.node.ArrayNode) to.get(path[depth])
                    : to.putArray(path[depth]);
            for (int i = 0; i < node.size(); i++) {
                ObjectNode element = i < array.size() ? (ObjectNode) array.get(i) : array.addObject();
                copy(node.get(i), element, path, depth + 1);
            }
        } else {
            ObjectNode child = to.has(path[depth]) ? (ObjectNode) to.get(path[depth]) : to.putObject(path[depth]);
            copy(node, child, path, depth + 1);
        }
    }

    private ImpactDataset impactDataset(List<String> criteria) {
        VerticalConfig config = new VerticalConfig();
        config.setId("oven");
        config.setAvailableImpactScoreCriterias(new ArrayList<>(criteria));
        return new ImpactDataset(config, "https://nudger.fr");
    }
}
