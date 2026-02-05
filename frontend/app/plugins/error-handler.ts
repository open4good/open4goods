export default defineNuxtPlugin(nuxtApp => {
  nuxtApp.hook('vue:error', (error, instance, info) => {
    // Log error to console with clear formatting
    console.group('Global Vue Error Handler')
    console.error('Error:', error)
    console.error('Component Instance:', instance)
    console.error('Info:', info)
    console.groupEnd()

    // Ensure we trigger the Nuxt error page for better visibility if it's a fatal error
    // check if showError is available (it is a nuanced auto-import)
    if (process.client) {
      // We can optionally show a toast here if we had a toast system
    }
  })

  // Hook into Nuxt specific errors
  nuxtApp.hook('app:error', error => {
    console.group('Global Nuxt App Error Handler')
    console.error('Error:', error)
    console.groupEnd()
  })
})
