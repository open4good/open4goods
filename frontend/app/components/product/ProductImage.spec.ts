import { describe, expect, it } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import ProductImage from './ProductImage.vue'

describe('ProductImage', () => {
  it('renders an <img> with src, srcset and alt', async () => {
    const wrapper = await mountSuspended(ProductImage, {
      props: {
        src: 'https://static.nudger.fr/images/a.webp',
        alt: 'Bosch WGG244A0FR',
        sizes: '320px',
      },
    })
    const img = wrapper.find('img')
    expect(img.exists()).toBe(true)
    expect(img.attributes('alt')).toBe('Bosch WGG244A0FR')
    expect(img.attributes('srcset')).toBeTruthy()
    expect(img.attributes('width')).toBe('400')
    expect(img.attributes('loading')).toBe('lazy')
  })
})
