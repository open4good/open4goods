import { toHealthViewModel, type HealthViewModel } from '~/domains/health/health'

export function useBackendApi() {
  const { get } = useApiClient()

  const getHealth = async (): Promise<HealthViewModel> => {
    const payload = await get<unknown>('/api/health')
    return toHealthViewModel(payload)
  }

  return {
    getHealth
  }
}
