export type EnergySummary = {
  requests: number
  requestsWithKnownTokens: number
  partialFailures: number
  anomalies: number
  joulesGross: number
  joulesBaseline: number
  joulesNet: number
  avgWatts: number
  tokensTotal: number | null
  jPerToken: number | null
  whPer1kTokens: number | null
}

export type EnergyBreakdownItem = {
  key: string
  label: string
  summary: EnergySummary
}

export type EnergyTimeseriesPoint = {
  bucketStart: string
  summary: EnergySummary
}

export type EnergyLedgerItem = {
  requestId: string
  modelId: string
  apiKeyId: string
  userId: string | null
  orgId: string | null
  nodeId: string
  slotId: string | null
  slotKind: string | null
  methodTier: string
  confidenceScore: number | null
  joulesNet: number
  tokensTotal: number | null
  jPerToken: number | null
  whPer1kTokens: number | null
  partialFailure: boolean
  anomalyFlag: boolean
  exclusionReason: string | null
  eventTime: string
}

export type EnergyAnalyticsResponse = {
  summary: EnergySummary
  timeseries: EnergyTimeseriesPoint[]
  byModel: EnergyBreakdownItem[]
  byApiKey: EnergyBreakdownItem[]
  byUser: EnergyBreakdownItem[]
  byOrganization: EnergyBreakdownItem[]
  byMethodTier: EnergyBreakdownItem[]
  byNode: EnergyBreakdownItem[]
  bySlot: EnergyBreakdownItem[]
  ledger: EnergyLedgerItem[]
  meta: {
    from: string
    to: string
    bucket: string
    scope: string
    scopeId: string | null
    model: string | null
    methodTier: string | null
    minConfidence: number | null
    timezone: string
  }
}

export type EnergyAnalyticsQuery = {
  from?: string
  to?: string
  bucket?: 'hour' | 'day'
  scope?: 'global' | 'apikey' | 'model' | 'user' | 'organization' | 'client' | 'node' | 'slot'
  scopeId?: string
  model?: string
  methodTier?: string
  minConfidence?: number
}

export function numberValue(value: unknown): number {
  if (typeof value === 'number') {
    return Number.isFinite(value) ? value : 0
  }
  if (typeof value === 'string') {
    const parsed = Number(value)
    return Number.isFinite(parsed) ? parsed : 0
  }
  return 0
}

export function nullableNumber(value: unknown): number | null {
  if (value === null || value === undefined || value === '') {
    return null
  }
  return numberValue(value)
}

export function normalizeEnergySummary(payload: Partial<EnergySummary> | undefined): EnergySummary {
  return {
    requests: numberValue(payload?.requests),
    requestsWithKnownTokens: numberValue(payload?.requestsWithKnownTokens),
    partialFailures: numberValue(payload?.partialFailures),
    anomalies: numberValue(payload?.anomalies),
    joulesGross: numberValue(payload?.joulesGross),
    joulesBaseline: numberValue(payload?.joulesBaseline),
    joulesNet: numberValue(payload?.joulesNet),
    avgWatts: numberValue(payload?.avgWatts),
    tokensTotal: nullableNumber(payload?.tokensTotal),
    jPerToken: nullableNumber(payload?.jPerToken),
    whPer1kTokens: nullableNumber(payload?.whPer1kTokens)
  }
}

export function normalizeEnergyAnalyticsResponse(payload: Partial<EnergyAnalyticsResponse>): EnergyAnalyticsResponse {
  return {
    summary: normalizeEnergySummary(payload.summary),
    timeseries: (payload.timeseries ?? []).map((item) => ({
      bucketStart: item.bucketStart ?? '',
      summary: normalizeEnergySummary(item.summary)
    })),
    byModel: normalizeBreakdown(payload.byModel),
    byApiKey: normalizeBreakdown(payload.byApiKey),
    byUser: normalizeBreakdown(payload.byUser),
    byOrganization: normalizeBreakdown(payload.byOrganization),
    byMethodTier: normalizeBreakdown(payload.byMethodTier),
    byNode: normalizeBreakdown(payload.byNode),
    bySlot: normalizeBreakdown(payload.bySlot),
    ledger: (payload.ledger ?? []).map((item) => ({
      requestId: item.requestId ?? '',
      modelId: item.modelId ?? '',
      apiKeyId: item.apiKeyId ?? '',
      userId: item.userId ?? null,
      orgId: item.orgId ?? null,
      nodeId: item.nodeId ?? '',
      slotId: item.slotId ?? null,
      slotKind: item.slotKind ?? null,
      methodTier: item.methodTier ?? '',
      confidenceScore: nullableNumber(item.confidenceScore),
      joulesNet: numberValue(item.joulesNet),
      tokensTotal: nullableNumber(item.tokensTotal),
      jPerToken: nullableNumber(item.jPerToken),
      whPer1kTokens: nullableNumber(item.whPer1kTokens),
      partialFailure: Boolean(item.partialFailure),
      anomalyFlag: Boolean(item.anomalyFlag),
      exclusionReason: item.exclusionReason ?? null,
      eventTime: item.eventTime ?? ''
    })),
    meta: {
      from: payload.meta?.from ?? '',
      to: payload.meta?.to ?? '',
      bucket: payload.meta?.bucket ?? 'hour',
      scope: payload.meta?.scope ?? 'global',
      scopeId: payload.meta?.scopeId ?? null,
      model: payload.meta?.model ?? null,
      methodTier: payload.meta?.methodTier ?? null,
      minConfidence: nullableNumber(payload.meta?.minConfidence),
      timezone: payload.meta?.timezone ?? 'UTC'
    }
  }
}

/** Convert joules to watt-hours. */
export function joulesToWh(joules: number): number {
  return joules / 3600
}

/** Format joules as a human-readable energy string (Wh or kWh). */
export function formatEnergy(joules: number): string {
  const wh = joulesToWh(joules)
  if (wh >= 1000) {
    return `${(wh / 1000).toFixed(3)} kWh`
  }
  if (wh >= 1) {
    return `${wh.toFixed(3)} Wh`
  }
  return `${(wh * 1000).toFixed(1)} mWh`
}

/** Format Wh per 1 k tokens as a readable string. */
export function formatWhPer1kTokens(value: number): string {
  return `${value.toFixed(4)} Wh/1k tok`
}

function normalizeBreakdown(payload: EnergyBreakdownItem[] | undefined): EnergyBreakdownItem[] {
  return (payload ?? []).map((item) => ({
    key: item.key ?? '',
    label: item.label ?? item.key ?? '',
    summary: normalizeEnergySummary(item.summary)
  }))
}
