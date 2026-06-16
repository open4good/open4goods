import type { B2bAdminOrganization, B2bAdminUsageEvent, B2bAdminAuditEvent, B2bManualGrantRequest, B2bManualGrantResponse } from '~/domains/b2b/admin'
import type { B2bApiKey } from '~/domains/b2b/keys'
import type { B2bTransaction } from '~/domains/b2b/billing'

export function useAdminRepository() {
  const { get, post } = useApiClient()

  const listOrganizations = () => get<B2bAdminOrganization[]>('/api/v1/admin/organizations')
  const getOrganization = (id: string) => get<B2bAdminOrganization>(`/api/v1/admin/organizations/${id}`)
  const getOrganizationTransactions = (id: string, limit = 50) => get<B2bTransaction[]>(`/api/v1/admin/organizations/${id}/transactions`, { limit })
  const grantManualCredits = (id: string, req: B2bManualGrantRequest) => post<B2bManualGrantResponse>(`/api/v1/admin/organizations/${id}/credits/grants`, req)
  const listApiKeys = () => get<B2bApiKey[]>('/api/v1/admin/api-keys')
  const revokeApiKey = (id: string) => post<B2bApiKey>(`/api/v1/admin/api-keys/${id}/revoke`)
  const listUsage = (limit = 50) => get<B2bAdminUsageEvent[]>('/api/v1/admin/usage', { limit })
  const listAuditEvents = (limit = 50) => get<B2bAdminAuditEvent[]>('/api/v1/admin/audit', { limit })

  return { listOrganizations, getOrganization, getOrganizationTransactions, grantManualCredits, listApiKeys, revokeApiKey, listUsage, listAuditEvents }
}
