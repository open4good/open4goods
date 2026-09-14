package org.open4goods.datareference.model.projection;

import java.util.List;

/**
 * Current product behaviours that will consume a typed projection component.
 *
 * <p>The owner is the WorkOrder that supplies or rewires the named input. This
 * inventory deliberately names behaviours rather than legacy classes so a
 * future implementation cannot retain a {@code Product} dependency merely to
 * satisfy an old call site.
 */
public enum ProjectionConsumer {
    /** Current price and availability supplied by the observation time series. */
    CURRENT_PRICE_AND_AVAILABILITY("price-observation-timeseries", List.of("offers")),
    /** Scores, rankings and dataviz supplied through the evaluation bridge. */
    SCORES_RANKINGS_AND_DATAVIZ("reference-resolution-and-surface-projections", List.of("evaluation")),
    /** Consumer lexical search supplied by the dedicated lexical refactor. */
    LEXICAL_SEARCH("consumer-search-lexical-refactor", List.of("search")),
    /** Product images supplied as policy-filtered reference media. */
    IMAGES("reference-resolution-and-surface-projections", List.of("resolvedValues")),
    /** Sitemap entries supplied by the one-GTIN read model. */
    SITEMAPS("reference-api-frontend-breaking-cutover", List.of("resolvedValues", "search"));

    private final String ownerWorkOrder;
    private final List<String> inputs;

    ProjectionConsumer(String ownerWorkOrder, List<String> inputs) {
        this.ownerWorkOrder = ownerWorkOrder;
        this.inputs = List.copyOf(inputs);
    }

    /** @return WorkOrder responsible for supplying or rewiring this behaviour */
    public String ownerWorkOrder() {
        return ownerWorkOrder;
    }

    /** @return typed projection inputs required by this behaviour */
    public List<String> inputs() {
        return inputs;
    }
}
