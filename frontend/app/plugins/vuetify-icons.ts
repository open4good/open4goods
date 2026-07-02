import * as mdiPaths from '@mdi/js'
import { h } from 'vue'

type VuetifyIconProps = {
  icon?: unknown
  tag: string
}

type VuetifyOptionsWithIcons = {
  icons?: {
    defaultSet?: string
    sets?: Record<string, { component: (props: VuetifyIconProps) => unknown }>
  }
}

const mdiIconPaths = mdiPaths as Record<string, string>

const toMdiExportName = (iconName: string) =>
  `mdi${iconName
    .slice('mdi-'.length)
    .split('-')
    .map(part => part.charAt(0).toUpperCase() + part.slice(1))
    .join('')}`

const resolveMdiIconPath = (icon: unknown) => {
  if (typeof icon !== 'string' || !icon.startsWith('mdi-')) {
    return icon
  }

  return mdiIconPaths[toMdiExportName(icon)] ?? ''
}

const mdiSvgIconSet = {
  component: (props: VuetifyIconProps) =>
    h(props.tag, { style: null }, [
      h(
        'svg',
        {
          class: 'v-icon__svg',
          xmlns: 'http://www.w3.org/2000/svg',
          viewBox: '0 0 24 24',
          role: 'img',
          'aria-hidden': 'true',
        },
        [
          h('path', {
            d: resolveMdiIconPath(props.icon),
          }),
        ]
      ),
    ]),
}

export default defineNuxtPlugin({
  name: 'nudger:vuetify-icons',
  order: 20,
  setup(nuxtApp) {
    nuxtApp.hook('vuetify:before-create', ({ vuetifyOptions }) => {
      const options = vuetifyOptions as VuetifyOptionsWithIcons

      options.icons = {
        ...options.icons,
        defaultSet: 'mdi-svg',
        sets: {
          ...options.icons?.sets,
          'mdi-svg': mdiSvgIconSet,
        },
      }
    })
  },
})
