import type { AppApiError } from '~/composables/useApiClient'
import type { CustomerApiKeyListItem } from '~/composables/useCustomerKeysRepository'
import type { KeyCreateRequest, KeyCreateResponse, KeyListItem, KeyRoutingPolicyUpdateRequest } from '~/domains/keys/keys'

export function useCustomerKeys() {
  const repository = useCustomerKeysRepository()
  const rows = ref<KeyListItem[]>([])
  const loading = ref(false)
  const error = ref<AppApiError | null>(null)
  const revealedKey = ref<KeyCreateResponse | null>(null)

  const refresh = async () => {
    loading.value = true
    try {
      rows.value = (await repository.listKeys()).map(toKeyListItem)
      error.value = null
    } catch (err) {
      error.value = err as AppApiError
    } finally {
      loading.value = false
    }
  }

  const createKey = async (payload: KeyCreateRequest) => {
    revealedKey.value = await repository.create(payload) as unknown as KeyCreateResponse
    await refresh()
  }

  const updateRoutingPolicy = async (apiKeyId: string, payload: KeyRoutingPolicyUpdateRequest) => {
    const response = await repository.updateRoutingPolicy(apiKeyId, payload)
    await refresh()
    return response as unknown as KeyCreateResponse
  }

  const rotateKey = async (apiKeyId: string) => {
    revealedKey.value = await repository.rotate(apiKeyId) as unknown as KeyCreateResponse
    await refresh()
  }

  const revokeKey = async (apiKeyId: string) => {
    await repository.revoke(apiKeyId)
    await refresh()
  }

  return {
    rows,
    loading,
    error,
    revealedKey,
    refresh,
    createKey,
    updateRoutingPolicy,
    rotateKey,
    revokeKey
  }
}

function toKeyListItem(item: CustomerApiKeyListItem): KeyListItem {
  return {
    apiKeyId: item.apiKeyId,
    clientId: item.clientId,
    name: item.name,
    keyPrefix: item.keyPrefix,
    tier: item.tier,
    enabled: item.enabled,
    fallbackEnabled: false,
    fallbackBillingMode: '',
    auditTrailEnabled: false,
    bannedWordsDetectorEnabled: item.bannedWordsDetectorEnabled ?? false,
    bannedWordsAction: item.bannedWordsAction ?? 'REJECT',
    bannedWords: item.bannedWords ?? [],
    pseudonymizationEnabled: item.pseudonymizationEnabled ?? false,
    nativeRoutingMode: item.nativeRoutingMode,
    nativeProviderKind: item.nativeProviderKind,
    nativeProviderBaseUrl: item.nativeProviderBaseUrl,
    nativeProviderModel: item.nativeProviderModel,
    nativeProviderHeadersJson: item.nativeProviderHeadersJson,
    nativeProviderDefaultsJson: item.nativeProviderDefaultsJson,
    nativeProviderExtraBodyJson: item.nativeProviderExtraBodyJson,
    nativeProviderCustomPropertiesJson: item.nativeProviderCustomPropertiesJson,
    trafficExperimentEnabled: item.trafficExperimentEnabled,
    trafficExperimentProviderSharePct: item.trafficExperimentProviderSharePct,
    trafficExperimentRulesJson: item.trafficExperimentRulesJson,
    allowedModels: item.allowedModels,
    spendLimitEnabled: Boolean(item.monthlySpendLimitMinor || item.lifetimeSpendLimitMinor),
    spendLimitCurrency: item.spendLimitCurrency,
    monthlySpendLimitMinor: item.monthlySpendLimitMinor,
    lifetimeSpendLimitMinor: item.lifetimeSpendLimitMinor,
    monthlySpendMinor: 0,
    lifetimeSpendMinor: 0,
    spendAlertThreshold: 0,
    createdAt: item.createdAt,
    updatedAt: item.updatedAt,
    lastUsedAt: '',
    requests: 0,
    captured: 0,
    refunded: 0,
    inputTokens: 0,
    outputTokens: 0,
    totalTokens: 0
  }
}
