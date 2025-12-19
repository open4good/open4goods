import type { ReleaseNote } from '~~/types/releases'

export const useLatestRelease = async () => {
  const existing = useNuxtData<ReleaseNote | null>('latest-release')
  const requestHeaders = useRequestHeaders(['host', 'x-forwarded-host'])

  const hasExistingData = existing?.data.value !== undefined

  const asyncData = hasExistingData && existing
    ? existing
    : await useAsyncData<ReleaseNote | null>(
        'latest-release',
        () => $fetch('/api/releases/latest', { headers: requestHeaders }),
        { default: () => null }
      )

  return asyncData
}
