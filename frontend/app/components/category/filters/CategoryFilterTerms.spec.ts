import { describe, expect, it } from 'vitest'
import { createI18n } from 'vue-i18n'
import { mount } from '@vue/test-utils'
import type { FieldMetadataDto } from '~~/shared/api-client'

import CategoryFilterTerms from './CategoryFilterTerms.vue'

const mountTerms = (locale: 'fr' | 'en') =>
  mount(CategoryFilterTerms, {
    props: {
      field: {
        mapping: 'price.conditions',
        title: 'État des offres',
      } as FieldMetadataDto,
      aggregation: {
        buckets: [
          { key: 'NEW', count: 981 },
          { key: 'OCCASION', count: 673 },
        ],
      } as never,
    },
    global: {
      plugins: [
        createI18n({
          legacy: false,
          locale,
          messages: {
            fr: {
              category: {
                filters: { condition: { new: 'Neuf', used: 'Occasion' } },
              },
            },
            en: {
              category: {
                filters: { condition: { new: 'New', used: 'Used' } },
              },
            },
          },
        }),
      ],
      stubs: {
        VCheckbox: {
          props: ['label'],
          template: '<div class="cb">{{ label }}<slot name="append" /></div>',
        },
        VTextField: true,
      },
    },
  })

describe('CategoryFilterTerms condition labels', () => {
  it('shows Neuf / Occasion in French with counts', () => {
    const text = mountTerms('fr').text()
    expect(text).toContain('Neuf')
    expect(text).toContain('Occasion')
    expect(text).toContain('981')
    expect(text).not.toContain('NEW')
  })

  it('shows New / Used in English', () => {
    const text = mountTerms('en').text()
    expect(text).toContain('New')
    expect(text).toContain('Used')
    expect(text).not.toContain('OCCASION')
  })
})
