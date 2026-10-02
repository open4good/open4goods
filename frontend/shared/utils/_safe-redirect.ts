/**
 * Guards against open redirects: only same-origin, root-relative paths are
 * accepted (rejects absolute URLs and protocol-relative `//host` targets).
 */
export const isSafeRedirectTarget = (target: unknown): target is string =>
  typeof target === 'string' &&
  target.startsWith('/') &&
  !target.startsWith('//')
