package org.open4goods.model.vertical;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration for the dataviz statistics page of a vertical.
 */
public class DatavizConfig {

    /**
     * List of chart IDs that should be enabled.
     * If populated, only these charts (and default ones not in disabled list) will be shown?
     * Or does this override the default catalog?
     * The logic usually is: Start with catalog, filter by enabled (if present), exclude disabled.
     */
    private List<String> enabledCharts = new ArrayList<>();

    /**
     * List of chart IDs that should be disabled/hidden.
     */
    private List<String> disabledCharts = new ArrayList<>();

    /**
     * Map of chart ID to configuration overrides (title, description).
     */
    private Map<String, DatavizChartOverride> chartOverrides = new HashMap<>();

    /**
     * List of custom hero KPIs to display.
     */
    private List<DatavizHeroKpi> heroKpis = new ArrayList<>();

    public List<String> getEnabledCharts() {
        return enabledCharts;
    }

    public void setEnabledCharts(List<String> enabledCharts) {
        this.enabledCharts = enabledCharts;
    }

    public List<String> getDisabledCharts() {
        return disabledCharts;
    }

    public void setDisabledCharts(List<String> disabledCharts) {
        this.disabledCharts = disabledCharts;
    }

    public Map<String, DatavizChartOverride> getChartOverrides() {
        return chartOverrides;
    }

    public void setChartOverrides(Map<String, DatavizChartOverride> chartOverrides) {
        this.chartOverrides = chartOverrides;
    }

    public List<DatavizHeroKpi> getHeroKpis() {
        return heroKpis;
    }

    public void setHeroKpis(List<DatavizHeroKpi> heroKpis) {
        this.heroKpis = heroKpis;
    }
}
