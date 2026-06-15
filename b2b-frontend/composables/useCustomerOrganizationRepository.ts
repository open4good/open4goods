/**
 * Customer organization repository composable for the Product Data API dashboard.
 *
 * In the B2B model, one authenticated user belongs to exactly one organization.
 * Organization data is embedded in the session returned by /api/v1/auth/me and
 * the OIDC login response. There is no separate "list organizations" endpoint;
 * this composable derives the org from the active session state.
 */

/** Lightweight organization DTO matching the backend AuthResponse.AuthOrganizationDto. */
export interface OrganizationResponse {
  id: string
  name: string
  slug: string
  balanceCredits: number
  status?: 'ACTIVE' | 'SUSPENDED' | 'CLOSED' | 'PENDING_VALIDATION'
}

export function useCustomerOrganizationRepository() {
  const { session } = useAuthSession()
  const backendBaseUrl = useRuntimeConfig().public.backendBaseUrl

  /**
   * Returns the organization embedded in the current authenticated session.
   * Throws if the user is not authenticated or has no active organization.
   */
  async function getBySlug(slug: string): Promise<OrganizationResponse | null> {
    try {
      const data = await $fetch<{ organization?: OrganizationResponse }>('/api/v1/auth/me', {
        baseURL: import.meta.server
          ? resolveServerRuntimeBaseUrl(backendBaseUrl, useRequestURL().origin)
          : resolveRuntimeUrl(backendBaseUrl),
        credentials: 'include',
        headers: import.meta.server ? useRequestHeaders(['cookie']) : {}
      })
      const org = data?.organization
      if (!org || org.slug !== slug) return null
      return org
    } catch {
      return null
    }
  }

  /**
   * Returns the organizations the current user belongs to.
   * In the B2B model this is always at most one (the session org).
   */
  async function list(): Promise<OrganizationResponse[]> {
    if (!session.value) return []
    try {
      const data = await $fetch<{ organization?: OrganizationResponse }>('/api/v1/auth/me', {
        baseURL: import.meta.server
          ? resolveServerRuntimeBaseUrl(backendBaseUrl, useRequestURL().origin)
          : resolveRuntimeUrl(backendBaseUrl),
        credentials: 'include',
        headers: import.meta.server ? useRequestHeaders(['cookie']) : {}
      })
      return data?.organization ? [data.organization] : []
    } catch {
      return []
    }
  }

  return { getBySlug, list }
}
