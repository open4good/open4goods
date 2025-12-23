/**
 * Security headers middleware
 * Adds essential security headers to all responses
 */
export default defineEventHandler(event => {
  const isProduction = process.env.NODE_ENV === 'production'

  // Content Security Policy
  const cspDirectives = {
    'default-src': ["'self'"],
    'script-src': [
      "'self'",
      "'unsafe-inline'", // Required for Vue hydration
      "'unsafe-eval'", // Required for dev mode
      'https://plausible.nudger.fr',
      'https://hcaptcha.com',
      'https://*.hcaptcha.com',
      'https://cdn.jsdelivr.net',
      'https://unpkg.com',
    ],
    'style-src': [
      "'self'",
      "'unsafe-inline'", // Required for Vuetify
      'https://fonts.googleapis.com',
      'https://cdn.jsdelivr.net',
      'https://unpkg.com',
    ],
    'img-src': [
      "'self'",
      'data:',
      'blob:',
      'https:',
      'https://static.nudger.fr',
      'https://plausible.nudger.fr',
    ],
    'font-src': [
      "'self'",
      'data:',
      'https://fonts.gstatic.com',
      'https://cdn.jsdelivr.net',
    ],
    'connect-src': [
      "'self'",
      'https://beta.front-api.nudger.fr',
      'https://front-api.nudger.fr',
      'https://plausible.nudger.fr',
      'https://hcaptcha.com',
      'https://*.hcaptcha.com',
    ],
    'frame-src': ["'self'", 'https://hcaptcha.com', 'https://*.hcaptcha.com'],
    'worker-src': ["'self'", 'blob:'],
    'manifest-src': ["'self'"],
  }

  const csp = Object.entries(cspDirectives)
    .map(([key, values]) => `${key} ${values.join(' ')}`)
    .join('; ')

  setResponseHeader(event, 'Content-Security-Policy', csp)

  // Prevent MIME type sniffing
  setResponseHeader(event, 'X-Content-Type-Options', 'nosniff')

  // Prevent clickjacking
  setResponseHeader(event, 'X-Frame-Options', 'SAMEORIGIN')

  // XSS Protection (legacy but still useful for older browsers)
  setResponseHeader(event, 'X-XSS-Protection', '1; mode=block')

  // Referrer Policy
  setResponseHeader(event, 'Referrer-Policy', 'strict-origin-when-cross-origin')

  // HTTP Strict Transport Security (HSTS) - only in production
  if (isProduction) {
    setResponseHeader(
      event,
      'Strict-Transport-Security',
      'max-age=31536000; includeSubDomains; preload'
    )
  }

  // Permissions Policy (formerly Feature Policy)
  setResponseHeader(
    event,
    'Permissions-Policy',
    'camera=(), microphone=(), geolocation=(self)'
  )
})
