package org.open4goods.nudgerfrontapi.service;

import java.util.List;

import org.open4goods.model.RolesConstants;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.nudgerfrontapi.dto.stats.DatavizChartPresetDto;
import org.open4goods.nudgerfrontapi.dto.stats.DatavizDefaultFilterDto;
import org.open4goods.nudgerfrontapi.dto.stats.VerticalDatavizPlanDto;
import org.open4goods.nudgerfrontapi.localization.DomainLanguage;
import org.open4goods.verticals.VerticalsConfigService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Provides default dataviz presets for vertical statistics pages.
 * <p>
 * The current implementation exposes a curated, localisation-aware plan that can be
 * consumed by the frontend while YAML-driven customisation is progressively introduced.
 * </p>
 */
@Service
public class DatavizStatsService {

    private static final String DEFAULT_DATE_FILTER = "now-2d";

    private final VerticalsConfigService verticalsConfigService;

    public DatavizStatsService(VerticalsConfigService verticalsConfigService) {
        this.verticalsConfigService = verticalsConfigService;
    }

    /**
     * Resolve the dataviz plan for a vertical.
     *
     * @param verticalId vertical identifier
     * @param domainLanguage language driving localisation
     * @return dashboard plan or {@code null} when the vertical is unknown
     */
    public VerticalDatavizPlanDto getVerticalPlan(String verticalId, DomainLanguage domainLanguage) {
        if (!StringUtils.hasText(verticalId)) {
            return null;
        }

        String normalizedVerticalId = verticalId.trim();
        VerticalConfig vertical = verticalsConfigService.getConfigById(normalizedVerticalId);
        if (vertical == null) {
            return null;
        }

        List<DatavizDefaultFilterDto> defaults = List.of(
                new DatavizDefaultFilterDto("lastChange", "range", null, null, DEFAULT_DATE_FILTER),
                new DatavizDefaultFilterDto("offersCount", "range", 1d, null, null)
        );

        return new VerticalDatavizPlanDto(normalizedVerticalId, defaults, buildChartCatalog(domainLanguage));
    }

