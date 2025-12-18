import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import OpensourceContributionSection from './OpensourceContributionSection.vue'

describe('OpensourceContributionSection', () => {
  const defaultProps = {
    eyebrow: 'Contribution',
    title: 'How to contribute',
    descriptionBlocId: 'webpages:opensource:contribute-intro',
    steps: [
      {
        title: 'Step 1',
        descriptionBlocId: 'webpages:opensource:step-1',
        icon: 'mdi-test'
      },
      {
        title: 'Step 2',
        descriptionBlocId: 'webpages:opensource:step-2',
        icon: 'mdi-test-2'
      }
    ]
  }

  it('renders correctly with default props', () => {
    const wrapper = mount(OpensourceContributionSection, {
      props: defaultProps,
      global: {
        stubs: {
          TextContent: true,
          VIcon: true,
          VContainer: { template: '<div><slot /></div>' },
          VChip: true
        }
      }
    })

    expect(wrapper.find('section').exists()).toBe(true)
    expect(wrapper.find('h2').text()).toBe(defaultProps.title)
    expect(wrapper.findAll('.step-card').length).toBe(2)
  })

  it('renders list with correct semantic structure (ul/li)', () => {
    const wrapper = mount(OpensourceContributionSection, {
      props: defaultProps,
      global: {
        stubs: {
          TextContent: true,
          VIcon: true,
          VContainer: { template: '<div><slot /></div>' },
          VChip: true
        }
      }
    })

    const list = wrapper.find('ul.steps-grid')
    expect(list.exists()).toBe(true)
    
    const items = list.findAll('li.step-card')
    expect(items.length).toBe(2)
  })
})
