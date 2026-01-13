export const HOTJAR_RECORDING_COOKIE_NAME = 'hotjar_record'
export const HOTJAR_RECORDING_COOKIE_VALUE = 'true'
export const HOTJAR_RECORDING_COOKIE_MAX_AGE = 60 * 60 * 24 * 90

export const isHotjarRecordingCookieEnabled = (
  value: string | null | undefined
): boolean => value === HOTJAR_RECORDING_COOKIE_VALUE
