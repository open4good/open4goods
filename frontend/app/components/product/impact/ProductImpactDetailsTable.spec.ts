import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import { createI18n } from 'vue-i18n'
import { computed, defineComponent, h, nextTick } from 'vue'
import ProductImpactDetailsTable from './ProductImpactDetailsTable.vue'
import type { ScoreView } from './impact-types'

const i18n = createI18n({
  legacy: false,
  locale: 'en-US',
  messages: {
    'en-US': {
      product: {
        impact: {
          detailsTitle: 'Impact details',
          noDetailsAvailable: 'No details',
          hideDetails: 'Hide details',
          subscoreDetailsToggle: 'Show details',
          showVirtualScores: 'Show virtual scores',
          valueOutOf: '{value} / {max}',
          tableHeaders: {
            scoreName: 'Indicator',
            attributeValue: 'Raw value',
            scoreValue: 'Score',
            coefficient: 'Coefficient',
            lifecycle: 'Lifecycle',
            scoreOn20: 'Score / 20',
          },
          lifecycle: {
            END_OF_LIFE: 'End of life',
            EXTRACTION: 'Extraction',
            MANUFACTURING: 'Manufacturing',
            TRANSPORTATION: 'Transportation',
            USE: 'Use',
          },
          aggregateScores: {
            DIVERS: 'Miscellaneous',
            AGG1: 'Aggregate 1',
            AGG2: 'Aggregate 2',
          },
        },
      },
    },
  },
})

const VDataTableStub = defineComponent({
  name: 'VDataTableStub',
  props: [
    'headers',
    'items',
    'itemsPerPage',
    'density',
    'class',
    'hideDefaultFooter',
  ],
  setup(props, { slots }) {
    const items = computed(
      () => (props.items as Array<{ rowType?: string }> | undefined) ?? []
    )

    return () =>
      h(
        'div',
        { class: 'v-data-table-stub' },
        items.value.map(item =>
          h('div', { class: `row row-${item.rowType ?? 'unknown'}` }, [
            slots['item.label']?.({ item }),
            slots['item.attributeValue']?.({ item }),
            slots['item.lifecycle']?.({ item }),
          ])
        ) ?? []
      )
  },
})

const VBtnStub = defineComponent({
  name: 'VBtnStub',
  emits: ['click'],
  setup(_props, { emit, slots }) {
    return () =>
      h(
        'button',
        {
          class: 'v-btn-stub',
          onClick: () => emit('click'),
        },
        slots.default?.()
      )
  },
})

const VIconStub = defineComponent({
  name: 'VIconStub',
  props: ['icon', 'size'],
  setup(props) {
    return () => h('span', { class: 'v-icon-stub', 'data-icon': props.icon })
  },
})

const VChipStub = defineComponent({
  name: 'VChipStub',
  props: ['color', 'size', 'variant'],
  setup(_props, { slots }) {
    return () => h('span', { class: 'v-chip-stub' }, slots.default?.())
  },
})

const ProductImpactSubscoreRatingStub = defineComponent({
  name: 'ProductImpactSubscoreRatingStub',
  props: ['score', 'max', 'size', 'showValue'],
  setup(props) {
    return () =>
      h(
        'span',
        { class: 'subscore-rating-stub', 'data-score': props.score },
        String(props.score ?? '')
      )
  },
})

const ImpactCoefficientBadgeStub = defineComponent({
  name: 'ImpactCoefficientBadgeStub',
  props: ['value', 'tooltipParams'],
  setup(props) {
    return () =>
      h(
        'span',
        { class: 'impact-coefficient-stub' },
        String(props.value ?? '—')
      )
  },
})

const ProductAttributeSourcingLabelStub = defineComponent({
  name: 'ProductAttributeSourcingLabelStub',
  props: ['value', 'sourcing'],
  setup(props) {
    return () =>
      h(
        'span',
        { class: 'product-attribute-sourcing-stub' },
        String(props.value ?? '')
      )
  },
})

const mountComponent = (scores: ScoreView[]) =>
  mount(ProductImpactDetailsTable, {
    props: { scores },
    global: {
      plugins: [i18n],
      stubs: {
        'v-data-table': VDataTableStub,
        'v-btn': VBtnStub,
        'v-icon': VIconStub,
        'v-chip': VChipStub,
        ProductImpactSubscoreRating: ProductImpactSubscoreRatingStub,
        ImpactCoefficientBadge: ImpactCoefficientBadgeStub,
        ProductAttributeSourcingLabel: ProductAttributeSourcingLabelStub,
      },
    },
  })

