import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import { mockNuxtImport } from '@nuxt/test-utils/runtime'
import { defineComponent } from 'vue'
import ImpactScoreMethodology from './ImpactScoreMethodology.vue'

mockNuxtImport('useLocalePath', () => () => (input: string) => input)
mockNuxtImport('useI18n', () => () => ({ t: (key: string) => key }))

const VChipStub = defineComponent({
  name: 'VChip',
  inheritAttrs: false,
  template: '<a v-bind="$attrs"><slot /></a>',
})

const VIconStub = defineComponent({
  name: 'VIcon',
  template: '<span />',
})

const globalConfig = {
  stubs: {
    VChip: VChipStub,
    VIcon: VIconStub,
  },
}

describe('ImpactScoreMethodology', () => {
  it('renders clickable capsules for each vertical', () => {
    const wrapper = mount(ImpactScoreMethodology, {
      props: {
        verticals: [
          {
            id: 'washing',
            verticalHomeTitle: 'Lave-linge',
            verticalHomeUrl: 'lave-linge',
            order: 1,
          },
          {
            id: 'tv',
            verticalHomeTitle: 'Téléviseurs',
            verticalHomeUrl: '/televiseurs',
            order: 2,
          },
        ],
      },
      global: globalConfig,
    })

    const chips = wrapper.findAll('.impact-score-methodology__chip')
    expect(chips).toHaveLength(2)
    expect(chips[0].text()).toContain('Lave-linge')
    expect(chips[1].text()).toContain('Téléviseurs')
  })

  it('shows an empty state when no verticals are available', () => {
    const wrapper = mount(ImpactScoreMethodology, {
      props: {
        verticals: [],
      },
      global: globalConfig,
    })

    expect(wrapper.text()).toContain('impactScorePage.sections.methodology.empty')
  })
})
