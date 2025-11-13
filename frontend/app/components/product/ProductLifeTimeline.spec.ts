import { mountSuspended } from '@nuxt/test-utils/runtime'
import { describe, expect, it } from 'vitest'
import { createI18n } from 'vue-i18n'
import { defineComponent, h } from 'vue'
import {
  ProductTimelineEventSource,
  ProductTimelineEventType,
  type ProductTimelineDto,
} from '~~/shared/api-client'

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
            sources: {
              priceHistory: 'Price history',
              eprel: 'EPREL registry',
              generic: 'Timeline event',
            },
            conditions: {
              new: 'New',
              occasion: 'Second-hand',
              generic: 'Condition',
            },
            events: {
              priceFirstSeenNew: 'First new offer spotted',
              priceFirstSeenOccasion: 'First second-hand offer spotted',
              priceLastSeenNew: 'Latest new offer',
              priceLastSeenOccasion: 'Latest second-hand offer',
              eprelOnMarketStart: 'EPREL on-market start',
              eprelOnMarketEnd: 'EPREL on-market end',
              eprelOnMarketFirstStart: 'EPREL first entry',
              eprelFirstPublication: 'EPREL first publication',
              eprelLastPublication: 'EPREL last publication',
              eprelExport: 'EPREL export',
              eprelImported: 'Imported',
              eprelOrganisationClosed: 'Organisation closed',
              generic: 'Lifecycle event',
            },
          },
        },
      },
    },
  },
})

const VCardStub = defineComponent({
  name: 'VCardStub',
  setup(_, { slots }) {
    return () => h('div', { class: 'v-card-stub' }, slots.default?.())
  },
})

const VTimelineStub = defineComponent({
  name: 'VTimelineStub',
  setup(_, { slots }) {
    return () => h('div', { class: 'v-timeline-stub' }, slots.default?.())
  },
})

const VTimelineItemStub = defineComponent({
  name: 'VTimelineItemStub',
  setup(_, { slots }) {
    return () => h('div', { class: 'v-timeline-item-stub' }, [slots.opposite?.(), slots.default?.()])
  },
})

const VIconStub = defineComponent({
  name: 'VIconStub',
  props: { icon: { type: String, default: '' } },
  setup(props) {
    return () => h('i', { class: 'v-icon-stub', 'data-icon': props.icon }, props.icon)
  },
})

const buildTimeline = (): ProductTimelineDto => ({
  events: [
    {
      type: ProductTimelineEventType.PriceFirstSeenNew,
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

const mountComponent = async (timeline: ProductTimelineDto | null) => {
  const module = await import('./ProductLifeTimeline.vue')
  const Component = module.default

  return mountSuspended(Component, {
    props: { timeline },
    global: {
      plugins: [i18n],
      stubs: {
        VCard: VCardStub,
        VTimeline: VTimelineStub,
        VTimelineItem: VTimelineItemStub,
        VIcon: VIconStub,
      },
    },
  })
}

describe('ProductLifeTimeline', () => {
  it('renders timeline events with formatted dates', async () => {
    const wrapper = await mountComponent(buildTimeline())

    expect(wrapper.text()).toContain('First new offer spotted')
    expect(wrapper.text()).toContain('EPREL on-market start')
    expect(wrapper.text()).toContain('Jan 2024')
  })

  it('renders empty state when no events', async () => {
    const wrapper = await mountComponent({ events: [] })

    expect(wrapper.text()).toContain('Lifecycle information is not available yet.')
  })
})
