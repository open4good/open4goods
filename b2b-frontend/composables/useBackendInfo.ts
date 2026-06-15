export function useBackendInfo() {
  const config = useRuntimeConfig()

  return {
    backendBaseUrl: computed(() => config.public.backendBaseUrl)
  }
}
