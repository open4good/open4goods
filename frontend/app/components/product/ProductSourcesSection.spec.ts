import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import { createI18n } from 'vue-i18n'
import ProductSourcesSection from './ProductSourcesSection.vue'
import type { ProductSourceDto } from '~~/shared/api-client'

const i18n = createI18n({
  legacy: false,
  locale: 'en-US',
  messages: {
    'en-US': {
      product: {
        sources: {
          title: 'Data sources',
          subtitle: 'Where the information comes from.',
          license: 'License: {license}',
        },
      },
    },
  },
})

const mountSection = (sources: ProductSourceDto[] | null) =>
  mount(ProductSourcesSection, {
    props: { sources },
    global: { plugins: [i18n] },
  })

describe('ProductSourcesSection', () => {
  it('lists each source with its link, attribution and licence', () => {
    const wrapper = mountSection([
      {
        name: 'fr-indice-durabilite',
        portalUrl: 'https://www.data.gouv.fr/fr/datasets/indice/',
        license: 'Licence Ouverte 2.0',
        licenseUrl: 'https://www.etalab.gouv.fr/licence-ouverte-open-licence/',
        attribution: 'Producteurs, via data.gouv.fr',
      },
    ])

    expect(wrapper.find('h2').text()).toBe('Data sources')
    const item = wrapper.get('[data-testid="product-source"]')
    const name = item.get('a.product-sources__name')
    expect(name.text()).toBe('fr-indice-durabilite')
    expect(name.attributes('href')).toBe(
      'https://www.data.gouv.fr/fr/datasets/indice/'
    )
    expect(name.attributes('rel')).toBe('nofollow noopener noreferrer')
    expect(item.get('.product-sources__attribution').text()).toBe(
      'Producteurs, via data.gouv.fr'
    )
    const license = item.get('.product-sources__license')
    expect(license.text()).toBe('License: Licence Ouverte 2.0')
    expect(license.get('a').attributes('href')).toBe(
      'https://www.etalab.gouv.fr/licence-ouverte-open-licence/'
    )
  })

  it('shows only the name of a source without licence nor attribution', () => {
    const wrapper = mountSection([
      { name: 'rakuten.com', license: '  ', attribution: null },
      { name: '  ' },
    ])

    const items = wrapper.findAll('[data-testid="product-source"]')
    expect(items).toHaveLength(1)
    expect(items[0].text()).toBe('rakuten.com')
    expect(items[0].find('a').exists()).toBe(false)
    expect(wrapper.find('.product-sources__license').exists()).toBe(false)
    expect(wrapper.find('.product-sources__attribution').exists()).toBe(false)
    expect(wrapper.text()).not.toMatch(/license/i)
  })

  it('shows an unlinked licence when the source gives no licence URL', () => {
    const wrapper = mountSection([
      { name: 'eprel', license: 'CC BY 4.0', licenseUrl: null },
    ])

    const license = wrapper.get('.product-sources__license')
    expect(license.text()).toBe('License: CC BY 4.0')
    expect(license.find('a').exists()).toBe(false)
  })
})
