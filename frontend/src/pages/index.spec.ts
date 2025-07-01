import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import IndexPage from './index.vue'
describe('Index page', () => {
  it('renders welcome text', () => {
    const wrapper = mount(IndexPage, {
      global: {
        stubs: {
          NuxtLink: {
            template: '<a><slot /></a>',
            props: ['to']
          }
        }
      }
    })
    expect(wrapper.text()).toContain('Nudger')
  })

  it('links to blog page', () => {
    const wrapper = mount(IndexPage, {
      global: {
        stubs: {
          NuxtLink: {
            template: '<a><slot /></a>',
            props: ['to']
          }
        }
      }
    })
    const link = wrapper.get('a')
    expect(link.text()).toContain('Blog')
  })
})
