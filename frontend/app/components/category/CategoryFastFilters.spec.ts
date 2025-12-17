import { mount } from '@vue/test-utils'
import { afterAll, beforeAll, describe, expect, it, vi } from 'vitest'
import { defineComponent, h } from 'vue'
import type { VerticalSubsetDto } from '~~/shared/api-client'

vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    t: (key: string, params?: Record<string, unknown>) => {
      if (key === 'category.fastFilters.operator.greaterThan') {
        return `≥ ${params?.value ?? ''}`
      }

      if (key === 'category.fastFilters.operator.lowerThan') {
        return `≤ ${params?.value ?? ''}`
      }

      const translations: Record<string, string> = {
        'category.fastFilters.title': 'Quick filters',
        'category.fastFilters.reset': 'Clear fast filters',
        'category.fastFilters.groupDefault': 'Other quick filters',
        'category.fastFilters.groups.price': 'Price',
        'category.fastFilters.groups.screen_size': 'Screen size',
        'category.fastFilters.navigation.previous': 'Previous quick filters',
        'category.fastFilters.navigation.next': 'Next quick filters',
        'category.filters.toggle.show': 'Show filters column',
        'category.filters.toggle.hide': 'Hide filters column',
      }

      return translations[key] ?? key
    },
  }),
}))

const resolveClassList = (value: unknown): string => {
  if (!value) {
    return ''
  }

  if (typeof value === 'string') {
    return value
  }

  if (Array.isArray(value)) {
    return value
      .map(entry => resolveClassList(entry))
      .filter(Boolean)
      .join(' ')
  }

  if (typeof value === 'object') {
    return Object.entries(value as Record<string, unknown>)
      .filter(([, active]) => Boolean(active))
      .map(([name]) => name)
      .join(' ')
  }

  return String(value)
}

const VChipGroupStub = defineComponent({
  name: 'VChipGroup',
  props: {
    modelValue: { type: [String, Number, Array, Object], default: null },
  },
  emits: ['update:modelValue'],
  setup(props, { slots, attrs }) {
    const className = [
      'v-chip-group-stub',
      resolveClassList((attrs as Record<string, unknown>).class),
    ]
      .filter(Boolean)
      .join(' ')

    return () =>
      h(
        'div',
        {
          ...attrs,
          class: className,
          'data-model-value': Array.isArray(props.modelValue)
            ? JSON.stringify(props.modelValue)
            : (props.modelValue ?? ''),
        },
        slots.default?.()
      )
  },
})

const VChipStub = defineComponent({
  name: 'VChip',
  props: {
    value: { type: [String, Number], default: undefined },
    closable: { type: Boolean, default: false },
  },
  emits: ['click', 'click:close'],
  setup(props, { slots, emit, attrs }) {
    const className = [
      'v-chip-stub',
      resolveClassList((attrs as Record<string, unknown>).class),
    ]
      .filter(Boolean)
      .join(' ')

    const handleClick = (event: MouseEvent) => {
      emit('click', event)
    }

    const handleClose = (event: MouseEvent) => {
      emit('click:close', event)
    }

    return () =>
      h(
        'button',
        {
          ...attrs,
          class: className,
          type: 'button',
          'data-value': props.value ?? '',
          'data-closable': props.closable ? 'true' : 'false',
          onClick: handleClick,
        },
        [
          slots.default?.(),
          props.closable
            ? h(
                'span',
                {
                  class: 'v-chip-stub__close',
                  role: 'button',
                  onClick: (event: MouseEvent) => {
                    event.stopPropagation()
                    handleClose(event)
                  },
                },
                '×'
              )
            : null,
        ]
      )
  },
})

const VTooltipStub = defineComponent({
  name: 'VTooltip',
  props: {
    text: { type: String, default: '' },
    location: { type: String, default: 'bottom' },
  },
  setup(props, { slots, attrs }) {
    const className = [
      'v-tooltip-stub',
      resolveClassList((attrs as Record<string, unknown>).class),
    ]
      .filter(Boolean)
      .join(' ')

    return () =>
      h(
        'div',
        {
          ...attrs,
          class: className,
          'data-text': props.text,
          'data-location': props.location,
        },
        [slots.activator?.({ props: {} }), slots.default?.()]
      )
  },
})

const VBtnStub = defineComponent({
  name: 'VBtn',
  props: {
    type: { type: String, default: 'button' },
  },
  emits: ['click'],
  setup(props, { slots, emit, attrs }) {
    const className = [
      'v-btn-stub',
      resolveClassList((attrs as Record<string, unknown>).class),
    ]
      .filter(Boolean)
      .join(' ')

    return () =>
      h(
        'button',
        {
          ...attrs,
          class: className,
          type: props.type ?? 'button',
          onClick: (event: MouseEvent) => emit('click', event),
        },
        slots.default?.()
      )
  },
})

const VIconStub = defineComponent({
  name: 'VIcon',
  props: {
    icon: { type: String, default: '' },
  },
  setup(props, { slots, attrs }) {
    const className = [
      'v-icon-stub',
      resolveClassList((attrs as Record<string, unknown>).class),
    ]
      .filter(Boolean)
      .join(' ')

    return () =>
      h(
        'span',
        {
          ...attrs,
          class: className,
          'data-icon': props.icon,
        },
        slots.default?.()
      )
  },
})

