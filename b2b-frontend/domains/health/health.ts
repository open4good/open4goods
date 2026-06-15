import { mapHealthResponse, type HealthEntity } from './health.gen'
import { healthSchema } from './health.zod.gen'

export type HealthStatus = 'UP' | 'DOWN' | 'DEGRADED' | 'UNKNOWN'

export type HealthViewModel = HealthEntity & {
  normalizedStatus: HealthStatus
}

export function toHealthViewModel(payload: unknown): HealthViewModel {
  const mapped = mapHealthResponse(payload)
  const parsed = healthSchema.safeParse(mapped)
  const normalized = parsed.success ? parsed.data : { status: 'UNKNOWN' as const, components: {} }

  return {
    ...normalized,
    normalizedStatus: normalizeStatus(normalized.status)
  }
}

function normalizeStatus(value: string): HealthStatus {
  if (value === 'UP') {
    return 'UP'
  }

  if (value === 'DOWN' || value === 'OUT_OF_SERVICE') {
    return 'DOWN'
  }

  if (value === 'DEGRADED') {
    return 'DEGRADED'
  }

  return 'UNKNOWN'
}
