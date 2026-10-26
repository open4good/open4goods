import { mockNuxtImport, mountSuspended } from '@nuxt/test-utils/runtime'
import { injectHead, useHead, useSeoMeta } from '#imports'
import { describe, expect, it } from 'vitest'
import { defineComponent, h } from 'vue'
import { normalizeCanonicalPathname, useHeaderSeo } from './useHeaderSeo'

const requestUrl = { current: new URL('https://nudger.fr/opendata/gtin/') }

mockNuxtImport('useRequestURL', () => () => requestUrl.current)

type ResolvedTag = {
  tag: string
  props: Record<string, unknown>
  textContent?: string
}

const HeaderStub = defineComponent({
  name: 'HeaderStub',
  setup() {
    useHeaderSeo({
      title: 'Hero title',
      subtitle: '',
      headingLevel: 'h1',
    })
    return () => h('header')
  },
})

/**
 * Mounts a page that may declare its own SEO above a PageHeader-like child,
 * then returns the tags the real head resolves for that tree.
 */
const resolveHeadTags = async (pageSeo: boolean) => {
  let head: ReturnType<typeof injectHead> | undefined

  const Page = defineComponent({
    name: 'PageStub',
    setup() {
      head = injectHead()
      if (pageSeo) {
        useSeoMeta({ title: 'Page SEO title', description: 'Page SEO desc' })
        useHead({
          link: [{ rel: 'canonical', href: 'https://nudger.fr/opendata/gtin' }],
        })
      }
      return () => h('main', [h(HeaderStub)])
    },
  })

  const wrapper = await mountSuspended(Page)
  const tags = ((await head?.resolveTags()) ?? []) as ResolvedTag[]
  await wrapper.unmount()

  return {
    title: tags.find(tag => tag.tag === 'title')?.textContent,
    description: tags.find(
      tag => tag.tag === 'meta' && tag.props.name === 'description'
    )?.props.content,
    canonicals: tags
      .filter(tag => tag.tag === 'link' && tag.props.rel === 'canonical')
      .map(tag => tag.props.href),
  }
}

describe('normalizeCanonicalPathname', () => {
  it('drops trailing slashes but keeps the root path', () => {
    expect(normalizeCanonicalPathname('/opendata/gtin/')).toBe('/opendata/gtin')
    expect(normalizeCanonicalPathname('/opendata/gtin//')).toBe(
      '/opendata/gtin'
    )
    expect(normalizeCanonicalPathname('/opendata/gtin')).toBe('/opendata/gtin')
    expect(normalizeCanonicalPathname('/')).toBe('/')
  })
})

describe('useHeaderSeo', () => {
  it('keeps the page title, description and canonical over the header ones', async () => {
    requestUrl.current = new URL('https://nudger.fr/opendata/gtin/')

    const resolved = await resolveHeadTags(true)

    expect(resolved.title).toBe('Page SEO title')
    expect(resolved.description).toBe('Page SEO desc')
    expect(resolved.canonicals).toEqual(['https://nudger.fr/opendata/gtin'])
  })

  it('falls back to the header SEO with a slash-free canonical', async () => {
    requestUrl.current = new URL('https://nudger.fr/equipe/')

    const resolved = await resolveHeadTags(false)

    expect(resolved.title).toBe('Hero title')
    expect(resolved.description).toBeUndefined()
    expect(resolved.canonicals).toEqual(['https://nudger.fr/equipe'])
  })
})
