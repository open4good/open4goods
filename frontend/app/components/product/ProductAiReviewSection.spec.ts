import { mountSuspended } from '@nuxt/test-utils/runtime'
import { describe, expect, it, vi } from 'vitest'
import { createI18n } from 'vue-i18n'
import { defineComponent, h, nextTick } from 'vue'
import ProductAiReviewSection from './ProductAiReviewSection.vue'
import type {
  AiReviewAttributeDto,
  AiReviewDto,
  AiReviewSourceDto,
} from '~~/shared/api-client'

vi.mock('vuetify', () => ({
  useTheme: () => ({
    global: {
      current: { value: { dark: false } },
    },
  }),
}))

vi.mock('@hcaptcha/vue3-hcaptcha', () => ({
  default: defineComponent({
    name: 'VueHcaptchaStub',
    emits: ['verify', 'expired', 'error'],
    setup(_, { attrs }) {
      return () => h('div', { class: 'vue-hcaptcha-stub', ...attrs })
    },
  }),
}))

vi.mock('~/composables/useIpQuota', () => ({
  useIpQuota: () => ({
    getRemaining: () => null,
    refreshQuota: vi.fn().mockResolvedValue(null),
    recordUsage: vi.fn(),
  }),
}))

const ClientOnlyStub = defineComponent({
  name: 'ClientOnlyStub',
  setup(_, { slots }) {
    return () => slots.default?.() ?? null
  },
})

const VIconStub = defineComponent({
  name: 'VIconStub',
  inheritAttrs: false,
  props: {
    icon: { type: String, default: '' },
    size: { type: [String, Number], default: '' },
  },
  setup(props, { attrs, slots }) {
    const { class: className, ...rest } = attrs

    return () =>
      h(
        'span',
        {
          ...rest,
          class: ['v-icon-stub', className].filter(Boolean).join(' '),
          'data-icon': props.icon ?? '',
          'data-size': String(props.size ?? ''),
        },
        slots.default?.()
      )
  },
})

const VTableStub = defineComponent({
  name: 'VTableStub',
  inheritAttrs: false,
  props: {
    density: { type: String, default: '' },
  },
  setup(props, { attrs, slots }) {
    const { class: className, ...rest } = attrs

    return () =>
      h(
        'table',
        {
          ...rest,
          class: ['v-table-stub', className].filter(Boolean).join(' '),
          'data-density': props.density,
        },
        slots.default?.()
      )
  },
})

const VBtnStub = defineComponent({
  name: 'VBtnStub',
  inheritAttrs: false,
  props: {
    loading: { type: Boolean, default: false },
    disabled: { type: Boolean, default: false },
  },
  setup(props, { attrs, slots }) {
    const { class: className, ...rest } = attrs

    return () =>
      h(
        'button',
        {
          ...rest,
          class: ['v-btn-stub', className].filter(Boolean).join(' '),
          'data-loading': props.loading ? 'true' : 'false',
          disabled: props.disabled || props.loading,
          type: 'button',
        },
        slots.default?.()
      )
  },
})

const VBtnToggleStub = defineComponent({
  name: 'VBtnToggleStub',
  inheritAttrs: false,
  props: {
    modelValue: { type: [String, Number], default: '' },
  },
  emits: ['update:modelValue'],
  setup(props, { attrs, slots }) {
    const { class: className, ...rest } = attrs

    return () =>
      h(
        'div',
        {
          ...rest,
          class: ['v-btn-toggle-stub', className].filter(Boolean).join(' '),
          'data-value': props.modelValue ?? '',
        },
        slots.default?.()
      )
  },
})

const VAlertStub = defineComponent({
  name: 'VAlertStub',
  inheritAttrs: false,
  setup(_, { attrs, slots }) {
    const { class: className, ...rest } = attrs

    return () =>
      h(
        'div',
        {
          ...rest,
          class: ['v-alert-stub', className].filter(Boolean).join(' '),
        },
        slots.default?.()
      )
  },
})

