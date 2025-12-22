import type { ReleaseNote } from '~~/types/releases'

export const useReleaseNotes = async () => {
  const existing = useNuxtData<ReleaseNote[]>('release-notes')
  const requestHeaders = useRequestHeaders(['host', 'x-forwarded-host'])

  const hasExistingData = existing?.data.value !== undefined

  const asyncData =
    hasExistingData && existing
      ? existing
      : await useAsyncData<ReleaseNote[]>(
          'release-notes',
          () => $fetch('/api/releases', { headers: requestHeaders }),
          { default: () => [] }
        )

  const releases = computed(() => asyncData.data.value ?? [])
  const latestRelease = computed(() => releases.value[0] ?? null)

  return {
    ...asyncData,
    releases,
    latestRelease,
  }
}
