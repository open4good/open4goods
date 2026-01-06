import { mount } from '@vue/test-utils'
import { describe, expect, it, vi, beforeAll } from 'vitest'

const messages: Record<string, string> = {
  'pwa.landing.actionBar.ariaLabel': 'Mobile quick actions',
  'pwa.landing.actions.scan.title': 'Scan',
  'pwa.landing.actions.wizard.title': 'Wizard',
  'pwa.landing.actions.search.title': 'Search',
  'pwa.landing.actions.share.title': 'Share',
}

vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    t: (key: string) => messages[key] ?? key,
  }),
}))

type ActionBarComponent = (typeof import('./PwaMobileActionBar.vue'))['default']
let PwaMobileActionBar: ActionBarComponent

describe('PwaMobileActionBar', () => {
  beforeAll(async () => {
    PwaMobileActionBar = (await import('./PwaMobileActionBar.vue')).default
  })

  it('emits actions on button click', async () => {
    const wrapper = mount(PwaMobileActionBar, {
      global: {
        stubs: {
          VSheet: {
            template: '<div><slot /></div>',
          },
          VContainer: {
            template: '<div><slot /></div>',
          },
          VBtn: {
            props: ['prependIcon'],
            emits: ['click'],
            template:
              '<button class="v-btn-stub" @click="$emit(\'click\')"><slot /></button>',
          },
        },
      },
    })

    const buttons = wrapper.findAll('.v-btn-stub')
    expect(buttons).toHaveLength(3)

    await buttons[0].trigger('click')
    await buttons[1].trigger('click')
    await buttons[2].trigger('click')

    expect(wrapper.emitted('scan')).toBeTruthy()
    expect(wrapper.emitted('wizard')).toBeTruthy()
    expect(wrapper.emitted('search')).toBeTruthy()
  })
})
