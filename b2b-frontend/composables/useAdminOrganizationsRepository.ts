import type { AdminBulkResult, AdminAuditItem } from '~/domains/admin/users'
import type { AdminOrganizationItem, AdminOrganizationListQuery, AdminOrganizationPage, AdminOrganizationUpsertRequest } from '~/domains/admin/organizations'

export function useAdminOrganizationsRepository() {
  const { get, post, patch, del } = useApiClient()

  const list = async (query: AdminOrganizationListQuery): Promise<AdminOrganizationPage> => {
    return await get<AdminOrganizationPage>('/api/v1/admin/organizations', query as unknown as Record<string, unknown>)
  }

  const create = async (payload: AdminOrganizationUpsertRequest): Promise<AdminOrganizationItem> => {
    return await post<AdminOrganizationItem>('/api/v1/admin/organizations', payload)
  }

  const update = async (organizationId: string, payload: AdminOrganizationUpsertRequest): Promise<AdminOrganizationItem> => {
    return await patch<AdminOrganizationItem>(`/api/v1/admin/organizations/${encodeURIComponent(organizationId)}`, payload)
  }

  const disable = async (organizationId: string): Promise<void> => {
    await del<unknown>(`/api/v1/admin/organizations/${encodeURIComponent(organizationId)}`)
  }

  const bulkUpdate = async (payload: { organizationIds: string[], status: 'active' | 'disabled' }): Promise<AdminBulkResult> => {
    return await post<AdminBulkResult>('/api/v1/admin/organizations/bulk', payload)
  }

  const timeline = async (organizationId: string): Promise<AdminAuditItem[]> => {
    return await get<AdminAuditItem[]>(`/api/v1/admin/organizations/${encodeURIComponent(organizationId)}/timeline`)
  }

  return {
    list,
    create,
    update,
    disable,
    bulkUpdate,
    timeline
  }
}
