export const ShareResolutionStatus = {
  Pending: 'PENDING',
  Resolved: 'RESOLVED',
  Timeout: 'TIMEOUT',
  Error: 'ERROR',
} as const

export type ShareResolutionStatus =
  (typeof ShareResolutionStatus)[keyof typeof ShareResolutionStatus]
