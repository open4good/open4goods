import { computed, defineComponent, h } from 'vue'
import type { JSXComponent, PropType } from 'vue'

import type { IconAliases, IconProps, IconSet, IconValue } from 'vuetify'
import { VComponentIcon, VSvgIcon } from 'vuetify/components'
import { aliases as mdiIconAliases } from 'vuetify/iconsets/mdi-svg'

import * as mdiIcons from '@mdi/js'

const toPascalCase = (value: string) =>
  value
    .split('-')
    .filter(Boolean)
    .map((segment) => segment[0]?.toUpperCase() + segment.slice(1))
    .join('')

const resolveMdiIcon = (icon: string): IconValue => {
  if (icon.startsWith('svg:')) {
    return icon.slice(4)
  }

  const cleaned = icon.replace(/^mdi:/, '')
  const directMatch = (mdiIcons as Record<string, string>)[cleaned]

  if (directMatch) {
    return directMatch
  }

  const normalized = cleaned.startsWith('mdi-') ? cleaned.slice(4) : cleaned
  const candidate = `mdi${toPascalCase(normalized)}`
  const resolvedIcon = (mdiIcons as Record<string, string>)[candidate]

  return resolvedIcon ?? icon
}

const resolveIconValue = (icon: IconValue) => {
  if (typeof icon === 'string') {
    return resolveMdiIcon(icon)
  }

  return icon
}

const MdiSvgIcon = defineComponent({
  name: 'Open4GoodsMdiSvgIcon',
  props: {
    icon: {
      type: [String, Array, Object, Function] as PropType<IconValue>,
      required: true,
    },
    tag: {
      type: [String, Object, Function] as PropType<IconProps['tag'] | JSXComponent>,
      default: 'svg',
    },
  },
  setup(props, { attrs }) {
    const resolvedIcon = computed(() => resolveIconValue(props.icon))

    return () => {
      const icon = resolvedIcon.value

      if (!icon) {
        return null
      }

      if (typeof icon === 'string' || Array.isArray(icon)) {
        return h(VSvgIcon, { ...attrs, icon, tag: props.tag })
      }

      return h(VComponentIcon, { ...attrs, icon, tag: props.tag })
    }
  },
})

export const mdiSvgAliases: IconAliases = {
  ...mdiIconAliases,
}

export const mdiSvg: IconSet = {
  component: MdiSvgIcon,
}
