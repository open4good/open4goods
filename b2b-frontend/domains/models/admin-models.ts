export interface AdminModelCatalogItem {
  id: string
  galleryFamily: string
  galleryHardwareTier: string
  name: string
  description?: string
  license?: string
  backend?: string
  downloadPriority: number
  sizeBytes?: number
  sha256?: string
  enabled: boolean
  updatedAt: string
}

export interface AdminModelTierPolicy {
  tier: string
  policyVersion: number
  modelIds: string[]
  updatedAt: string
}

export interface AdminNodeModelOverride {
  nodeId: string
  modelId: string
  action: 'FORCE_INCLUDE' | 'FORCE_EXCLUDE'
  createdAt: string
  updatedAt: string
}

export interface CatalogSyncResult {
  inserted: number
  updated: number
  disabled: number
}

export function formatSizeBytes(bytes?: number | null): string {
  if (!bytes) return '—'
  const gb = bytes / (1024 ** 3)
  return gb >= 1 ? `${gb.toFixed(2)} GB` : `${(bytes / (1024 ** 2)).toFixed(0)} MB`
}
