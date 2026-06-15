export interface UserProfile {
  id: string
  preferredCurrency: string | null
  accountType: 'PERSONAL' | 'PRO'
  addressStreet: string | null
  addressCity: string | null
  addressZip: string | null
  addressCountry: string | null
  vatNumber: string | null
  siret: string | null
}

export interface UserProfileUpdatePayload {
  preferredCurrency?: string
  accountType?: 'PERSONAL' | 'PRO'
  addressStreet?: string
  addressCity?: string
  addressZip?: string
  addressCountry?: string
  vatNumber?: string
  siret?: string
}

export function useUserProfileRepository() {
  const { get, patch } = useApiClient()

  const fetchProfile = (): Promise<UserProfile> =>
    get<UserProfile>('/api/v1/profile')

  const updateProfile = (payload: UserProfileUpdatePayload): Promise<UserProfile> =>
    patch<UserProfile>('/api/v1/profile', payload)

  return { fetchProfile, updateProfile }
}
