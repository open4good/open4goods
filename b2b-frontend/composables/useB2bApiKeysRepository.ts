import type { B2bApiKey, B2bApiKeySecretResponse, B2bCreateApiKeyRequest } from '~/domains/b2b/keys'

export function useB2bApiKeysRepository() {
  const { get, post } = useApiClient()

  const list = () => get<B2bApiKey[]>('/api/v1/customer/api-keys')
  const create = (req: B2bCreateApiKeyRequest) => post<B2bApiKeySecretResponse>('/api/v1/customer/api-keys', req)
  const rotate = (id: string) => post<B2bApiKeySecretResponse>(`/api/v1/customer/api-keys/${id}/rotate`)
  const revoke = (id: string) => post<B2bApiKey>(`/api/v1/customer/api-keys/${id}/revoke`)

  return { list, create, rotate, revoke }
}