describe('ProductImpactDetailsTable', () => {
  it('groups orphan scores into the DIVERS aggregate and keeps groups collapsed', () => {
    const scores: ScoreView[] = [
      {
        id: 'AGG1',
        label: 'Aggregate 1',
        relativeValue: 4.5,
        participateInScores: [],
      },
      {
        id: 'SUB1',
        label: 'Subscore 1',
        relativeValue: 3.2,
        participateInScores: ['AGG1'],
        participateInACV: ['USE'],
      },
      {
        id: 'ORPHAN',
        label: 'Orphan score',
        relativeValue: 2.4,
        participateInScores: [],
      },
    ]

    const wrapper = mountComponent(scores)
    const vm = wrapper.vm as unknown as {
      tableItems: Array<{
        id: string
        rowType: string
        displayValue: number | null
      }>
      expandedGroups: Set<string>
    }

    expect(vm.tableItems).toHaveLength(4)
    const diversRow = vm.tableItems.find(row => row.id === 'DIVERS')
    expect(diversRow?.rowType).toBe('aggregate')
    expect(diversRow?.displayValue).toBeCloseTo(2.4)
    expect(vm.expandedGroups.size).toBe(2)
  })

  it('duplicates scores across aggregates and computes aggregate values from children when missing', async () => {
    const scores: ScoreView[] = [
      {
        id: 'CHILD',
        label: 'Child score',
        relativeValue: 3,
        participateInScores: ['AGG1', 'AGG2'],
        aggregates: { AGG2: 4.6 },
      },
    ]

    const wrapper = mountComponent(scores)
    const vm = wrapper.vm as unknown as {
      tableItems: Array<{
        id: string
        rowType: string
        displayValue: number | null
        parentId?: string
      }>
      toggleGroup: (id: string) => void
    }

    const agg1 = vm.tableItems.find(row => row.id === 'AGG1')
    const agg2 = vm.tableItems.find(row => row.id === 'AGG2')

    expect(agg1?.displayValue).toBeCloseTo(3)
    expect(agg2?.displayValue).toBeCloseTo(4.6)

    // Groups are expanded by default
    await nextTick()

    const childRows = vm.tableItems.filter(row => row.rowType === 'subscore')
    expect(childRows).toHaveLength(2)
    expect(childRows.map(row => row.parentId)).toContain('AGG1')
    expect(childRows.map(row => row.parentId)).toContain('AGG2')
    expect(vm.tableItems.find(row => row.id === 'DIVERS')).toBeUndefined()
  })

  it('correctly passes the virtual property to table rows', async () => {
    const scores: ScoreView[] = [
      {
        id: 'REAL',
        label: 'Real score',
        value: 1,
        relativeValue: 1,
        virtual: false,
      },
      {
        id: 'VIRTUAL',
        label: 'Virtual score',
        value: 2,
        relativeValue: 2,
        virtual: true,
      },
    ]

    const wrapper = mountComponent(scores)
    const vm = wrapper.vm as unknown as {
      tableItems: Array<{
        id: string
        virtual?: boolean
      }>
      toggleGroup: (id: string) => void
    }

    // DIVERS group is expanded by default
    await nextTick()

    // Toggle virtual scores to make sure they are included in tableItems
    // Assuming there is a showVirtualScores ref or similar mechanism that filters them out by default
    // We can simulate the toggle click or change the ref if exposed, but since we are testing VM computed logic which relies on props/state:
    // The component filters virtual scores out by default. We need to enable them.
    // Looking at the component, there is a v-btn that toggles `showVirtualScores`.
    // We can interact with the DOM or directly with the component instance if available.
    // Let's try finding the toggle button and clicking it.

    // We need to look for the toggle button. It has class 'impact-details__virtual-toggle'.
    const toggleBtn = wrapper.find('.impact-details__virtual-toggle')
    if (toggleBtn.exists()) {
      await toggleBtn.trigger('click')
    }

    const realRow = vm.tableItems.find(row => row.id === 'REAL')
    const virtualRow = vm.tableItems.find(row => row.id === 'VIRTUAL')

    expect(realRow).toBeDefined()
    expect(virtualRow).toBeDefined()
    expect(realRow?.virtual).toBeFalsy()
    expect(virtualRow?.virtual).toBe(true)
  })
})