const VProgressLinearStub = defineComponent({
  name: 'VProgressLinearStub',
  inheritAttrs: false,
  props: {
    modelValue: { type: Number, default: 0 },
    color: { type: String, default: 'primary' },
  },
  setup(props, { attrs }) {
    const { class: className, ...rest } = attrs

    return () =>
      h('div', {
        ...rest,
        class: ['v-progress-linear-stub', className].filter(Boolean).join(' '),
        'data-value': String(props.modelValue ?? 0),
        'data-color': props.color,
      })
  },
})

const VDialogStub = defineComponent({
  name: 'VDialogStub',
  inheritAttrs: false,
  setup(_, { attrs, slots }) {
    const { class: className, ...rest } = attrs

    return () =>
      h(
        'div',
        {
          ...rest,
          class: ['v-dialog-stub', className].filter(Boolean).join(' '),
        },
        slots.default?.()
      )
  },
})

const VCardStub = defineComponent({
  name: 'VCardStub',
  inheritAttrs: false,
  setup(_, { attrs, slots }) {
    const { class: className, ...rest } = attrs

    return () =>
      h(
        'div',
        {
          ...rest,
          class: ['v-card-stub', className].filter(Boolean).join(' '),
        },
        slots.default?.()
      )
  },
})

const VCardTextStub = defineComponent({
  name: 'VCardTextStub',
  inheritAttrs: false,
  setup(_, { attrs, slots }) {
    const { class: className, ...rest } = attrs

    return () =>
      h(
        'div',
        {
          ...rest,
          class: ['v-card-text-stub', className].filter(Boolean).join(' '),
        },
        slots.default?.()
      )
  },
})

const VCardActionsStub = defineComponent({
  name: 'VCardActionsStub',
  inheritAttrs: false,
  setup(_, { attrs, slots }) {
    const { class: className, ...rest } = attrs

    return () =>
      h(
        'div',
        {
          ...rest,
          class: ['v-card-actions-stub', className].filter(Boolean).join(' '),
        },
        slots.default?.()
      )
  },
})

const VDividerStub = defineComponent({
  name: 'VDividerStub',
  inheritAttrs: false,
  setup(_, { attrs }) {
    const { class: className, ...rest } = attrs

    return () =>
      h('div', {
        ...rest,
        class: ['v-divider-stub', className].filter(Boolean).join(' '),
      })
  },
})

const VCheckboxStub = defineComponent({
  name: 'VCheckboxStub',
  inheritAttrs: false,
  setup(_, { attrs, slots }) {
    const { class: className, ...rest } = attrs

    return () =>
      h(
        'label',
        {
          ...rest,
          class: ['v-checkbox-stub', className].filter(Boolean).join(' '),
        },
        slots.label?.() ?? slots.default?.()
      )
  },
})

const VSpacerStub = defineComponent({
  name: 'VSpacerStub',
  setup(_, { attrs }) {
    return () => h('div', { ...attrs, class: 'v-spacer-stub' })
  },
})

const i18nMessages = {
  'en-US': {
    product: {
      aiReview: {
        title: 'AI review',
        subtitle: {
          one: 'Key takeaways generated by our assistant from {count} source.',
          other:
            'Key takeaways generated by our assistant from {count} sources.',
        },
        generatedAt: 'Generated on {date}',
        requestButton: 'Request an AI review',
        request: {
          agreement:
            'I authorize Nudger to research and summarize information about {productName}.',
          bannerDescription: 'Start a summary for {productName}.',
          bannerTitle: 'AI summary',
          cancel: 'Cancel',
          close: 'Close',
          description: 'blabla',
          eyebrow: 'AI summary',
          productFallback: 'this product',
          remainingUnknown: 'Unavailable',
          submit: 'Confirm',
          title: 'Request an AI summary',
          callToAction: {
            title: 'Request an AI review',
            subtitle: 'Start a summary for this product.',
          },
        },
        empty: 'No AI review is available for this product yet.',
        sections: {
          overall: 'Overall summary',
          details: 'Details and highlights',
          technical: 'Technical analysis',
          ecological: 'Ecological impact',
          community: 'Community feedback',
          pros: 'Strengths',
          cons: 'Limitations',
          identity: 'Identity card',
          sources: 'Sources',
          sourcesCount: '{count} sources consulted',
        },
        levels: {
          label: 'Reading level',
          simple: 'Simple',
          intermediate: 'Intermediate',
          advanced: 'Advanced',
        },
        sources: {
          index: '#',
          source: 'Source',
          description: 'Description',
          showLess: 'See fewer sources',
          showMore: 'See more sources',
        },
        status: {
          failed: 'Generation failed',
          success: 'Review generated',
          running: 'Generation in progress: {step}',
        },
        errors: {
          captcha: 'Captcha validation failed.',
          generic: 'Unable to generate a review.',
        },
      },
    },
    siteIdentity: {
      menu: {
        account: {
          privacy: {
            quotas: {
              aiRemaining: 'AI generations remaining',
            },
          },
        },
      },
    },
  },
}

