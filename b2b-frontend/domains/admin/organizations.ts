export interface AdminOrganizationListQuery {
  page: number
  size: number
  search?: string
  status?: 'active' | 'disabled'
  clientId?: string
  sort?: string
}

export interface AdminOrganizationItem {
  organizationId: string
  name: string
  clientId: string
  status: 'active' | 'disabled'
  spendCeilingCurrency: string
  monthlySpendCeilingMinor: number | null
  lifetimeSpendCeilingMinor: number | null
  monthlySpendMinor: number
  lifetimeSpendMinor: number
  spendAlertThreshold: number
  createdAt: string
  updatedAt: string
  usersCount: number
}

export interface AdminOrganizationUpsertRequest {
  name: string
  clientId: string
  status: 'active' | 'disabled'
  spendCeilingCurrency: string
  monthlySpendCeilingMinor: number | null
  lifetimeSpendCeilingMinor: number | null
}

export interface AdminOrganizationPage {
  page: number
  size: number
  totalElements: number
  totalPages: number
  content: AdminOrganizationItem[]
}
