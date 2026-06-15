import type { OperationAuditDetail, OperationAuditFilter, OperationAuditListItem } from '~/domains/audit/audit'

export function useAdminOperationAuditRepository() {
  const { get } = useApiClient()

  const list = async (filter: OperationAuditFilter): Promise<OperationAuditListItem[]> => {
    return await get<OperationAuditListItem[]>('/api/v1/admin/logs', filter as Record<string, unknown>)
  }

  const detail = async (requestId: string): Promise<OperationAuditDetail> => {
    return await get<OperationAuditDetail>(`/api/v1/admin/logs/${encodeURIComponent(requestId)}`)
  }

  return {
    list,
    detail
  }
}
