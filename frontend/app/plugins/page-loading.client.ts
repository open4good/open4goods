export default defineNuxtPlugin(nuxtApp => {
  if (!import.meta.client) {
    return
  }

  const routeLoading = useState('routeLoading', () => false)

  nuxtApp.hooks.hook('page:loading:start', () => {
    routeLoading.value = true
  })

  const stopLoading = () => {
    routeLoading.value = false
  }

  nuxtApp.hooks.hook('page:loading:end', stopLoading)
  nuxtApp.hooks.hook(
    'page:error' as Parameters<typeof nuxtApp.hooks.hook>[0],
    () => {
      stopLoading()
    }
  )
})
