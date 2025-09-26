export default defineEventHandler((event) => {
  const config = useRuntimeConfig(event)
  const isProduction = config.public.appEnv === 'production'

  setHeader(event, 'Content-Type', 'text/plain; charset=utf-8')

  if (!isProduction) {
    return ['User-agent: *', 'Disallow: /'].join('\n')
  }

  const normalizedSiteUrl = (config.public.siteUrl || '').replace(/\/$/, '')

  return [
    'User-agent: *',
    'Allow: /',
    `Sitemap: ${normalizedSiteUrl}/sitemap.xml`,
  ].join('\n')
})
