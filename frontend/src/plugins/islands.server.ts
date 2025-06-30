import { defineNuxtPlugin } from '#app'

export default defineNuxtPlugin((nuxtApp) => {
  nuxtApp.vueApp.component('LazyHydrate', {
    setup(_, { slots }) {
      return () => slots.default?.({ hydrated: true })
    }
  })
})
