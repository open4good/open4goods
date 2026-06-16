export type AdminOrgStatus = 'ACTIVE' | 'SUSPENDED' | 'CLOSED' | 'PENDING_VALIDATION'

export interface B2bAdminOrganization {
  id: string
  name: string
  slug: string
  billingEmail: string | null
  defaultLanguage: string | null
  status: AdminOrgStatus
  freeGrantApplied: boolean
  creditBalance: number
  createdAt: string
  updatedAt: string
}

export interface B2bAdminUsageEvent {
  id: string
  organizationId: string
  organizationName: string
  apiKeyId: string | null
  apiKeyPrefix: string | null
  facetId: string
  gtin: string | null
  requestId: string
  httpStatus: number
  billable: boolean
  creditsConsumed: number
  noPayReason: string | null
  responseTimeMs: number | null
  createdAt: string
}

export interface B2bAdminAuditEvent {
  id: string
  actorUserId: string
  actorUserEmail: string
  action: string
  targetOrganizationId: string | null
  targetOrganizationName: string | null
  targetRef: string | null
  detail: Record<string, unknown>
  createdAt: string
}

export interface B2bManualGrantRequest {
  credits: number
  note: string
  expiresAt?: string
}

export interface B2bManualGrantResponse {
  bucketId: string
  creditsGranted: number
  durableBalance: number
}
