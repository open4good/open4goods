export const CSRF_COOKIE_NAME = 'csrf_token'
export const CSRF_HEADER_NAME = 'x-csrf-token'

const SAFE_METHODS = new Set(['GET', 'HEAD', 'OPTIONS'])

export const isSafeMethod = (method: string) =>
  SAFE_METHODS.has(method.toUpperCase())
