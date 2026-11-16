import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import { createI18n } from 'vue-i18n'
import { defineComponent, h, reactive, ref } from 'vue'
import ProductAttributeSourcingLabel from './ProductAttributeSourcingLabel.vue'
import {
  indexProductSources,
  productSourcesKey,
} from '~/composables/useProductSources'
import type {
  ProductAttributeSourceDto,
  ProductSourceDto,
  ProductSourcedAttributeDto,
} from '~~/shared/api-client'

const VTooltipStub = defineComponent({
  name: 'VTooltipStub',
  props: {
    text: { type: String, default: '' },
    interactive: { type: Boolean, default: false },
  },
  setup(props, { slots }) {
    return () =>
      h(
        'div',
        {
          class: 'v-tooltip-stub',
          'data-text': props.text,
          'data-interactive': String(props.interactive),
        },
        [
          slots.activator?.({ props: {} }),
          h('div', { class: 'v-tooltip-stub__content' }, slots.default?.()),
        ]
      )
  },
})

const VBtnStub = defineComponent({
  name: 'VBtnStub',
  props: {
    icon: { type: Boolean, default: false },
    density: { type: String, default: 'default' },
    variant: { type: String, default: 'text' },
    ariaLabel: { type: String, default: '' },
  },
  setup(props, { slots }) {
    return () =>
      h(
        'button',
        {
          class: 'v-btn-stub',
          'data-icon': props.icon,
          'data-density': props.density,
          'data-variant': props.variant,
          'aria-label': props.ariaLabel,
          type: 'button',
        },
        slots.default?.()
      )
  },
})

const VIconStub = defineComponent({
  name: 'VIconStub',
  props: {
    icon: { type: String, default: '' },
    color: { type: String, default: '' },
  },
  setup(props) {
    return () =>
      h('i', { class: 'v-icon-stub', 'data-color': props.color }, props.icon)
  },
})

const VCardStub = defineComponent({
  name: 'VCardStub',
  setup(_, { slots }) {
    return () => h('div', { class: 'v-card-stub' }, slots.default?.())
  },
})

const VTableStub = defineComponent({
  name: 'VTableStub',
  setup(_, { slots }) {
    return () => h('table', { class: 'v-table-stub' }, slots.default?.())
  },
})

const VDividerStub = defineComponent({
  name: 'VDividerStub',
  setup() {
    return () => h('hr', { class: 'v-divider-stub' })
  },
})

const VChipStub = defineComponent({
  name: 'VChipStub',
  setup(_, { slots }) {
    return () => h('span', { class: 'v-chip-stub' }, slots.default?.())
  },
})

const i18n = createI18n({
  legacy: false,
  locale: 'en-US',
  messages: {
    'en-US': {
      product: {
        attributes: {
          sourcing: {
            bestValue: 'Best value',
            sourceCount: {
              one: '{count} source',
              other: '{count} sources',
            },
            columns: {
              source: 'Source',
              value: 'Value',
            },
            empty: 'No sourcing information available.',
            status: {
              conflicts: 'Conflicts detected',
              noConflicts: 'No conflicts detected',
            },
            tooltipAriaLabel: 'Show sourcing details',
            license: 'License: {license}',
          },
        },
      },
    },
  },
})

