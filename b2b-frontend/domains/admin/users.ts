export interface AdminUserListQuery {
  page: number
  size: number
  search?: string
  level?: 'user' | 'client' | 'admin'
  status?: 'active' | 'disabled'
  organizationId?: string
  authProvider?: string
  sort?: string
}

export interface AdminUserItem {
  userId: string
  email: string
  level: 'user' | 'client' | 'admin'
  status: 'active' | 'disabled'
  authProvider: string
  organizationId?: string
  createdAt: string
  updatedAt: string
}

export interface AdminAuditItem {
  eventId: string
  entityType: 'user' | 'organization'
  entityId: string
  action: string
  actor: string
  happenedAt: string
  summary: string
}

export interface AdminBulkResult {
  requested: number
  updated: number
  skipped: number
}

export interface AdminUserPage {
  page: number
  size: number
  totalElements: number
  totalPages: number
  content: AdminUserItem[]
}
