package org.open4goods.services.opendata.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.model.Localisable;
import org.open4goods.model.attribute.IndexedAttribute;
import org.open4goods.model.attribute.SourcedAttribute;
import org.open4goods.model.localization.DomainLanguage;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.ProductCanonicalUrl;
import org.open4goods.model.vertical.AttributeConfig;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.opendata.model.ColumnManifest;

/**
 * Columns and rows of the impact dataset of one vertical (GOU-2250): the raw values of the criteria listed by
 * {@code availableImpactScoreCriterias} of the ImpactScore (from {@code impactscores/{vertical}.yml}, available for 5
 * verticals out of 7), not the vertical YAML, without any aggregated score, normalised sub-score, percentile or rank.
 * A published criterion is a criterion that enters the score: {@code MINAVAILABILITYSPAREPARTSYEARS} (dishwasher) and
 * {@code OVEN_ENERGY_EFFICIENCY_INDEX} (oven) are not in their ImpactScore, so they stay unpublished until a later
 * batch decides otherwise. A missing value stays empty: no default and no imputation.
 */
final class ImpactDataset {

    /** Criterion that measures the data, not the product: never published. */
    static final String EXCLUDED_CRITERION = "DATA_QUALITY";

    static final String[] COMMON_HEADER = {"gtin", "brand", "model", "name", "vertical", "nudger_url",
            "eprel_registration_number", "offers_count", "last_updated"};

    private static final String SOURCE_SEPARATOR = " | ";

    private final String vertical;
    private final VerticalConfig verticalConfig;
    private final String siteBaseUrl;
    private final List<String> criteria;
    private final String[] header;
    private final List<ColumnManifest> columnDocs;
    /** Index in the header of the value column of each criterion, the source column follows it. */
    private final int[] valueColumns;

    ImpactDataset(VerticalConfig verticalConfig, String siteBaseUrl) {
        this.verticalConfig = verticalConfig;
        this.vertical = verticalConfig.getId();
        this.siteBaseUrl = StringUtils.removeEnd(StringUtils.defaultString(siteBaseUrl).trim(), "/");
        this.criteria = criteriaOf(verticalConfig);

        List<String> columns = new ArrayList<>(Arrays.asList(COMMON_HEADER));
        List<ColumnManifest> docs = new ArrayList<>();
        for (String name : COMMON_HEADER) {
            docs.add(new ColumnManifest(name, 0));
        }
        Set<String> used = new LinkedHashSet<>(columns);
        this.valueColumns = new int[criteria.size()];
        for (int i = 0; i < criteria.size(); i++) {
            String criterion = criteria.get(i);
            AttributeConfig config = verticalConfig.getAttributesConfig() == null ? null
                    : verticalConfig.getAttributesConfig().getAttributeConfigByKey(criterion);
            String unit = unitOf(config);
            String label = labelOf(config, criterion);

            String unitSlug = slug(unit);
            String valueName = unique(used, criterion.toLowerCase(Locale.ROOT) + (unitSlug.isEmpty() ? "" : "_" + unitSlug));
            String sourceName = unique(used, criterion.toLowerCase(Locale.ROOT) + "_source");
            valueColumns[i] = columns.size();
            columns.add(valueName);
            columns.add(sourceName);
            docs.add(new ColumnManifest(valueName, 0, unit.isEmpty() ? null : unit, label, "Valeur brute de l'attribut " + criterion
                    + " (EPREL, Icecat, marchands : source la plus fiable)"));
            docs.add(new ColumnManifest(sourceName, 0, null, "Origine de la valeur de " + label, "Sources de l'attribut " + criterion));
        }
        this.header = columns.toArray(new String[0]);
        this.columnDocs = List.copyOf(docs);
    }

    /**
     * @param config vertical configuration
     * @return criteria of the vertical, in YAML order, without duplicates and without {@value #EXCLUDED_CRITERION}
     */
    static List<String> criteriaOf(VerticalConfig config) {
        List<String> declared = config.getAvailableImpactScoreCriterias();
        if (declared == null) {
            return List.of();
        }
        return declared.stream()
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .filter(key -> !EXCLUDED_CRITERION.equals(key))
                .distinct()
                .toList();
    }

    String vertical() {
        return vertical;
    }

    String[] header() {
        return header;
    }

    List<ColumnManifest> columnDocs() {
        return columnDocs;
    }

