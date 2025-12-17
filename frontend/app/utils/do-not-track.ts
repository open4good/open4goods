export const isDoNotTrackEnabled = (): boolean => {
  if (typeof navigator === 'undefined' && typeof window === 'undefined') {
    return false
  }

  const navigatorDoNotTrack =
    typeof navigator !== 'undefined'
      ? navigator.doNotTrack ||
        (navigator as unknown as { msDoNotTrack?: string }).msDoNotTrack
      : undefined
  const windowDoNotTrack =
    typeof window !== 'undefined'
      ? (window as Window & { doNotTrack?: string }).doNotTrack
      : undefined
  const doNotTrackValue = navigatorDoNotTrack || windowDoNotTrack

  return doNotTrackValue === '1' || doNotTrackValue === 'yes'
}
