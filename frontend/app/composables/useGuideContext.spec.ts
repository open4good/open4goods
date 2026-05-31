import { mount } from '@vue/test-utils'
import { defineComponent, h } from 'vue'
import { describe, expect, it } from 'vitest'

import { provideGuideContext, useGuideContext } from './useGuideContext'

describe('useGuideContext', () => {
  it('provides category guide context to descendant widgets', () => {
    const Child = defineComponent({
      name: 'GuideContextChild',
      setup() {
        const context = useGuideContext()

        return () =>
          h('span', { 'data-test': 'vertical' }, context?.verticalId ?? '')
      },
    })

    const Parent = defineComponent({
      name: 'GuideContextParent',
      setup() {
        provideGuideContext({
          verticalId: 'tv',
          categorySlug: 'televiseurs',
          categoryPath: '/televiseurs',
          categoryTitle: 'Téléviseurs',
          heroImage: '/tv.jpg',
        })

        return () => h(Child)
      },
    })

    const wrapper = mount(Parent)

    expect(wrapper.get('[data-test="vertical"]').text()).toBe('tv')
  })
})
