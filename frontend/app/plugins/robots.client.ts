export default defineNuxtPlugin(() => {
  const config = useRuntimeConfig()

  if (config.public.appEnv !== 'production') {
    useHead({
      meta: [
        { name: 'robots', content: 'noindex, nofollow, noarchive' },
      ],
    })
  }
})
