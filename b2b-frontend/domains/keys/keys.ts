import type { AuditLogLevel } from '~/domains/audit/audit'

export interface KeyListQuery {
  page: number
  size: number
  search?: string
  clientId?: string
  tier?: string
  enabled?: boolean
  model?: string
  sort?: string
  days: number
}

export interface KeyListItem {
  apiKeyId: string
  clientId: string
  name: string
  keyPrefix: string
  tier: string
  enabled: boolean
  fallbackEnabled: boolean
  fallbackBillingMode: string
  auditTrailEnabled: boolean
  bannedWordsDetectorEnabled: boolean
  bannedWordsAction: 'AUDIT' | 'REJECT'
  bannedWords: string[]
  pseudonymizationEnabled: boolean
  operationLogLevel?: AuditLogLevel
  operationLogRecentLimit?: number
  performanceRoutingEnabled?: boolean
  performanceMode?: string
  nativeRoutingMode: string
  nativeProviderKind: string
  nativeProviderBaseUrl: string
  nativeProviderModel: string
  nativeProviderHeadersJson: string
  nativeProviderDefaultsJson: string
  nativeProviderExtraBodyJson: string
  nativeProviderCustomPropertiesJson: string
  trafficExperimentEnabled: boolean
  trafficExperimentProviderSharePct: number
  trafficExperimentRulesJson: string
  allowedModels: string[]
  spendLimitEnabled: boolean
  spendLimitCurrency: string
  monthlySpendLimitMinor: number | null
  lifetimeSpendLimitMinor: number | null
  monthlySpendMinor: number
  lifetimeSpendMinor: number
  spendAlertThreshold: number
  createdAt: string
  updatedAt: string
  lastUsedAt: string
  requests: number
  captured: number
  refunded: number
  inputTokens: number
  outputTokens: number
  totalTokens: number
}

export interface KeyPage {
  content: KeyListItem[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface KeySummary {
  totalKeys: number
  activeKeys: number
  capturedRequests: number
  refundedRequests: number
  totalTokens: number
}

export interface KeyTimeseriesPoint {
  timestamp: string
  requests: number
  totalTokens: number
}

export interface KeyBreakdownItem {
  key: string
  requests: number
  totalTokens: number
}

export interface KeyLedgerItem {
  requestId: string
  billingId: string
  billingStatus: string
  modelId: string
  endpoint: string
  nodeId: string
  capturedAt: string
  inputTokens: number
  outputTokens: number
  totalTokens: number
}

export interface KeyAnalytics {
  apiKeyId: string
  clientId: string
  summary: KeyListItem
  timeseries: KeyTimeseriesPoint[]
  byModel: KeyBreakdownItem[]
  ledger: KeyLedgerItem[]
}

export interface KeyCreateRequest {
  tier: 'PUBLIC' | 'TRUSTED' | 'HDS'
  fallbackEnabled: boolean
  fallbackBillingMode: string
  auditTrailEnabled: boolean
  bannedWordsDetectorEnabled: boolean
  bannedWordsAction: 'AUDIT' | 'REJECT'
  bannedWords: string[]
  pseudonymizationEnabled: boolean
  operationLogLevel?: AuditLogLevel
  operationLogRecentLimit?: number
  performanceRoutingEnabled: boolean
  performanceMode: 'TOP_10' | 'TOP_25' | 'TOP_50' | 'DISABLED'
  nativeRoutingMode: 'INFERA_GRID' | 'ORIGINAL_PROVIDER'
  nativeProviderKind: 'OPENAI' | 'GEMINI' | 'CLAUDE' | 'MISTRAL' | 'CUSTOM_OPENAI_COMPATIBLE'
  nativeProviderBaseUrl: string
  nativeProviderApiKey: string
  nativeProviderModel: string
  nativeProviderHeadersJson: string
  nativeProviderDefaultsJson: string
  nativeProviderExtraBodyJson: string
  nativeProviderCustomPropertiesJson: string
  trafficExperimentEnabled: boolean
  trafficExperimentProviderSharePct: number
  trafficExperimentRulesJson: string
  spendLimitEnabled: boolean
  spendLimitCurrency: string
  monthlySpendLimitMinor: number | null
  lifetimeSpendLimitMinor: number | null
  allowedModels: string[]
}

export interface KeyCreateResponse {
  apiKeyId: string
  clientId: string
  name: string
  keyPrefix: string
  keyValue: string | null
  tier: string
  enabled: boolean
  fallbackEnabled: boolean
  fallbackBillingMode: string
  auditTrailEnabled: boolean
  bannedWordsDetectorEnabled: boolean
  bannedWordsAction: 'AUDIT' | 'REJECT'
  bannedWords: string[]
  pseudonymizationEnabled: boolean
  operationLogLevel?: AuditLogLevel
  operationLogRecentLimit?: number
  performanceRoutingEnabled: boolean
  performanceMode: string
  nativeRoutingMode: string
  nativeProviderKind: string
  nativeProviderBaseUrl: string
  nativeProviderApiKey: string
  nativeProviderModel: string
  nativeProviderHeadersJson: string
  nativeProviderDefaultsJson: string
  nativeProviderExtraBodyJson: string
  nativeProviderCustomPropertiesJson: string
  trafficExperimentEnabled: boolean
  trafficExperimentProviderSharePct: number
  trafficExperimentRulesJson: string
  spendLimitEnabled: boolean
  spendLimitCurrency: string
  monthlySpendLimitMinor: number | null
  lifetimeSpendLimitMinor: number | null
  allowedModels: string[]
  policyVersion: string
  updatedAt: string
  createdAt: string
  revokedAt: string
}

export type KeyUpdateRequest = KeyCreateRequest

export interface KeyAuditPolicyUpdateRequest {
  auditTrailEnabled: boolean
  operationLogLevel: AuditLogLevel
  operationLogRecentLimit: number
}

export interface KeyRoutingPolicyUpdateRequest {
  nativeRoutingMode: 'INFERA_GRID' | 'ORIGINAL_PROVIDER'
  nativeProviderKind: 'OPENAI' | 'GEMINI' | 'CLAUDE' | 'MISTRAL' | 'CUSTOM_OPENAI_COMPATIBLE'
  nativeProviderBaseUrl: string
  nativeProviderApiKey: string
  nativeProviderModel: string
  nativeProviderHeadersJson: string
  nativeProviderDefaultsJson: string
  nativeProviderExtraBodyJson: string
  nativeProviderCustomPropertiesJson: string
  trafficExperimentEnabled: boolean
  trafficExperimentProviderSharePct: number
  trafficExperimentRulesJson: string
}
