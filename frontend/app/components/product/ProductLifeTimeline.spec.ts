import { mountSuspended } from '@nuxt/test-utils/runtime'
import { describe, expect, it } from 'vitest'
import { createI18n } from 'vue-i18n'
import { defineComponent, h } from 'vue'
import { ProductTimelineEventSource, ProductTimelineEventType, type ProductTimelineDto } from '~~/shared/api-client'

const i18n = createI18n({
  legacy: false,
  locale: 'en',
  messages: {
    en: {
      product: {
        attributes: {
          timeline: {
            title: 'Lifecycle',
            subtitle: 'Milestones',
            empty: 'Lifecycle information is not available yet.',
            ariaYear: 'Year {year}',
            sources: {
              priceHistory: 'Nudger',
              eprel: 'EPREL registry',
              generic: 'Timeline event',
            },
            tooltip: {
              date: 'Date',
              source: 'Source',
              ariaLabel: '{label} on {date} · {source}',
            },
            events: {
              priceFirstSeenNew: 'First new offer',
              priceFirstSeenOccasion: 'First second-hand offer',
              priceLastSeenNew: 'Latest new offer',
              priceLastSeenOccasion: 'Latest second-hand offer',
              eprelOnMarketStart: 'Market start',
              eprelOnMarketEnd: 'Market end',
              eprelOnMarketFirstStart: 'EPREL first entry',
              eprelFirstPublication: 'EPREL first publication',
              eprelLastPublication: 'EPREL last publication',
              eprelExport: 'EPREL export',
              eprelImported: 'Imported',
              eprelOrganisationClosed: 'Organisation closed',
              generic: 'Lifecycle event',
            },
            descriptions: {
              priceFirstSeenNew: 'First new offer detected.',
              priceFirstSeenOccasion: 'First second-hand offer detected.',
              priceLastSeenNew: 'Most recent new offer recorded.',
              priceLastSeenOccasion: 'Most recent second-hand offer recorded.',
              eprelOnMarketStart: 'EPREL start date.',
              eprelOnMarketEnd: 'EPREL end date.',
              eprelOnMarketFirstStart: 'EPREL first entry.',
              eprelFirstPublication: 'EPREL first publication.',
              eprelLastPublication: 'EPREL last publication.',
              eprelExport: 'EPREL export timestamp.',
              eprelImported: 'EPREL import timestamp.',
              eprelOrganisationClosed: 'Organisation closed.',
              generic: 'Lifecycle event.',
            },
          },
        },
      },
    },
  },
})

const VCardStub = defineComponent({
  name: 'VCardStub',
  setup(_, { slots, attrs }) {
    return () => h('div', { ...attrs, class: ['v-card-stub', attrs.class] }, slots.default?.())
  },
})

const VIconStub = defineComponent({
  name: 'VIconStub',
  props: { icon: { type: String, default: '' } },
  setup(props) {
    return () => h('i', { class: 'v-icon-stub', 'data-icon': props.icon }, props.icon)
  },
})

const VTooltipStub = defineComponent({
  name: 'VTooltipStub',
  props: { text: { type: String, default: '' } },
  setup(props, { slots }) {
    return () =>
      h('div', { class: 'v-tooltip-stub', 'data-text': props.text }, [
        slots.activator?.({ props: {} }),
        slots.default?.(),
      ])
  },
})

const buildTimeline = (): ProductTimelineDto => ({
  events: [
    {
      type: ProductTimelineEventType.PriceFirstSeenNew,
      source: ProductTimelineEventSource.PriceHistory,
      timestamp: Date.UTC(2023, 8, 10),
    },
    {
      type: ProductTimelineEventType.PriceFirstSeenOccasion,
      source: ProductTimelineEventSource.PriceHistory,
      timestamp: Date.UTC(2024, 0, 5),
    },
    {
      type: ProductTimelineEventType.EprelOnMarketStart,
      source: ProductTimelineEventSource.Eprel,
      timestamp: Date.UTC(2024, 5, 1),
    },
  ],
})

const mountComponent = async (
  timeline: ProductTimelineDto | null,
  extraProps: Partial<{ layout: 'vertical' | 'horizontal'; alternate: boolean }> = {},
) => {
  const module = await import('./ProductLifeTimeline.vue')
  const Component = module.default

  return mountSuspended(Component, {
    props: { timeline, ...extraProps },
    global: {
      plugins: [i18n],
      stubs: {
        VCard: VCardStub,
        VIcon: VIconStub,
        VTooltip: VTooltipStub,
      },
    },
  })
}

describe('ProductLifeTimeline', () => {
  it('renders timeline events with compact month labels and tooltips', async () => {
    const wrapper = await mountComponent(buildTimeline())

    const monthLabels = wrapper.findAll('.product-life-timeline__event-month').map((node) => node.text())
    const titles = wrapper.findAll('.product-life-timeline__event-title').map((node) => node.text())

    expect(monthLabels).toEqual(['Sep', 'Jan', 'Jun'])
    expect(titles).toContain('Market start')
    expect(wrapper.text()).toContain('First new offer')
    expect(wrapper.text()).toContain('Market start')
    expect(wrapper.find('.product-life-timeline--horizontal').exists()).toBe(true)
  })

  it('renders empty state when no events', async () => {
    const wrapper = await mountComponent({ events: [] })

    expect(wrapper.text()).toContain('Lifecycle information is not available yet.')
  })

  it('supports vertical layout option', async () => {
    const wrapper = await mountComponent(buildTimeline(), { layout: 'vertical', alternate: true })

    expect(wrapper.find('.product-life-timeline--vertical').exists()).toBe(true)
  })
})
