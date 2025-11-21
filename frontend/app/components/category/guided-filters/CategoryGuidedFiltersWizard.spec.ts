import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import { defineComponent, h, nextTick } from 'vue'
import type { ModalFilterWizardDto } from '~~/shared/api-client'

vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    t: (key: string, params?: Record<string, unknown>) => {
      const map: Record<string, string> = {
        'category.guidedFilters.progressLabel': `Step ${params?.current ?? ''} of ${params?.total ?? ''}`,
        'category.guidedFilters.stepTitle': `Step ${params?.current ?? ''}/${params?.total ?? ''}`,
        'category.guidedFilters.close': 'Close',
        'category.guidedFilters.skip': 'Skip',
        'category.guidedFilters.previous': 'Previous',
        'category.guidedFilters.next': 'Next',
        'category.guidedFilters.apply': 'Apply filters',
        'category.guidedFilters.resultsLoading': 'Loading…',
        'category.guidedFilters.resultsLabel': `${params?.count ?? ''} products match in ${params?.category ?? ''}`,
        'category.guidedFilters.resultsFallback': 'Select an option to preview results.',
        'category.guidedFilters.defaultCategory': 'this category',
      }

      return map[key] ?? key
    },
    n: (value: number) => value.toString(),
  }),
}))

const createSimpleStub = (tag: string) =>
  defineComponent({
    name: `${tag}-stub`,
    setup(_, { slots, attrs }) {
      return () => h(tag, attrs, slots.default ? slots.default() : [])
    },
  })

const VDialogStub = defineComponent({
  name: 'VDialogStub',
  props: {
    modelValue: { type: Boolean, default: false },
  },
  emits: ['update:modelValue'],
  setup(props, { slots }) {
    return () => (props.modelValue ? h('div', { class: 'v-dialog-stub' }, slots.default?.()) : null)
  },
})

const VBtnStub = defineComponent({
  name: 'VBtnStub',
  props: {
    type: { type: String, default: 'button' },
  },
  emits: ['click'],
  setup(props, { slots, emit, attrs }) {
    return () =>
      h(
        'button',
        {
          ...attrs,
          type: props.type,
          class: ['v-btn-stub', attrs?.class].filter(Boolean).join(' '),
          onClick: (event: MouseEvent) => emit('click', event),
        },
        slots.default?.(),
      )
  },
})

const VSheetStub = defineComponent({
  name: 'VSheetStub',
  props: {
    tag: { type: String, default: 'div' },
    disabled: { type: Boolean, default: false },
  },
  setup(props, { slots, attrs }) {
    return () =>
      h(
        props.tag === 'button' ? 'button' : props.tag,
        {
          ...attrs,
          disabled: props.disabled,
          type: props.tag === 'button' ? 'button' : undefined,
        },
        slots.default?.(),
      )
  },
})

const wizardFixture: ModalFilterWizardDto = {
  questions: [
    {
      id: 'usage',
      title: 'Main usage',
      subtitle: 'How will you use it?',
      description: 'Tell us more about your expectations.',
      selectionType: 'SINGLE',
      choices: [
        {
          id: 'movies',
          title: 'Movie nights',
          expression: {
            operator: 'AND',
            clauses: [
              { field: 'screenType', operator: 'TERM', terms: ['OLED'] },
            ],
          },
        },
        {
          id: 'sports',
          title: 'Sports',
          expression: {
            operator: 'AND',
            clauses: [
              { field: 'refreshRate', operator: 'RANGE', min: 120 },
            ],
          },
        },
      ],
    },
    {
      id: 'size',
      title: 'Preferred size',
      selectionType: 'SINGLE',
      choices: [
        {
          id: 'medium',
          title: 'Between 50" and 60"',
          expression: {
            operator: 'AND',
            clauses: [
              { field: 'screenSize', operator: 'RANGE', min: 50, max: 60 },
            ],
          },
        },
        {
          id: 'large',
          title: '65" and more',
          expression: {
            operator: 'AND',
            clauses: [
              { field: 'screenSize', operator: 'RANGE', min: 65 },
            ],
          },
        },
      ],
    },
  ],
}