    private List<DatavizChartPresetDto> buildChartCatalog(DomainLanguage domainLanguage) {
        boolean fr = domainLanguage == DomainLanguage.fr;

        return List.of(
                chart("active-products-kpi", "kpi", fr ? "Produits actifs" : "Active products", fr ? "Nombre de produits actifs" : "Number of active products", "activeProducts", RolesConstants.ROLE_FRONTEND),
                chart("active-offers-kpi", "kpi", fr ? "Offres actives" : "Active offers", fr ? "Nombre total d'offres actives" : "Total number of active offers", "activeOffers", RolesConstants.ROLE_FRONTEND),
                chart("median-min-price-kpi", "kpi", fr ? "Prix minimum médian" : "Median minimum price", fr ? "Médiane du prix minimum" : "Median of minimum prices", "medianMinimumPrice", RolesConstants.ROLE_FRONTEND),
                chart("new-vs-used-kpi", "donut", fr ? "Neuf vs occasion" : "New vs used", fr ? "Répartition neuf / occasion" : "Split between new and used", "newVsUsed", RolesConstants.ROLE_FRONTEND),
                chart("products-by-brand", "bar", fr ? "Produits par marque" : "Products by brand", fr ? "Top marques par volume" : "Top brands by volume", "productsByBrand", RolesConstants.ROLE_FRONTEND),
                chart("brand-market-share", "treemap", fr ? "Parts de marque" : "Brand market share", fr ? "Part des marques dans la verticale" : "Brand shares in the vertical", "brandMarketShare", RolesConstants.ROLE_FRONTEND),
                chart("products-by-platform", "bar", fr ? "Produits par plateforme" : "Products by platform", fr ? "Répartition par source" : "Distribution by source", "productsByPlatform", RolesConstants.ROLE_FRONTEND),
                chart("offers-by-platform", "stacked-bar", fr ? "Offres par plateforme" : "Offers by platform", fr ? "Volume d'offres par source" : "Offer volume per source", "offersByPlatform", RolesConstants.ROLE_FRONTEND),
                chart("condition-by-platform", "stacked-percent", fr ? "Neuf/occasion par plateforme" : "New/used by platform", fr ? "Structure des états par source" : "Condition structure by source", "conditionByPlatform", RolesConstants.ROLE_FRONTEND),
                chart("products-by-country", "bar", fr ? "Produits par pays GTIN" : "Products by GTIN country", fr ? "Origine des GTIN" : "GTIN country distribution", "productsByCountry", RolesConstants.ROLE_FRONTEND),
                chart("new-products-over-time", "line", fr ? "Nouveaux produits" : "New products", fr ? "Nouveaux produits dans le temps" : "New products over time", "newProductsOverTime", RolesConstants.ROLE_FRONTEND),
                chart("updated-products-over-time", "line", fr ? "Produits mis à jour" : "Updated products", fr ? "Produits mis à jour dans le temps" : "Updated products over time", "updatedProductsOverTime", RolesConstants.ROLE_FRONTEND),
                chart("active-offers-over-time", "area", fr ? "Évolution des offres" : "Offer trend", fr ? "Offres actives dans le temps" : "Active offers over time", "activeOffersOverTime", RolesConstants.ROLE_FRONTEND),
                chart("median-price-over-time", "line", fr ? "Prix médian dans le temps" : "Median price over time", fr ? "Évolution du prix médian" : "Median price evolution", "medianPriceOverTime", RolesConstants.ROLE_FRONTEND),
                chart("price-volatility-over-time", "band", fr ? "Volatilité des prix" : "Price volatility", fr ? "Écart-type du prix par période" : "Price standard deviation by period", "priceVolatilityOverTime", RolesConstants.ROLE_FRONTEND),
                chart("min-price-histogram", "histogram", fr ? "Histogramme des prix min" : "Minimum price histogram", fr ? "Distribution des prix minimum" : "Minimum price distribution", "minimumPriceHistogram", RolesConstants.ROLE_FRONTEND),
                chart("price-boxplot-by-brand", "boxplot", fr ? "Boxplot prix par marque" : "Price boxplot by brand", fr ? "Distribution prix pour top marques" : "Price distribution for top brands", "priceBoxplotByBrand", RolesConstants.ROLE_FRONTEND),
                chart("price-distribution-by-platform", "boxplot", fr ? "Prix par plateforme" : "Price by platform", fr ? "Distribution des prix par plateforme" : "Price distribution per platform", "priceDistributionByPlatform", RolesConstants.ROLE_FRONTEND),
                chart("price-deciles", "bar", fr ? "Déciles de prix" : "Price deciles", fr ? "Répartition par déciles" : "Distribution by deciles", "priceDeciles", RolesConstants.ROLE_FRONTEND),
                chart("excluded-vs-included", "donut", fr ? "Exclus vs non exclus" : "Excluded vs included", fr ? "Part des produits exclus" : "Share of excluded products", "excludedVsIncluded", RolesConstants.ROLE_FRONTEND),
                chart("excluded-causes-pareto", "pareto", fr ? "Causes d'exclusion" : "Exclusion causes", fr ? "Top causes d'exclusion" : "Top exclusion causes", "excludedCausesPareto", RolesConstants.ROLE_EDITOR),
                chart("offers-density", "histogram", fr ? "Densité d'offres" : "Offer density", fr ? "Distribution de offersCount" : "offersCount distribution", "offersDensity", RolesConstants.ROLE_FRONTEND),
                chart("price-vs-score", "scatter", fr ? "Prix vs score" : "Price vs score", fr ? "Corrélation prix et score" : "Price and score correlation", "priceVsScore", RolesConstants.ROLE_FRONTEND),
                chart("brand-platform-heatmap", "heatmap", fr ? "Heatmap marque × plateforme" : "Brand × platform heatmap", fr ? "Matrice marque / plateforme" : "Brand / platform matrix", "brandPlatformHeatmap", RolesConstants.ROLE_FRONTEND)
        );
    }

    private DatavizChartPresetDto chart(String id,
                                        String chartType,
                                        String title,
                                        String description,
                                        String queryPreset,
                                        String hasRole) {
        return new DatavizChartPresetDto(id, chartType, title, description, queryPreset, hasRole, true);
    }
}
