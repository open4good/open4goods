package org.open4goods.services.feedservice.definition;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.commons.config.yml.datasource.CsvDataSourceProperties;
import org.open4goods.commons.config.yml.datasource.DataSourceProperties;
import org.open4goods.datareference.model.SourceContentType;
import org.open4goods.datareference.model.SourceId;
import org.open4goods.model.attribute.ReferentielKey;
import org.open4goods.model.helper.IdHelper;
import org.open4goods.services.feedservice.definition.ColumnTarget.OfferField;
import org.open4goods.services.feedservice.definition.ColumnTarget.OfferFieldKind;
import org.open4goods.services.feedservice.definition.ColumnTarget.ReferenceField;

/**
 * Builds a {@link FeedDefinition} deterministically from a feed's already-reviewed
 * {@link DataSourceProperties}, never from a guessed label (AC1/AC2).
 *
 * <p>The column names are not invented here: they are exactly the ones a human declared in
 * the feed's {@code csvDatasource} configuration (loaded from the deployment's
 * {@code datasources} folder). A feed whose configuration declares neither an explicit url
 * nor an explicit price column cannot be read deterministically and is rejected
 * (fail-closed), see {@link MissingRequiredColumnsException}.
 */
public final class FeedDefinitionFactory {

    /**
     * No explicit per-feed schema version is tracked yet in {@link CsvDataSourceProperties};
     * every definition built from it is schema version 1 until that becomes configurable.
     */
    private static final String PROVIDER_SCHEMA_VERSION = "1";

    private FeedDefinitionFactory() {
    }

    public static FeedDefinition from(DataSourceProperties dsProperties) {
        CsvDataSourceProperties csv = dsProperties.getCsvDatasource();
        if (csv == null || StringUtils.isEmpty(csv.getUrl()) || csv.getPrice() == null || csv.getPrice().isEmpty()) {
            throw new MissingRequiredColumnsException(
                    "feed '" + dsProperties.getDatasourceConfigName() + "' must declare both an explicit url "
                            + "column and at least one explicit price column in its csvDatasource configuration");
        }

        Map<String, ColumnTarget> columnMappings = new LinkedHashMap<>();
        putReference(columnMappings, csv.getUrl(), SourceContentType.IDENTITY, "url");
        putReference(columnMappings, csv.getAffiliatedUrl(), SourceContentType.IDENTITY, "affiliatedUrl");
        putOffer(columnMappings, csv.getPrice(), OfferFieldKind.PRICE);
        putReference(columnMappings, csv.getName(), SourceContentType.TEXT, "name");
        putReferenceSet(columnMappings, csv.getDescription(), SourceContentType.TEXT, "description");
        putReferenceSet(columnMappings, csv.getImage(), SourceContentType.MEDIA, "image");
        putOffer(columnMappings, csv.getInStock(), OfferFieldKind.AVAILABILITY);
        putOffer(columnMappings, csv.getProductState(), OfferFieldKind.OFFER_CONDITION);
        putReference(columnMappings, csv.getQuantityInStock(), SourceContentType.ATTRIBUTE, "quantityInStock");
        putReference(columnMappings, csv.getShippingCost(), SourceContentType.ATTRIBUTE, "shippingCost");
        putReference(columnMappings, csv.getShippingTime(), SourceContentType.ATTRIBUTE, "shippingTime");
        putReference(columnMappings, csv.getWarranty(), SourceContentType.ATTRIBUTE, "warranty");
        putReference(columnMappings, csv.getAttrs(), SourceContentType.ATTRIBUTE, "attrs");
        putReferenceSet(columnMappings, csv.getMpn(), SourceContentType.IDENTITY, "mpn");
        putReferenceSet(columnMappings, csv.getSku(), SourceContentType.IDENTITY, "sku");
        if (csv.getReferentiel() != null) {
            for (Map.Entry<ReferentielKey, Set<String>> entry : csv.getReferentiel().entrySet()) {
                putReferenceSet(columnMappings, entry.getValue(), SourceContentType.IDENTITY,
                        entry.getKey().name().toLowerCase(Locale.ROOT));
            }
        }

        String rawSourceId = StringUtils.isEmpty(dsProperties.getFeedKey()) ? dsProperties.getDatasourceConfigName()
                : dsProperties.getFeedKey();
        String sourceId = IdHelper.azCharAndDigitsPointsDash(rawSourceId.toLowerCase(Locale.ROOT));
        Locale language = Locale.forLanguageTag(
                StringUtils.isEmpty(dsProperties.getLanguage()) ? "fr" : dsProperties.getLanguage());

        return new FeedDefinition(
                new SourceId(sourceId),
                PROVIDER_SCHEMA_VERSION,
                List.of(csv.getUrl()),
                language,
                Map.of(),
                FeedSemantics.FULL,
                columnMappings,
                UnknownColumnPolicy.REPORT);
    }

    private static void putReference(Map<String, ColumnTarget> mappings, String column, SourceContentType contentType,
            String canonicalFieldId) {
        if (!StringUtils.isEmpty(column)) {
            mappings.put(column, new ReferenceField(contentType, canonicalFieldId));
        }
    }

    private static void putReferenceSet(Map<String, ColumnTarget> mappings, Set<String> columns,
            SourceContentType contentType, String canonicalFieldId) {
        if (columns == null) {
            return;
        }
        for (String column : columns) {
            putReference(mappings, column, contentType, canonicalFieldId);
        }
    }

    private static void putOffer(Map<String, ColumnTarget> mappings, String column, OfferFieldKind field) {
        if (!StringUtils.isEmpty(column)) {
            mappings.put(column, new OfferField(field));
        }
    }

    private static void putOffer(Map<String, ColumnTarget> mappings, Set<String> columns, OfferFieldKind field) {
        if (columns == null) {
            return;
        }
        for (String column : columns) {
            putOffer(mappings, column, field);
        }
    }
}
