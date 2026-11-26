import { mountSuspended } from '@nuxt/test-utils/runtime'
import { describe, expect, it, vi } from 'vitest'
import ProductSummaryNavigation from './ProductSummaryNavigation.vue'

vi.mock('vuetify', () => ({
  useDisplay: () => ({
    mdAndDown: { value: false },
  }),
}))

describe('ProductSummaryNavigation', () => {
  const sections = [
    { id: 'hero', label: 'Overview', icon: 'mdi-information-outline' },
    { id: 'impact', label: 'Impact', icon: 'mdi-leaf' },
    { id: 'price', label: 'Price', icon: 'mdi-currency-eur' },
  ]

  const mountComponent = async (props = {}) =>
    await mountSuspended(ProductSummaryNavigation, {
      props: {
        sections,
        activeSection: 'impact',
        orientation: 'vertical',
        ariaLabel: 'Navigation',
        ...props,
      },
      global: {
        stubs: {
          'v-icon': {
            template: '<span class="v-icon"><slot /></span>',
          },
        },
      },
    })

  it('renders each section and highlights the active one', async () => {
    const wrapper = await mountComponent()

    const items = wrapper.findAll('.product-summary-navigation__link')
    expect(items).toHaveLength(sections.length)
    expect(items[1]?.classes()).toContain(
      'product-summary-navigation__link--active'
    )

    await wrapper.unmount()
  })

  it('emits navigate when a section is clicked', async () => {
    const wrapper = await mountComponent({ activeSection: 'hero' })

    const buttons = wrapper.findAll('button.product-summary-navigation__link')
    await buttons[2]?.trigger('click')

    expect(wrapper.emitted('navigate')).toBeTruthy()
    expect(wrapper.emitted('navigate')?.[0]).toEqual(['price'])

    await wrapper.unmount()
  })

  it('renders submenu entries and emits navigate', async () => {
    const wrapper = await mountComponent({
      sections: [
        {
          id: 'impact',
          label: 'Impact',
          icon: 'mdi-leaf',
          subsections: [
            { id: 'impact-energy', label: 'Energy' },
            { id: 'impact-repair', label: 'Repairability' },
          ],
        },
      ],
    })

    const toggle = wrapper.find('button.product-summary-navigation__toggle')
    await toggle.trigger('click')

    const submenuLinks = wrapper.findAll(
      'button.product-summary-navigation__submenu-link'
    )
    expect(submenuLinks).toHaveLength(2)

    await submenuLinks[1]?.trigger('click')

    const emitted = wrapper.emitted('navigate') ?? []
    expect(emitted[emitted.length - 1]).toEqual(['impact-repair'])

    await wrapper.unmount()
  })

  it('applies horizontal orientation modifier', async () => {
    const wrapper = await mountComponent({ orientation: 'horizontal' })

    expect(wrapper.find('.product-summary-navigation').classes()).toContain(
      'product-summary-navigation--horizontal'
    )

    await wrapper.unmount()
  })

  it('lets CSS media queries drive the layout by default', async () => {
    const wrapper = await mountComponent({ orientation: undefined })

    const classes = wrapper.find('.product-summary-navigation').classes()
    expect(classes).toContain('product-summary-navigation--responsive')
    expect(classes).not.toContain('product-summary-navigation--horizontal')
    expect(classes).not.toContain('product-summary-navigation--vertical')

    await wrapper.unmount()
  })

  it('only opens submenus on user action in the horizontal bar', async () => {
    const impactSection = {
      id: 'impact',
      label: 'Impact',
      icon: 'mdi-leaf',
      subsections: [{ id: 'impact-energy', label: 'Energy' }],
    }
    const isSubmenuShown = (
      wrapper: Awaited<ReturnType<typeof mountComponent>>
    ) =>
      (wrapper.find('#submenu-impact').element as HTMLElement).style.display !==
      'none'

    const vertical = await mountComponent({
      sections: [impactSection],
      activeSection: 'impact-energy',
    })
    expect(isSubmenuShown(vertical)).toBe(true)
    await vertical.unmount()

    const horizontal = await mountComponent({
      sections: [impactSection],
      activeSection: 'impact-energy',
      orientation: 'horizontal',
    })
    expect(isSubmenuShown(horizontal)).toBe(false)

    await horizontal.setProps({ activeSection: 'impact' })
    expect(isSubmenuShown(horizontal)).toBe(false)

    await horizontal
      .find('button.product-summary-navigation__toggle')
      .trigger('click')
    expect(isSubmenuShown(horizontal)).toBe(true)

    await horizontal.unmount()
  })

  it('emits collapse-change when toggling collapse state', async () => {
    const wrapper = await mountComponent()

    const toggle = wrapper.find(
      'button.product-summary-navigation__collapse-toggle'
    )
    await toggle.trigger('click')

    expect(wrapper.emitted('collapse-change')).toBeTruthy()
    expect(wrapper.emitted('collapse-change')?.[0]).toEqual([true])

    await toggle.trigger('click')
    expect(wrapper.emitted('collapse-change')?.[1]).toEqual([false])

    await wrapper.unmount()
  })

  it('renders admin panel and reacts to clicks', async () => {
    const adminSections = [
      { id: 'admin-json', label: 'Product JSON', icon: 'mdi-code-json' },
    ]

    const wrapper = await mountComponent({
      adminSections,
      adminTitle: 'Admin tools',
      adminHelper: 'Restricted zone',
    })

    expect(
      wrapper.find('.product-summary-navigation__admin-panel').exists()
    ).toBe(true)

    const adminButton = wrapper.find(
      'button.product-summary-navigation__admin-link'
    )
    await adminButton.trigger('click')

    const emitted = wrapper.emitted('navigate') ?? []
    expect(emitted[emitted.length - 1]).toEqual(['admin-json'])

    await wrapper.unmount()
  })
})