describe('ProductAttributeSourcingLabel', () => {
  const mountComponent = (
    sourcing?: ProductAttributeSourceDto | null,
    value = 'Fallback',
    extraProps: Record<string, unknown> = {},
    productSources?: ProductSourceDto[]
  ) =>
    mount(ProductAttributeSourcingLabel, {
      props: { sourcing, value, ...extraProps },
      global: {
        plugins: [i18n],
        provide: productSources
          ? {
              [productSourcesKey as symbol]: ref(
                indexProductSources(productSources)
              ),
            }
          : {},
        stubs: {
          VTooltip: VTooltipStub,
          VBtn: VBtnStub,
          VIcon: VIconStub,
          VCard: VCardStub,
          VTable: VTableStub,
          VDivider: VDividerStub,
          VChip: VChipStub,
        },
      },
    })

  it('renders best value and conflict status', () => {
    const sourcing: ProductAttributeSourceDto = {
      bestValue: 'Argent',
      conflicts: true,
      sources: new Set([
        {
          datasourceName: 'fnac.com',
          value: 'Argent',
          language: 'fr',
          icecatTaxonomyId: 46757,
        },
      ]),
    }

    const wrapper = mountComponent(sourcing, 'Original value')

    expect(wrapper.text()).toContain('Argent')

    const icon = wrapper.find('.v-icon-stub')
    expect(icon.attributes('data-color')).toBe('warning')

    const tableRows = wrapper.findAll('.v-table-stub tbody tr')
    expect(tableRows).toHaveLength(1)
    expect(tableRows[0].text()).toContain('fnac.com')

    const countLabel = wrapper.find(
      '.product-attribute-sourcing__tooltip-count'
    )
    expect(countLabel.text()).toContain('1 source')
  })

  it('hides tooltip when sourcing is missing', () => {
    const wrapper = mountComponent(null, 'Displayed value')
    expect(wrapper.find('.v-btn-stub').exists()).toBe(false)
    expect(wrapper.text()).toContain('Displayed value')
  })

  it('can disable tooltip even when sourcing exists', () => {
    const sourcing: ProductAttributeSourceDto = {
      bestValue: 'Argent',
      conflicts: false,
      sources: [
        {
          datasourceName: 'fnac.com',
          value: 'Argent',
        },
      ],
    }

    const wrapper = mountComponent(sourcing, 'Argent', { enableTooltip: false })
    expect(wrapper.find('.v-btn-stub').exists()).toBe(false)
    expect(wrapper.text()).toContain('Argent')
  })

  it('renders sources when provided through a proxied Set', () => {
    const reactiveSources = reactive(
      new Set<ProductSourcedAttributeDto>([
        {
          datasourceName: 'icecat.biz',
          value: '9',
          language: null,
          icecatTaxonomyId: 3566,
        },
      ])
    )

    const sourcing: ProductAttributeSourceDto = {
      bestValue: '9',
      conflicts: false,
      sources: reactiveSources as unknown as Set<ProductSourcedAttributeDto>,
    }

    const wrapper = mountComponent(sourcing, '9')

    const rows = wrapper.findAll('.v-table-stub tbody tr')
    expect(rows).toHaveLength(1)
    expect(rows[0].text()).toContain('icecat.biz')

    const countLabel = wrapper.find(
      '.product-attribute-sourcing__tooltip-count'
    )
    expect(countLabel.text()).toContain('1 source')
  })

  it('renders sources when provided through a ref of array values', () => {
    const sourcesRef = ref<ProductSourcedAttributeDto[]>([
      {
        datasourceName: 'eprel',
        value: '269',
        language: null,
        name: 'POWERONMODEHDR',
      },
    ])

    const sourcing: ProductAttributeSourceDto = {
      bestValue: '269',
      conflicts: false,
      sources: sourcesRef as unknown as ProductAttributeSourceDto['sources'],
    }

    const wrapper = mountComponent(sourcing, '269')

    const rows = wrapper.findAll('.v-table-stub tbody tr')
    expect(rows).toHaveLength(1)
    expect(rows[0].text()).toContain('eprel')

    const countLabel = wrapper.find(
      '.product-attribute-sourcing__tooltip-count'
    )
    expect(countLabel.text()).toContain('1 source')
  })

  describe('licence and link of the sources (GOU-2302)', () => {
    const sourcing: ProductAttributeSourceDto = {
      bestValue: '7.9',
      conflicts: false,
      sources: [
        { datasourceName: 'fr-indice-durabilite', value: '7.9' },
        { datasourceName: 'rakuten.com', value: '7.9' },
      ],
    }

    it('shows the licence and the link of a source that states them', () => {
      const wrapper = mountComponent(sourcing, '7.9', {}, [
        {
          name: 'fr-indice-durabilite',
          portalUrl: 'https://www.data.gouv.fr/fr/datasets/indice/',
          license: 'Licence Ouverte 2.0',
          licenseUrl:
            'https://www.etalab.gouv.fr/licence-ouverte-open-licence/',
          attribution: 'Producteurs, via data.gouv.fr',
        },
        { name: 'rakuten.com', portalUrl: 'https://www.rakuten.com/' },
      ])

      const rows = wrapper.findAll('.v-table-stub tbody tr')
      expect(rows).toHaveLength(2)

      const sourceLink = rows[0].find(
        'a.product-attribute-sourcing__source-link'
      )
      expect(sourceLink.attributes('href')).toBe(
        'https://www.data.gouv.fr/fr/datasets/indice/'
      )
      expect(sourceLink.attributes('rel')).toContain('noopener')
      expect(sourceLink.text()).toBe('fr-indice-durabilite')

      const license = rows[0].find(
        '.product-attribute-sourcing__source-license'
      )
      expect(license.text()).toBe('License: Licence Ouverte 2.0')
      expect(license.find('a').attributes('href')).toBe(
        'https://www.etalab.gouv.fr/licence-ouverte-open-licence/'
      )

      expect(
        rows[1].find('.product-attribute-sourcing__source-license').exists()
      ).toBe(false)
      // A source without licence nor attribution keeps its plain name.
      expect(rows[1].find('a').exists()).toBe(false)
      expect(rows[1].text()).toContain('rakuten.com')
      expect(
        wrapper.find('.v-tooltip-stub').attributes('data-interactive')
      ).toBe('true')
    })

    it('shows no link nor licence when the source states none', () => {
      const wrapper = mountComponent(sourcing, '7.9', {}, [
        {
          name: 'fr-indice-durabilite',
          portalUrl: 'https://www.data.gouv.fr/fr/datasets/indice/',
          license: '  ',
          licenseUrl:
            'https://www.etalab.gouv.fr/licence-ouverte-open-licence/',
          attribution: null,
        },
        { name: 'rakuten.com', portalUrl: 'https://www.rakuten.com/' },
      ])

      const rows = wrapper.findAll('.v-table-stub tbody tr')
      expect(rows).toHaveLength(2)
      expect(rows[0].text()).toContain('fr-indice-durabilite')
      expect(
        wrapper.find('.product-attribute-sourcing__source-license').exists()
      ).toBe(false)
      expect(wrapper.find('a').exists()).toBe(false)
      expect(wrapper.text()).not.toMatch(/license/i)
      expect(
        wrapper.find('.v-tooltip-stub').attributes('data-interactive')
      ).toBe('false')
    })

    it('links a source that declares only an attribution, without licence', () => {
      const wrapper = mountComponent(sourcing, '7.9', {}, [
        {
          name: 'fr-indice-durabilite',
          portalUrl: 'https://www.data.gouv.fr/fr/datasets/indice/',
          attribution: 'Producteurs, via data.gouv.fr',
        },
      ])

      const rows = wrapper.findAll('.v-table-stub tbody tr')
      expect(
        rows[0].find('a.product-attribute-sourcing__source-link').exists()
      ).toBe(true)
      expect(
        rows[0].find('.product-attribute-sourcing__source-license').exists()
      ).toBe(false)
      expect(rows[1].find('a').exists()).toBe(false)
    })

    it('keeps the plain source names outside a product page', () => {
      const wrapper = mountComponent(sourcing, '7.9')

      const rows = wrapper.findAll('.v-table-stub tbody tr')
      expect(rows[0].text()).toContain('fr-indice-durabilite')
      expect(wrapper.find('a').exists()).toBe(false)
    })
  })
})