    /**
     * @return the {@code _source} fields the rows read, on top of those of the GTIN and ISBN datasets
     */
    List<String> sourceFields() {
        return criteria.stream().map(key -> "attributes.indexed." + key).toList();
    }

    boolean accepts(Product product) {
        return vertical.equals(product.getVertical());
    }

    String[] toRow(Product product) {
        String[] row = new String[header.length];
        Arrays.fill(row, "");
        row[0] = StringUtils.defaultString(product.gtin());
        row[1] = StringUtils.defaultString(product.brand());
        row[2] = StringUtils.defaultString(product.model());
        row[3] = StringUtils.defaultString(OpenDataService.exportName(product));
        row[4] = vertical;
        String slug = ProductCanonicalUrl.fullSlug(product, verticalConfig, DomainLanguage.fr);
        row[5] = StringUtils.isBlank(slug) || siteBaseUrl.isEmpty() ? "" : siteBaseUrl + slug;
        if (product.getExternalIds() != null) {
            row[6] = StringUtils.defaultString(product.getExternalIds().getEprel());
        }
        if (product.getOffersCount() != null) {
            row[7] = String.valueOf(product.getOffersCount());
        }
        if (product.getLastChange() > 0) {
            row[8] = isoDate(product.getLastChange());
        }

        if (product.getAttributes() == null || product.getAttributes().getIndexed() == null) {
            return row;
        }
        for (int i = 0; i < criteria.size(); i++) {
            IndexedAttribute attribute = product.getAttributes().getIndexed().get(criteria.get(i));
            if (attribute == null || StringUtils.isBlank(attribute.getValue())) {
                continue;
            }
            int column = valueColumns[i];
            row[column] = attribute.getValue().trim();
            row[column + 1] = sourcesOf(attribute);
        }
        return row;
    }

    /**
     * @return the datasources whose value is the published one; empty when none matches it (never all contributors)
     */
    private static String sourcesOf(IndexedAttribute attribute) {
        String published = attribute.getValue().trim();
        Set<String> matching = new TreeSet<>();
        if (attribute.getSource() != null) {
            for (SourcedAttribute source : attribute.getSource()) {
                if (StringUtils.isBlank(source.getDataSourcename())) {
                    continue;
                }
                String value = source.getCleanedValue() != null ? source.getCleanedValue() : source.getValue();
                if (value != null && StringUtils.normalizeSpace(value).equalsIgnoreCase(StringUtils.normalizeSpace(published))) {
                    matching.add(source.getDataSourcename());
                }
            }
        }
        return String.join(SOURCE_SEPARATOR, matching);
    }

    /**
     * @param lastChange epoch milliseconds
     * @return ISO 8601 UTC, to the second
     */
    static String isoDate(long lastChange) {
        return Instant.ofEpochMilli(lastChange).truncatedTo(ChronoUnit.SECONDS).toString();
    }

    private static String unitOf(AttributeConfig config) {
        if (config == null) {
            return "";
        }
        String unit = firstOf(config.getSuffix());
        return unit.isEmpty() ? firstOf(config.getUnit()) : unit;
    }

    private static String firstOf(Localisable<String, String> values) {
        if (values == null) {
            return "";
        }
        String value = values.get("default");
        if (value == null) {
            value = values.get("fr");
        }
        return StringUtils.defaultString(value).trim();
    }

    private static String labelOf(AttributeConfig config, String criterion) {
        if (config == null || config.getName() == null) {
            return criterion;
        }
        String name = config.getName().get("fr");
        if (StringUtils.isBlank(name)) {
            name = config.getName().get("default");
        }
        return StringUtils.defaultIfBlank(name, criterion);
    }

    /**
     * @param unit unit as written in the attribute YAML, for instance {@code Year(s)} or {@code kWh/cycle}
     * @return lowercase ASCII fragment for a column name ({@code year}, {@code kwh_cycle})
     */
    static String slug(String unit) {
        String slug = unit.toLowerCase(Locale.ROOT).replace("(s)", "").replace("%", "percent");
        slug = java.text.Normalizer.normalize(slug, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        slug = slug.replaceAll("[^a-z0-9]+", "_");
        return StringUtils.strip(slug, "_");
    }

    private static String unique(Set<String> used, String wanted) {
        String name = wanted;
        for (int i = 2; !used.add(name); i++) {
            name = wanted + "_" + i;
        }
        return name;
    }
}