const createI18nPlugin = () =>
  createI18n({
    legacy: false,
    locale: 'en-US',
    fallbackLocale: 'en-US',
    messages: i18nMessages,
  })

const defaultAttributes: AiReviewAttributeDto[] = [
  { name: 'Brand', value: 'Samsung' },
  { name: 'Model', value: 'TQ65S90D' },
]

const defaultSources: AiReviewSourceDto[] = [
  {
    number: 1,
    name: 'Open4Goods',
    description: 'Manufacturer data sheet',
    url: 'https://example.com/source',
    favicon: 'https://example.com/icon.png',
  },
]

const mountComponent = (
  props: Partial<InstanceType<typeof ProductAiReviewSection>['$props']>
) =>
  mountSuspended(ProductAiReviewSection, {
    props: {
      sectionId: 'ai',
      gtin: '123456789',
      siteKey: '',
      ...props,
    },
    global: {
      plugins: [[createI18nPlugin()]],
      stubs: {
        ClientOnly: ClientOnlyStub,
        VIcon: VIconStub,
        'v-icon': VIconStub,
        VTable: VTableStub,
        'v-table': VTableStub,
        VBtn: VBtnStub,
        'v-btn': VBtnStub,
        VBtnToggle: VBtnToggleStub,
        'v-btn-toggle': VBtnToggleStub,
        VAlert: VAlertStub,
        'v-alert': VAlertStub,
        VDialog: VDialogStub,
        'v-dialog': VDialogStub,
        VCard: VCardStub,
        'v-card': VCardStub,
        VCardText: VCardTextStub,
        'v-card-text': VCardTextStub,
        VCardActions: VCardActionsStub,
        'v-card-actions': VCardActionsStub,
        VDivider: VDividerStub,
        'v-divider': VDividerStub,
        VCheckbox: VCheckboxStub,
        'v-checkbox': VCheckboxStub,
        VSpacer: VSpacerStub,
        'v-spacer': VSpacerStub,
        VProgressLinear: VProgressLinearStub,
        'v-progress-linear': VProgressLinearStub,
      },
    },
  })

