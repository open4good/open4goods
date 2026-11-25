/**
 * Props of the DatavizChart component (ECharts bar / line / donut).
 *
 * Kept local to the frontend: the charts are fed by the guide widgets
 * (BrandShareChart) from product search aggregations, not by a dedicated
 * front-api endpoint.
 */
export interface DatavizChartPreset {
  id?: string
  chartType?: string
  title?: string
  description?: string
}

/** Labels and values drawn by DatavizChart. */
export interface DatavizChartData {
  chartType?: string
  title?: string
  labels?: string[]
  values?: number[]
  totalHits?: number
}
