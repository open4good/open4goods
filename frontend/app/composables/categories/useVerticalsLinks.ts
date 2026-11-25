import type { VerticalConfigDto } from '~~/shared/api-client'

export interface VerticalLink {
  id: string
  title: string
  href: string
  subCategories: Array<{ id: string; title: string; href: string }>
  guides: Array<{ slug: string; href: string }>
}

const joinPath = (base: string, slug: string) =>
  `${base.replace(/\/+$/, '')}/${slug.replace(/^\/+/, '')}`

/**
 * Plain, enabled verticals with their sub-category and guide links, ordered like the menus.
 * Pure so it stays testable and renders identically on server and client.
 */
export const buildVerticalLinks = (
  verticals: readonly VerticalConfigDto[] | null | undefined
): VerticalLink[] =>
  [...(verticals ?? [])]
    .filter(vertical => vertical.enabled !== false)
    .sort(
      (a, b) =>
        (a.order ?? Number.MAX_SAFE_INTEGER) -
        (b.order ?? Number.MAX_SAFE_INTEGER)
    )
    .flatMap(vertical => {
      const base = vertical.verticalHomeUrl?.trim()
      if (!vertical.id || !base || !base.startsWith('/')) {
        return []
      }

      return [
        {
          id: vertical.id,
          title: vertical.verticalHomeTitle?.trim() || vertical.id,
          href: base,
          subCategories: (vertical.subCategories ?? [])
            .filter(sub => sub.slug?.trim())
            .map(sub => ({
              id: sub.id ?? sub.slug ?? '',
              title: sub.h1Title?.trim() || sub.slug!.trim(),
              href: joinPath(base, sub.slug!.trim()),
            })),
          guides: (vertical.guides ?? [])
            .filter(guide => guide?.trim())
            .map(guide => ({
              slug: guide.trim(),
              href: joinPath(base, guide.trim()),
            })),
        },
      ]
    })

/**
 * Verticals links fetched during SSR (shared payload key), so menus and the footer carry
 * crawlable `<a href>` on every page, mobile included.
 */
export const useVerticalsLinks = async () => {
  const { fetchCategories } = useCategories()
  const { data } = await useAsyncData<VerticalConfigDto[]>(
    'verticals-links',
    async () => [...(await fetchCategories())],
    { default: () => [] }
  )

  return computed(() => buildVerticalLinks(data.value))
}
