export interface OrganizationResponse {
  id: string
  name: string
  slug: string
  balanceCredits: number
  status?: 'ACTIVE' | 'SUSPENDED' | 'CLOSED' | 'PENDING_VALIDATION'
}

export function useCustomerOrganizationRepository() {
  const { session } = useAuthSession()

  function sessionOrg(): OrganizationResponse | null {
    const org = session.value?.organization
    if (!org) return null
    return { id: org.id, name: org.name, slug: org.slug, balanceCredits: org.balanceCredits }
  }

  async function getBySlug(slug: string): Promise<OrganizationResponse | null> {
    const org = sessionOrg()
    return org?.slug === slug ? org : null
  }

  async function list(): Promise<OrganizationResponse[]> {
    const org = sessionOrg()
    return org ? [org] : []
  }

  return { getBySlug, list }
}
