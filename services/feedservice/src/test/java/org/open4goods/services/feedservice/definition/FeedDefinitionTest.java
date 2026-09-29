package org.open4goods.services.feedservice.definition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.open4goods.datareference.model.SourceContentType;
import org.open4goods.datareference.model.SourceId;

class FeedDefinitionTest {

    private static final SourceId SOURCE_ID = new SourceId("merchant.awin.acme");
    private static final Map<String, ColumnTarget> COLUMN_MAPPINGS = Map.of(
            "title", new ColumnTarget.ReferenceField(SourceContentType.TEXT, "name"),
            "price", new ColumnTarget.OfferField(ColumnTarget.OfferFieldKind.PRICE));

    private static FeedDefinition definition(
            List<String> keyColumns, Map<String, String> unitColumns,
            Map<String, ColumnTarget> columnMappings, String providerSchemaVersion) {
        return new FeedDefinition(
                SOURCE_ID,
                providerSchemaVersion,
                keyColumns,
                Locale.FRENCH,
                unitColumns,
                FeedSemantics.FULL,
                columnMappings,
                UnknownColumnPolicy.REPORT);
    }

    private static FeedDefinition valid() {
        return definition(List.of("sku"), Map.of("weight", "weight_unit"), COLUMN_MAPPINGS, "2026-09-01");
    }

    @Test
    void buildsWithValidFields() {
        FeedDefinition definition = valid();

        assertThat(definition.sourceId()).isEqualTo(SOURCE_ID);
        assertThat(definition.declaredColumns()).containsExactlyInAnyOrder(
                "sku", "title", "price", "weight_unit");
    }

    @Test
    void rejectsEmptyKeyColumns() {
        assertThatThrownBy(() -> definition(List.of(), Map.of(), COLUMN_MAPPINGS, "2026-09-01"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("keyColumns");
    }

    @Test
    void rejectsEmptyColumnMappings() {
        assertThatThrownBy(() -> definition(List.of("sku"), Map.of(), Map.of(), "2026-09-01"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("columnMappings");
    }

    @Test
    void rejectsBlankProviderSchemaVersion() {
        assertThatThrownBy(() -> definition(List.of("sku"), Map.of(), COLUMN_MAPPINGS, " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("providerSchemaVersion");
    }

    @Test
    void referenceFieldRejectsOfferContentType() {
        assertThatThrownBy(() -> new ColumnTarget.ReferenceField(SourceContentType.PRICE, "price"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("OFFER or PRICE");
        assertThatThrownBy(() -> new ColumnTarget.ReferenceField(SourceContentType.OFFER, "offer"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("OFFER or PRICE");
    }
}
