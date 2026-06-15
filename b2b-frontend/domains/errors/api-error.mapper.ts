export type ProblemDetails = {
  type?: string
  title?: string
  status?: number
  detail?: string
  instance?: string
}

export type AppApiError = {
  status: number
  code: string
  i18nKey: string
  details?: string
  problem?: ProblemDetails
  context?: ApiErrorContext
}

export type ApiErrorContext = {
  method?: string
  path?: string
  url?: string
  runtime?: 'server' | 'client'
  elapsedMs?: number
}

const statusToCode: Record<number, string> = {
  0: 'network_error',
  400: 'bad_request',
  401: 'unauthorized',
  403: 'forbidden',
  404: 'not_found',
  410: 'gone',
  409: 'conflict',
  422: 'validation_error',
  429: 'rate_limited',
  500: 'internal_error',
  502: 'bad_gateway',
  503: 'service_unavailable'
}

const problemDetailToCode = new Set([
  'action_must_be_approve_or_deny',
  'challenge_agent_mismatch',
  'challenge_expired',
  'challenge_invalid_data',
  'challenge_not_approved',
  'challenge_not_found',
  'invalid_bootstrap_key',
  'otp_invalid',
  'otp_missing',
  'validation_error'
])

function parseProblemData(error: unknown): ProblemDetails | undefined {
  const payload = (error as { data?: unknown })?.data
  if (!payload || typeof payload !== 'object') {
    return undefined
  }

  const candidate = payload as Record<string, unknown>
  return {
    type: typeof candidate.type === 'string' ? candidate.type : undefined,
    title: typeof candidate.title === 'string' ? candidate.title : undefined,
    status: typeof candidate.status === 'number' ? candidate.status : undefined,
    detail: typeof candidate.detail === 'string' ? candidate.detail : undefined,
    instance: typeof candidate.instance === 'string' ? candidate.instance : undefined
  }
}

function parseProblemTypeCode(type?: string): string | undefined {
  if (!type) {
    return undefined
  }

  const slug = type.split('/').pop()?.trim()
  return normalizeProblemCode(slug)
}

function isTransportFailure(error: unknown): boolean {
  const candidate = error as { status?: number, statusCode?: number, response?: unknown }
  return candidate.status == null && candidate.statusCode == null && candidate.response == null
}

function extractErrorMessage(error: unknown): string | undefined {
  if (error instanceof Error) {
    return error.message
  }
  return (error as { message?: string })?.message
}

export function mapApiError(error: unknown): AppApiError {
  const err = error as { status?: number, statusCode?: number }
  const problem = parseProblemData(error)
  const status = err.status ?? err.statusCode ?? problem?.status ?? (isTransportFailure(error) ? 0 : 500)
  const problemCode = normalizeProblemCode(problem?.detail) ?? parseProblemTypeCode(problem?.type)
  const code = problemCode ?? statusToCode[status] ?? 'unknown_error'
  const context = (error as { context?: ApiErrorContext })?.context

  const mapped = {
    status,
    code,
    i18nKey: `errors.${code}`,
    details: problem?.detail ?? extractErrorMessage(error),
    problem,
    context
  }
  console.warn('[API Error Mapper] Mapped API failure', {
    status,
    code,
    detail: mapped.details,
    problem,
    context
  })
  return mapped
}

function normalizeProblemCode(detail?: string): string | undefined {
  if (!detail) {
    return undefined
  }

  const code = detail.trim().toLowerCase().replace(/\s+/g, '_')
  return problemDetailToCode.has(code) ? code : undefined
}
