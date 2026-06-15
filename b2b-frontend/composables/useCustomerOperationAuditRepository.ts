import type { OperationAuditDetail, OperationAuditFilter, OperationAuditListItem } from '~/domains/audit/audit'

export function useCustomerOperationAuditRepository() {
  const { get } = useApiClient()

  const list = async (filter: OperationAuditFilter): Promise<OperationAuditListItem[]> => {
    return await get<OperationAuditListItem[]>('/api/v1/customer/logs', filter as Record<string, unknown>)
  }

  const detail = async (requestId: string): Promise<OperationAuditDetail> => {
    return await get<OperationAuditDetail>(`/api/v1/customer/logs/${encodeURIComponent(requestId)}`)
  }

  return {
    list,
    detail
  }
}
