package org.open4goods.datareference.model.registry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.open4goods.datareference.model.CanonicalAttributeId;
import org.open4goods.datareference.model.CanonicalClassId;

class GitRegistryLoaderTest {

    private final GitRegistryLoader loader = new GitRegistryLoader();

    @Test
    void loadsTheCheckedInRegistryAsAnImmutableVersionedIndex() throws IOException {
        RegistryRuntimeIndex index = loader.loadDefault();

        assertThat(index.registry().version()).isEqualTo(new RegistryVersion(1));
        assertThat(index.classCount()).isEqualTo(7);
        assertThat(index.attributeCount()).isEqualTo(2);
        assertThat(index.contentHash()).matches("[0-9a-f]{64}");
        assertThat(index.registry().findClass(new CanonicalClassId("television"))).isPresent();
        assertThat(index.registry().findAttribute(new CanonicalAttributeId("width")))
                .hasValueSatisfying(attribute -> {
                    assertThat(attribute.dimension()).isEqualTo("LENGTH");
                    assertThat(attribute.canonicalUnit().value()).isEqualTo("cm");
                    assertThat(attribute.labels()).containsEntry("en", "Width").containsEntry("fr", "Largeur");
                });
        assertThat(index.registry().findReviewedMapping("icecat", "feature:1464", LocalDate.of(2026, 9, 11)))
                .hasValueSatisfying(mapping -> assertThat(mapping.conceptId().externalForm())
                        .isEqualTo("o4g:attribute:width"));
        assertThat(index.registry().findReviewedMapping("icecat", "feature:1464", LocalDate.of(2026, 9, 10)))
                .isEmpty();
        assertThat(index.registry().findVerticalView("tv"))
                .hasValueSatisfying(view -> assertThat(view.includedClasses())
                        .containsExactly(new CanonicalClassId("television")));
    }

    @Test
    void rejectsAnUnknownJsonPropertyBeforeBuildingTheRuntimeIndex() {
        String invalid = registryJson().replace("\"registryVersion\": 1,",
                "\"registryVersion\": 1, \"providerShortcut\": true,");

        assertThatThrownBy(() -> load(invalid))
                .isInstanceOf(RegistryValidationException.class)
                .hasMessageContaining("unknown property: providerShortcut");
    }

    @Test
    void rejectsDanglingClassAttributesAndOverlappingReviewedMappings() {
        String dangling = registryJson().replace("\"o4g:attribute:width\", \"o4g:attribute:color\"",
                "\"o4g:attribute:unknown\", \"o4g:attribute:color\"");
        String overlapping = registryJson()
                .replace("\"system\": \"wikidata\"", "\"system\": \"icecat\"")
                .replace("\"externalId\": \"P462\"", "\"externalId\": \"feature:1464\"");

        assertThatThrownBy(() -> load(dangling))
                .isInstanceOf(RegistryValidationException.class)
                .hasMessageContaining("unknown attribute");
        assertThatThrownBy(() -> load(overlapping))
                .isInstanceOf(RegistryValidationException.class)
                .hasMessageContaining("overlapping reviewed mapping");
    }

    @Test
    void requiresOneDimensionAndUcumUnitForQuantities() {
        String invalid = registryJson().replace("\"dimension\": \"LENGTH\"", "\"dimension\": null");

        assertThatThrownBy(() -> load(invalid))
                .isInstanceOf(RegistryValidationException.class)
                .hasMessageContaining("quantity attribute must declare one dimension");
    }

    @Test
    void rejectsAClassHierarchyCycleAndMissingTranslations() {
        String cyclic = registryJson().replaceFirst("\"parent\": null,", "\"parent\": \"o4g:class:dishwasher\",")
                .replaceFirst("\"parent\": null,", "\"parent\": \"o4g:class:television\",");
        String untranslated = registryJson().replace("\"labels\": {\"en\": \"Television\", \"fr\": \"Téléviseur\"}",
                "\"labels\": {\"en\": \"Television\"}");

        assertThatThrownBy(() -> load(cyclic))
                .isInstanceOf(RegistryValidationException.class)
                .hasMessageContaining("cycle");
        assertThatThrownBy(() -> load(untranslated))
                .isInstanceOf(RegistryValidationException.class)
                .hasMessageContaining("English and French translations");
    }

    @Test
    void rejectsAnUnknownClassFromAnEditorialVerticalView() {
        String invalid = registryJson().replace("\"o4g:class:television\"]}", "\"o4g:class:unknown\"]}");

        assertThatThrownBy(() -> load(invalid))
                .isInstanceOf(RegistryValidationException.class)
                .hasMessageContaining("references an unknown class");
    }

    private RegistryRuntimeIndex load(String content) throws IOException {
        return loader.load(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
    }

    private static String registryJson() {
        try (var stream = GitRegistryLoaderTest.class.getResourceAsStream(GitRegistryLoader.REGISTRY_RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("registry test resource is missing");
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("cannot read registry test resource", exception);
        }
    }
}
