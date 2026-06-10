import { mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { describe, expect, it, vi } from 'vitest'
import { ref } from 'vue'

import SectionReveal from './SectionReveal.vue'

vi.mock('@vueuse/core', () => ({
  usePreferredReducedMotion: () => ref('reduce'),
  useStorage: (_key: string, defaultValue: boolean) => ref(defaultValue),
}))

describe('SectionReveal', () => {
  it('does not force centered text or alignment classes on slot content', () => {
    const wrapper = mount(SectionReveal, {
      slots: {
        default: '<p>Editorial content</p>',
      },
      global: {
        plugins: [createPinia()],
        stubs: {
          VFadeTransition: {
            template: '<div><slot /></div>',
          },
        },
        directives: {
          intersect: vi.fn(),
        },
      },
    })

    const content = wrapper.get('.section-reveal__content')

    expect(content.classes()).not.toContain('text-center')
    expect(content.classes()).not.toContain('align-center')
  })
})
