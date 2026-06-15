export interface CustomerApiKeyListItem {
  apiKeyId: string
  clientId: string
  name: string
  keyPrefix: string
  keyValue: string | null
  tier: string
  enabled: boolean
  bannedWordsDetectorEnabled?: boolean
  bannedWordsAction?: 'AUDIT' | 'REJECT'
  bannedWords?: string[]
  pseudonymizationEnabled?: boolean
  allowedModels: string[]
  spendLimitCurrency: string
  monthlySpendLimitMinor: number | null
  lifetimeSpendLimitMinor: number | null
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
  createdAt: string
  updatedAt: string
  revokedAt: string
}

export interface CustomerApiKeyCreateRequest {
  tier: 'PUBLIC' | 'TRUSTED' | 'HDS'
  fallbackEnabled: boolean
  fallbackBillingMode: string
  auditTrailEnabled: boolean
  bannedWordsDetectorEnabled: boolean
  bannedWordsAction: 'AUDIT' | 'REJECT'
  bannedWords: string[]
  pseudonymizationEnabled: boolean
  operationLogLevel?: string
  operationLogRecentLimit?: number
  performanceRoutingEnabled: boolean
  performanceMode: 'TOP_10' | 'TOP_25' | 'TOP_50' | 'DISABLED'
  nativeRoutingMode?: 'INFERA_GRID' | 'ORIGINAL_PROVIDER'
  nativeProviderKind?: 'OPENAI' | 'GEMINI' | 'CLAUDE' | 'MISTRAL' | 'CUSTOM_OPENAI_COMPATIBLE'
  nativeProviderBaseUrl?: string
  nativeProviderApiKey?: string
  nativeProviderModel?: string
  nativeProviderHeadersJson?: string
  nativeProviderDefaultsJson?: string
  nativeProviderExtraBodyJson?: string
  nativeProviderCustomPropertiesJson?: string
  trafficExperimentEnabled?: boolean
  trafficExperimentProviderSharePct?: number
  trafficExperimentRulesJson?: string
  spendLimitEnabled: boolean
  spendLimitCurrency: string
  monthlySpendLimitMinor: number | null
  lifetimeSpendLimitMinor: number | null
  allowedModels: string[]
}

export interface CustomerApiKeyRoutingPolicyUpdateRequest {
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

export function useCustomerKeysRepository() {
  const { get, post, patch } = useApiClient()

  const listKeys = async (): Promise<CustomerApiKeyListItem[]> => {
    return await get<CustomerApiKeyListItem[]>('/api/v1/customer/api-keys')
  }

  const create = async (payload: CustomerApiKeyCreateRequest): Promise<CustomerApiKeyListItem> => {
    return await post<CustomerApiKeyListItem>('/api/v1/customer/api-keys', payload)
  }

  const rotate = async (apiKeyId: string): Promise<CustomerApiKeyListItem> => {
    return await post<CustomerApiKeyListItem>(`/api/v1/customer/api-keys/${encodeURIComponent(apiKeyId)}/rotate`)
  }

  const updateRoutingPolicy = async (apiKeyId: string, payload: CustomerApiKeyRoutingPolicyUpdateRequest): Promise<CustomerApiKeyListItem> => {
    return await patch<CustomerApiKeyListItem>(`/api/v1/customer/api-keys/${encodeURIComponent(apiKeyId)}/routing-policy`, payload)
  }

  const revoke = async (apiKeyId: string): Promise<void> => {
    await post(`/api/v1/customer/api-keys/${encodeURIComponent(apiKeyId)}/revoke`)
  }

  return {
    listKeys,
    create,
    rotate,
    updateRoutingPolicy,
    revoke
  }
}
