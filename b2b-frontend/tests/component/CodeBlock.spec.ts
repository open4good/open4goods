import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import B2bCodeBlock from '~/components/B2bCodeBlock.vue'

const globalStubs = {
  'v-sheet': { template: '<div class="v-sheet"><slot /></div>' },
  'v-btn': { template: '<button><slot /></button>' },
}

describe('B2bCodeBlock', () => {
  it('renders the provided code content in a pre element', () => {
    const code = 'GET /api/v1/products/123/price'
    const wrapper = mount(B2bCodeBlock, {
      props: { code, language: 'http' },
      global: { stubs: globalStubs }
    })
    expect(wrapper.html()).toContain(code)
    expect(wrapper.find('pre').exists()).toBe(true)
  })

  it('shows the language label', () => {
    const wrapper = mount(B2bCodeBlock, {
      props: { code: 'const x = 1;', language: 'javascript' },
      global: { stubs: globalStubs }
    })
    expect(wrapper.html()).toContain('javascript')
  })
})
