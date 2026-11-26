import { describe, expect, it } from 'vitest'
import { createSSRApp, h } from 'vue'
import { renderToString } from 'vue/server-renderer'
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

  it('renders a complete <img> in the SSR HTML', async () => {
    const app = createSSRApp({
      render: () =>
        h(ProductImage, {
          src: 'https://static.nudger.fr/images/a.webp',
          alt: 'Bosch WGG244A0FR',
          sizes: '320px',
        }),
    })
    const html = await renderToString(app)
    const img = html.match(/<img[^>]*>/)?.[0] ?? ''
    expect(img).toMatch(/\ssrc="[^"]+"/)
    expect(img).toMatch(/\ssrcset="[^"]+"/)
    const alt = img.match(/\salt="([^"]*)"/)?.[1] ?? ''
    expect(alt).not.toBe('')
    expect(alt).not.toMatch(/\.(webp|jpe?g|png|avif)$/i)
  })
})
