export default defineI18nConfig(() => ({
  legacy: false,
  locale: 'en-US',
  fallbackLocale: 'en-US',
  availableLocales: ['en-US', 'fr-FR'],
  missingWarn: false,
  fallbackWarn: false,
  warnHtmlMessage: false
}))
