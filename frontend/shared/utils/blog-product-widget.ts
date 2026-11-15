/**
 * Product widget written in a blog article body (GOU-2225):
 *
 * ```md
 * ::guide-product-grid{vertical="tv" subcategory="tv_55_inches" top="3"}
 * ::
 * ```
 *
 * The blog body is rendered to an HTML string by markdown-it (server/utils/blog-content.ts), not by
 * MDC. The directive becomes an empty marker element carrying only validated values; TheArticle.vue
 * splits the sanitized body on that marker and renders GuideProductGrid in its place, during SSR.
 * Values never reach the HTML unvalidated, so the content cannot inject markup through it.
 */
export interface BlogProductWidget {
  vertical: string
  subcategory?: string
  top: number
}

export const PRODUCT_WIDGET_DIRECTIVE = /^::guide-product-grid\{([^}]*)\}\s*$/

const ID_PATTERN = /^[a-z0-9_-]{1,64}$/
const DEFAULT_TOP = 3
const MAX_TOP = 12

const parseTop = (value?: string): number => {
  const parsed = /^\d{1,2}$/.test(value ?? '') ? Number(value) : Number.NaN
  return parsed >= 1 && parsed <= MAX_TOP ? parsed : DEFAULT_TOP
}

/**
 * Validates raw widget values. Returns null (the widget is dropped) when the vertical is missing or
 * invalid, or when the sub-category is invalid; an invalid `top` falls back to 3.
 */
export function toBlogProductWidget(raw: {
  vertical?: string
  subcategory?: string
  top?: string
}): BlogProductWidget | null {
  const vertical = raw.vertical?.trim() ?? ''
  if (!ID_PATTERN.test(vertical)) {
    return null
  }
  const subcategory = raw.subcategory?.trim() ?? ''
  if (subcategory && !ID_PATTERN.test(subcategory)) {
    return null
  }
  return {
    vertical,
    ...(subcategory ? { subcategory } : {}),
    top: parseTop(raw.top?.trim()),
  }
}

/** Parses the `{…}` attribute list of the directive (`key="value"` pairs, unknown keys ignored). */
export function parseProductWidgetAttributes(
  attributes: string
): BlogProductWidget | null {
  const values: Record<string, string> = {}
  for (const [, key, value] of attributes.matchAll(/([a-z]+)="([^"]*)"/g)) {
    if (key && value !== undefined) {
      values[key] = value
    }
  }
  return toBlogProductWidget(values)
}

/** Marker element emitted in the rendered body. One attribute only, so the sanitizer keeps it as is. */
export function renderProductWidgetMarker(widget: BlogProductWidget): string {
  return `<div data-product-widget="${widget.vertical}|${widget.subcategory ?? ''}|${widget.top}"></div>`
}

export type BlogBodyPart =
  | { type: 'html'; content: string }
  | { type: 'assistant' }
  | { type: 'product-widget'; widget: BlogProductWidget }

const BODY_MARKERS =
  /\[assistant\]|<div data-product-widget="([a-z0-9_-]+)\|([a-z0-9_-]*)\|(\d{1,2})"><\/div>/g

/**
 * Splits a sanitized blog body on its `[assistant]` and product widget markers. A body without
 * marker is returned as a single HTML part, unchanged.
 */
export function splitBlogBody(html: string): BlogBodyPart[] {
  const parts: BlogBodyPart[] = []
  let cursor = 0
  for (const match of html.matchAll(BODY_MARKERS)) {
    const before = html.slice(cursor, match.index)
    if (before) {
      parts.push({ type: 'html', content: before })
    }
    cursor = match.index + match[0].length
    if (match[0] === '[assistant]') {
      parts.push({ type: 'assistant' })
      continue
    }
    const widget = toBlogProductWidget({
      vertical: match[1],
      subcategory: match[2],
      top: match[3],
    })
    if (widget) {
      parts.push({ type: 'product-widget', widget })
    }
  }
  if (!parts.length) {
    return [{ type: 'html', content: html }]
  }
  const rest = html.slice(cursor)
  if (rest) {
    parts.push({ type: 'html', content: rest })
  }
  return parts
}
