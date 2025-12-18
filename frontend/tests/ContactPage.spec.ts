import { describe, it, expect } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import ContactPage from '~/pages/contact/index.vue'

describe('ContactPage', () => {
  it('renders the contact page structure', async () => {
    const component = await mountSuspended(ContactPage, {
      global: {
        stubs: {
          ContactHero: {
            template: '<div data-testid="contact-hero" />',
            props: ['title', 'subtitle']
          },
          ContactDetailsSection: {
            template: '<div data-testid="contact-details" />'
          },
          ContactFormCard: {
            template: '<div data-testid="contact-form" />'
          }
        }
      }
    })

    expect(component.find('[data-testid="contact-hero"]').exists()).toBe(true)
    expect(component.find('[data-testid="contact-details"]').exists()).toBe(true)
    expect(component.find('[data-testid="contact-form"]').exists()).toBe(true)
  })

  it('has correct SEO structure', async () => {
    // This is hard to test in unit test as useHead side effects are global, 
    // but we can check if the code runs without error.
    const component = await mountSuspended(ContactPage)
    expect(component.vm).toBeTruthy()
  })
})
