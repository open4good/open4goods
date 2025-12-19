import { defineAsyncComponent } from 'vue'

export default defineNuxtPlugin(nuxtApp => {
  nuxtApp.vueApp.component(
    'VueHcaptcha',
    defineAsyncComponent(async () => {
      const module = await import('@hcaptcha/vue3-hcaptcha')
      return module.default
    })
  )
})
