export type AuditLogLevel = 'INFO' | 'DEBUG'

export interface OperationAuditTimelineItem {
  eventId: string
  type: string
  happenedAt: string
  summary: string
  details: Record<string, string>
}

export interface OperationAuditListItem {
  requestId: string
  taskId: string
  endpoint: string
  requestedModel: string
  servedModel: string
  status: string
  streaming: boolean
  apiKeyId: string
  clientId: string
  nodeId: string
  nodeName: string
  slotId: string
  slotKind: string
  routeSource: string
  fallbackPoolId: string
  fallbackBillingMode: string
  policyVersion: string
  operationLogLevel: AuditLogLevel
  inputTokens: number | null
  outputTokens: number | null
  totalTokens: number | null
  errorCode: string
  errorMessage: string
  acceptedAt: string
  completedAt: string
  totalLatencyMs: number | null
  queueWaitMs: number | null
  executionDurationMs: number | null
  firstChunkLatencyMs: number | null
}

export interface OperationAuditDetail extends OperationAuditListItem {
  energyMethodTier: string
  energyConfidenceScore: string
  joulesNet: string
  avgWatts: string
  preauthorizedAt: string
  queuedAt: string
  leasedAt: string
  firstChunkAt: string
  timeline: OperationAuditTimelineItem[]
}

export interface OperationAuditFilter {
  clientId?: string
  apiKeyId?: string
  nodeId?: string
  status?: string
  endpoint?: string
  model?: string
  from?: string
  to?: string
  limit?: number
}