const sampleSubsets: VerticalSubsetDto[] = [
  {
    id: 'price_lower_500',
    group: 'price',
    caption: '< 500 €',
    title: 'TV pas chères',
    description: 'Les TV dont le prix est inférieur à 500€.',
    criterias: [
      { field: 'price.minPrice.price', operator: 'LOWER_THAN', value: '500' },
      { field: 'price.minPrice.price', operator: 'GREATER_THAN', value: '0' },
    ],
  },
  {
    id: 'price_greater_1000',
    group: 'price',
    caption: '> 1000 €',
    title: 'TV haut de gamme',
    description: 'Les TV dont le prix est supérieur à 1000€.',
    criterias: [
      {
        field: 'price.minPrice.price',
        operator: 'GREATER_THAN',
        value: '1000',
      },
    ],
  },
  {
    id: 'small_screens',
    group: 'screen_size',
    caption: 'Petits écrans (< 32")',
    title: 'Petits écrans',
    description: "Les TV avec une taille d'écran inférieure à 32 pouces.",
    criterias: [
      {
        field: 'attributes.indexed.DIAGONALE_POUCES.numericValue',
        operator: 'LOWER_THAN',
        value: '32',
      },
    ],
  },
]

const mountComponent = async (
  overrides: Partial<{
    subsets: VerticalSubsetDto[]
    activeSubsetIds: string[]
  }> = {}
) => {
  const module = await import('./CategoryFastFilters.vue')
  const Component = module.default

  return mount(Component, {
    props: {
      subsets: sampleSubsets,
      activeSubsetIds: [],
      ...overrides,
    },
    global: {
      stubs: {
        VChipGroup: VChipGroupStub,
        VChip: VChipStub,
        VTooltip: VTooltipStub,
        VBtn: VBtnStub,
        VIcon: VIconStub,
      },
    },
  })
}

describe('CategoryFastFilters', () => {
  beforeAll(() => {
    class ResizeObserverStub {
      observe() {}
      unobserve() {}
      disconnect() {}
    }

    vi.stubGlobal('ResizeObserver', ResizeObserverStub)

    if (!HTMLElement.prototype.scrollTo) {
      Object.defineProperty(HTMLElement.prototype, 'scrollTo', {
        value: vi.fn(),
        writable: true,
      })
    }
  })

  afterAll(() => {
    vi.unstubAllGlobals()
  })

  it('renders quick filter groups with localized labels and tooltips', async () => {
    const wrapper = await mountComponent()

    const groups = wrapper.findAll('.category-fast-filters__group')
    expect(groups).toHaveLength(2)

    const groupLabels = groups.map(node => node.attributes('aria-label'))
    expect(groupLabels).toEqual(['Price', 'Screen size'])

    const tooltipNodes = wrapper.findAll('.v-tooltip-stub')
    expect(tooltipNodes).toHaveLength(sampleSubsets.length)
    const firstTooltip = tooltipNodes[0]
    expect(firstTooltip).toBeDefined()
    const expectedDescription = sampleSubsets[0]?.description ?? ''
    expect(firstTooltip!.attributes('data-text')).toBe(expectedDescription)
  })

  it('emits toggle events when selecting and clearing quick filters within a group', async () => {
    const wrapper = await mountComponent()
    const chipGroups = wrapper.findAllComponents(VChipGroupStub)
    expect(chipGroups).toHaveLength(2)

    const priceGroup = chipGroups[0]
    expect(priceGroup).toBeDefined()
    priceGroup!.vm.$emit('update:modelValue', 'price_lower_500')

    expect(wrapper.emitted('toggle-subset')).toContainEqual([
      'price_lower_500',
      true,
    ])

    await wrapper.setProps({ activeSubsetIds: ['price_lower_500'] })

    priceGroup!.vm.$emit('update:modelValue', 'price_greater_1000')
    const emitted = wrapper.emitted('toggle-subset') ?? []
    expect(emitted).toContainEqual(['price_greater_1000', true])

    await wrapper.setProps({ activeSubsetIds: ['price_greater_1000'] })

    priceGroup!.vm.$emit('update:modelValue', null)
    const finalEvents = wrapper.emitted('toggle-subset') ?? []
    expect(finalEvents).toContainEqual(['price_greater_1000', false])
  })

  it('keeps active quick filter chips visible without a close affordance', async () => {
    const wrapper = await mountComponent({ activeSubsetIds: ['small_screens'] })

    const chip = wrapper.find('.v-chip-stub[data-value="small_screens"]')
    expect(chip.exists()).toBe(true)
    expect(chip.attributes('data-closable')).toBe('false')
  })

  it('enables horizontal navigation when quick filters overflow', async () => {
    const wrapper = await mountComponent()
    const scroller = wrapper.find('.category-fast-filters__scroller')
    expect(scroller.exists()).toBe(true)

    const element = scroller.element as HTMLElement & {
      scrollTo: (options: ScrollToOptions) => void
    }
    const scrollToSpy = vi.fn()
    let currentScrollLeft = 0

    Object.defineProperty(element, 'clientWidth', {
      configurable: true,
      value: 200,
    })
    Object.defineProperty(element, 'scrollWidth', {
      configurable: true,
      value: 500,
    })
    Object.defineProperty(element, 'scrollLeft', {
      configurable: true,
      get: () => currentScrollLeft,
      set: value => {
        currentScrollLeft = value
      },
    })
    element.scrollTo = scrollToSpy

    window.dispatchEvent(new Event('resize'))
    await wrapper.vm.$nextTick()

    const nextButton = wrapper.find(
      '[data-testid="category-fast-filters-next"]'
    )
    expect(nextButton.exists()).toBe(true)

    await nextButton.trigger('click')
    expect(scrollToSpy).toHaveBeenCalled()

    currentScrollLeft = 250
    element.dispatchEvent(new Event('scroll'))
    await wrapper.vm.$nextTick()

    const prevButton = wrapper.find(
      '[data-testid="category-fast-filters-prev"]'
    )
    expect(prevButton.exists()).toBe(true)
  })
})
