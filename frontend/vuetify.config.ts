import { defineVuetifyConfiguration } from 'vuetify-nuxt-module/custom-configuration'
import { aliases, mdi } from 'vuetify/iconsets/mdi-svg'
import { icons } from './app/config/icons'

export default defineVuetifyConfiguration(() => ({
  icons: {
    defaultSet: 'mdi',
    aliases: {
      ...aliases,
      ...icons,
    },
    sets: {
      mdi,
    },
  },
}))
