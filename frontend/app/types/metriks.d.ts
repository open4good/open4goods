/**
 * Metriks Schema 2.0 — TypeScript type definitions.
 *
 * Mirrors the NDJSON / JSON data produced by the metriks orchestrator
 * in open4goods-config.
 */

/** Allowed unit types for a metric value. */
export type MetrikUnit =
  | 'count'
  | 'bytes'
  | 'percent'
  | 'currency'
  | 'ms'
  | 'ratio'
  | 'unknown'

/** Status of a metric event collection. */
export type MetrikStatus = 'ok' | 'error'

/** Size variant for MetrikCard rendering. */
export type MetrikVariant = 'sm' | 'lg' | 'xl'

/**
 * A single metric event as produced by a provider run.
 */
export interface MetrikEvent {
  schemaVersion: string
  id: string
  name: string
  description: string
  value: number | null
  unit: MetrikUnit
  status: MetrikStatus
  errorMessage?: string | null
  url?: string | null
  params: Record<string, unknown>
  groups: string[]
  tags: string[]
}

/**
 * Period covered by a run.
 */
export interface MetrikPeriod {
  dateFrom: string
  dateTo: string
}

/**
 * Git context attached to each run.
 */
export interface MetrikGit {
  repository: string
  sha: string
  ref: string
  event: string
}

/**
 * A single run record (one line in events.ndjson with type "run").
 */
export interface MetrikRun {
  type: 'run'
  schemaVersion: string
  provider: string
  runId: string
  collectedAt: string
  period: MetrikPeriod
  git: MetrikGit
  events: MetrikEvent[]
}

/**
 * Meta record (first line of events.ndjson).
 */
export interface MetrikMeta {
  type: 'meta'
  schemaVersion: string
  provider: string
  providerDisplayName: string
  createdAt: string
  config: Record<string, unknown>
}

/**
 * Shape of the latest.json snapshot file.
 */
export interface MetrikLatest {
  schemaVersion: string
  type: 'latest'
  meta: MetrikMeta
  run: MetrikRun
}

/**
 * A single NDJSON record — either meta or run.
 */
export type MetrikNdjsonRecord = MetrikMeta | MetrikRun

/**
 * Enriched metric with computed trend/variation data,
 * built client-side from the history.
 */
export interface MetrikWithTrend extends MetrikEvent {
  /** Provider that produced this metric. */
  provider: string
  /** Provider display name. */
  providerDisplayName: string
  /** Period of the latest run this event belongs to. */
  period: MetrikPeriod
  /** Previous value (from the prior run), null if unavailable. */
  previousValue: number | null
  /** Absolute change (value − previousValue), null if unavailable. */
  absoluteChange: number | null
  /** Percentage change, null if unavailable or previous was 0. */
  percentChange: number | null
  /** Historical data points for charting: [{ date, value }]. */
  history: MetrikDataPoint[]
}

/**
 * A single data point for time-series charting.
 */
export interface MetrikDataPoint {
  date: string
  value: number | null
}

/**
 * Parsed provider data held in memory.
 */
export interface MetrikProviderData {
  meta: MetrikMeta
  runs: MetrikRun[]
  latest: MetrikRun
}
