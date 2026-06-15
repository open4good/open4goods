// AUTO-GENERATED FILE - DO NOT EDIT

export type HealthEntity = {
  status: string
  components?: Record<string, unknown>
}

export function mapHealthResponse(payload: unknown): HealthEntity {
  const status = typeof (payload as { status?: unknown })?.status === 'string' ? (payload as { status: string }).status : 'UNKNOWN'
  const components = typeof (payload as { components?: unknown })?.components === 'object' && (payload as { components?: unknown }).components !== null
    ? (payload as { components: Record<string, unknown> }).components
    : undefined

  return {
    status,
    components
  }
}
