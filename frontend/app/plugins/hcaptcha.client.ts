import VueHcaptcha from '@hcaptcha/vue3-hcaptcha'

export default defineNuxtPlugin(nuxtApp => {
  nuxtApp.vueApp.component('VueHcaptcha', VueHcaptcha)
})
