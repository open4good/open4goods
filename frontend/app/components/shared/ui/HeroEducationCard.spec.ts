import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import { defineComponent, h } from 'vue'
import HeroEducationCard from './HeroEducationCard.vue'

const stubs = {
  VCard: defineComponent({
    name: 'VCardStub',
    setup(_, { slots, attrs }) {
      return () =>
        h(
          'div',
          {
            ...attrs,
            'data-testid': 'hero-education-card',
          },
          slots.default?.(),
        )
    },
  }),
  VIcon: defineComponent({
    name: 'VIconStub',
    props: {
      icon: {
        type: String,
        required: false,
      },
      size: {
        type: [String, Number],
        default: undefined,
      },
    },
    setup(props) {
      return () =>
        h('span', {
          'data-testid': 'hero-education-card-icon',
          'data-icon': props.icon,
          'data-size': props.size,
        })
    },
  }),
  VDivider: defineComponent({
    name: 'VDividerStub',
    setup(_, { attrs }) {
      return () => h('hr', { ...attrs, 'data-testid': 'hero-education-card-divider' })
    },
  }),
}

describe('HeroEducationCard', () => {
  it('renders the title, icon and body HTML content', () => {
    const wrapper = mount(HeroEducationCard, {
      props: {
        icon: 'mdi-school-outline',
        title: 'What is open source?',
        bodyHtml: '<strong>Open4goods</strong> grows with everyone.',
      },
      global: {
        stubs,
      },
    })

    expect(wrapper.find('.hero-education-card__title').text()).toBe('What is open source?')
    expect(wrapper.find('[data-testid="hero-education-card-icon"]').attributes('data-icon')).toBe('mdi-school-outline')
    expect(wrapper.find('.hero-education-card__body').html()).toContain('<strong>Open4goods</strong>')
  })

  it('renders list items with their optional icons', () => {
    const wrapper = mount(HeroEducationCard, {
      props: {
        icon: 'mdi-school-outline',
        title: 'List example',
        items: [
          { icon: 'mdi-check', text: 'First item' },
          { text: 'Second item' },
        ],
      },
      global: {
        stubs,
      },
    })

    const listItems = wrapper.findAll('.hero-education-card__list li')
    expect(listItems).toHaveLength(2)
    expect(listItems[0].find('[data-testid="hero-education-card-icon"]').attributes('data-icon')).toBe('mdi-check')
    expect(listItems[1].find('[data-testid="hero-education-card-icon"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="hero-education-card-divider"]').exists()).toBe(true)
  })
})
