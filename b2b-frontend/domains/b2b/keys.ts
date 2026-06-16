export type ApiKeyStatus = 'ACTIVE' | 'REVOKED' | 'ROTATED'

export interface B2bApiKey {
  id: string
  name: string
  keyPrefix: string
  status: ApiKeyStatus
  createdBy: string
  createdAt: string
  lastUsedAt: string | null
  revokedAt: string | null
}

export interface B2bApiKeySecretResponse {
  key: B2bApiKey
  clearKey: string
}

export interface B2bCreateApiKeyRequest {
  name: string
}
