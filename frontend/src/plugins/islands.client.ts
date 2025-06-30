import { defineNuxtPlugin } from '#app'
import LazyHydrate from 'vue-lazy-hydration'

export default defineNuxtPlugin((nuxtApp) => {
  nuxtApp.vueApp.component('LazyHydrate', LazyHydrate)
})