describe('ProductAiReviewSection', () => {
  it('renders the structured review content with highlighted sections', async () => {
    const review: AiReviewDto = {
      mediumTitle: 'Samsung TQ65S90D 2024 overview',
      shortDescription: 'A premium QLED TV with balanced performance.',
      summary:
        'The product balances vivid visuals with smart integrations for a seamless living room experience.',
      description:
        '<p>The TQ65S90D focuses on brightness control and a <a class="review-ref" href="#review-ref-1">detailed contrast engine</a>.</p>',
      technicalReviewNovice: '<p>Simple technical recap.</p>',
      technicalReviewIntermediate:
        '<p>HDMI 2.1 ports, Wi-Fi 6 support and a 120 Hz panel provide future-proof connectivity.</p>',
      technicalReviewAdvanced: '<p>Advanced technical breakdown.</p>',
      ecologicalReviewNovice: '<p>Simple ecological summary.</p>',
      ecologicalReviewIntermediate:
        '<p>Packaging uses recycled fibres and the standby consumption stays below 0.5W.</p>',
      ecologicalReviewAdvanced: '<p>Advanced ecological breakdown.</p>',
      communityReviewNovice: '<p>Community quick take.</p>',
      communityReviewIntermediate:
        '<p>Community feedback highlights balanced brightness.</p>',
      communityReviewAdvanced: '<p>Community deep dive.</p>',
      pros: ['Excellent brightness calibration', 'Robust gaming features'],
      cons: ['Lacks bundled camera accessories'],
      attributes: defaultAttributes,
      sources: defaultSources,
    }

    const wrapper = await mountComponent({
      initialReview: review,
      reviewCreatedAt: Date.UTC(2024, 3, 12),
    })

    expect(wrapper.get('.product-ai-review__title').text()).toBe('AI review')
    expect(wrapper.get('.product-ai-review__subtitle').text()).toBe(
      'Key takeaways generated by our assistant from 1 source.'
    )

    const metadata = wrapper.get('.product-ai-review__metadata').text()
    expect(metadata).toContain('Generated on')
    expect(metadata).toMatch(/2024/)

    const summary = wrapper.find('.product-ai-review__summary')
    expect(summary.exists()).toBe(true)
    expect(summary.text()).toContain('balances vivid visuals')

    const cards = wrapper.findAll('.product-ai-review__card')
    expect(cards).toHaveLength(5)

    const prosItems = wrapper.findAll('.product-ai-review__list-item')
    expect(prosItems).toHaveLength(3)
    expect(prosItems[0].text()).toContain('Excellent brightness calibration')
    expect(prosItems[2].text()).toContain('Lacks bundled camera accessories')

    expect(wrapper.text()).toContain('HDMI 2.1 ports, Wi-Fi 6 support')
    expect(wrapper.text()).toContain('Packaging uses recycled fibres')
    expect(wrapper.text()).toContain('Community feedback highlights balanced')

    const prosIcon = wrapper
      .findAll('.v-icon-stub')
      .find(icon => icon.attributes('data-icon') === 'mdi-thumb-up-outline')
    expect(prosIcon?.exists()).toBe(true)

    const sourceRows = wrapper.findAll(
      '.product-ai-review__sources .v-table-stub tbody tr'
    )
    expect(sourceRows).toHaveLength(defaultSources.length)
    expect(sourceRows[0].text()).toContain('Manufacturer data sheet')
  })

  it('renders the empty state when no review content is available', async () => {
    const wrapper = await mountComponent({ initialReview: null })

    // Check for CTA card content
    expect(wrapper.get('.product-ai-review__cta-card').text()).toContain(
      'Request an AI review'
    )
    expect(wrapper.get('.product-ai-review__cta-card').text()).toContain(
      'Start a summary for this product.'
    )

    // Check dialog is initially closed (based on internal state, though stub renders content)
    // We can check if the dialog component exists
    expect(wrapper.findComponent({ name: 'VDialogStub' }).exists()).toBe(true)

    // Simulate click on CTA button
    await wrapper.find('.product-ai-review__cta-card button').trigger('click')
    await nextTick()

    // Since we use stubs, checking if it's "open" is tricky without checking props
    // but we can verify the request panel is present in the DOM
    expect(
      wrapper.findComponent({ name: 'ProductAiReviewRequestPanel' }).exists()
    ).toBe(true)
  })

  it('expands the sources list when clicking on a hidden reference', async () => {
    const sources = Array.from({ length: 6 }, (_, index) => ({
      number: index + 1,
      name: `Source ${index + 1}`,
      description: `Source ${index + 1} description`,
      url: `https://example.com/source-${index + 1}`,
    }))

    const review: AiReviewDto = {
      summary:
        'See details <a class="review-ref" href="#review-ref-6">[6]</a>.',
      pros: ['Good image'],
      cons: ['Limited ports'],
      technicalReview: '<p>Specs</p>',
      ecologicalReview: '<p>Eco</p>',
      sources,
    }

    const wrapper = await mountComponent({ initialReview: review })
    await nextTick()

    expect(wrapper.find('#review-ref-6').exists()).toBe(false)

    await wrapper.find('a.review-ref').trigger('click')
    await nextTick()
    await nextTick()

    expect(wrapper.find('#review-ref-6').exists()).toBe(true)
  })
})