const multiWizardFixture: ModalFilterWizardDto = {
  questions: [
    {
      id: 'features',
      title: 'Key features',
      selectionType: 'MULTIPLE',
      maxSelections: 2,
      combinationOperator: 'OR',
      choices: [
        {
          id: 'hdr',
          title: 'HDR',
          expression: {
            operator: 'AND',
            clauses: [{ field: 'hasHdr', operator: 'TERM', terms: ['true'] }],
          },
        },
        {
          id: 'gaming',
          title: 'Gaming',
          expression: {
            operator: 'AND',
            clauses: [{ field: 'gamingMode', operator: 'TERM', terms: ['true'] }],
          },
        },
        {
          id: 'cinema',
          title: 'Cinema',
          expression: {
            operator: 'AND',
            clauses: [{ field: 'cinemaPreset', operator: 'TERM', terms: ['true'] }],
          },
        },
      ],
    },
  ],
}

const rippleDirective = {
  mounted: vi.fn(),
  beforeUnmount: vi.fn(),
}

type WizardProps = {
  modelValue: boolean
  wizard: ModalFilterWizardDto | null
  productCount: number | null
  loadingCount: boolean
  categoryName?: string | null
}

const mountWizard = async (props: Partial<WizardProps> = {}) => {
  const module = await import('./CategoryGuidedFiltersWizard.vue')
  const Component = module.default

  return mount(Component, {
    props: {
      modelValue: true,
      wizard: wizardFixture,
      productCount: null,
      loadingCount: false,
      categoryName: 'Televisions',
      ...props,
    },
    global: {
      stubs: {
        VDialog: VDialogStub,
        VCard: createSimpleStub('div'),
        VCardText: createSimpleStub('div'),
        VCardActions: createSimpleStub('div'),
        VProgressLinear: createSimpleStub('div'),
        VProgressCircular: createSimpleStub('div'),
        VBtn: VBtnStub,
        VSheet: VSheetStub,
        VIcon: createSimpleStub('span'),
        VImg: createSimpleStub('img'),
        VAlert: createSimpleStub('div'),
        VSpacer: createSimpleStub('div'),
      },
      directives: { ripple: rippleDirective },
    },
  })
}

describe('CategoryGuidedFiltersWizard', () => {
  it('emits filters-change when selecting a choice', async () => {
    const wrapper = await mountWizard()
    const choices = wrapper.findAll('.category-guided-wizard__choice')

    await choices.at(0)?.trigger('click')
    await nextTick()

    const emissions = wrapper.emitted('filters-change') ?? []
    const lastEmission = emissions.at(-1)
    expect(lastEmission?.[0]).toEqual({
      filters: [
        { field: 'screenType', operator: 'term', terms: ['OLED'] },
      ],
    })
  })

  it('limits the number of selections for multiple choice questions', async () => {
    const wrapper = await mountWizard({ wizard: multiWizardFixture })
    const choices = wrapper.findAll('.category-guided-wizard__choice')

    await choices.at(0)?.trigger('click')
    await choices.at(1)?.trigger('click')
    await nextTick()
    await choices.at(2)?.trigger('click')
    await nextTick()

    const selected = wrapper
      .findAll('.category-guided-wizard__choice--selected')
      .map((choice) => choice.find('.category-guided-wizard__choice-title').text())

    expect(selected).toEqual(['HDR', 'Gaming'])
  })

  it('emits apply payload with filters across all steps', async () => {
    const wrapper = await mountWizard()
    const firstChoices = wrapper.findAll('.category-guided-wizard__choice')
    await firstChoices.at(1)?.trigger('click')
    await nextTick()

    const nextButton = wrapper.findAll('.v-btn-stub').find((button) => button.text() === 'Next')
    await nextButton?.trigger('click')
    await nextTick()

    const secondChoices = wrapper.findAll('.category-guided-wizard__choice')
    await secondChoices.at(0)?.trigger('click')
    await nextTick()

    const applyButton = wrapper.findAll('.v-btn-stub').find((button) => button.text() === 'Apply filters')
    await applyButton?.trigger('click')
    await nextTick()

    const applyEvents = wrapper.emitted('apply') ?? []
    expect(applyEvents.at(0)?.[0]).toEqual({
      filters: [
        { field: 'refreshRate', operator: 'range', min: 120 },
        { field: 'screenSize', operator: 'range', min: 50, max: 60 },
      ],
    })
  })
})
