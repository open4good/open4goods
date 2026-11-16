/**
 * Blog cover generator: the "data cover" template (1600x900 PNG) used as a blog post's `image`,
 * hence also its og:image. Everything is drawn from the post's `cover:` frontmatter block and the
 * repository's own assets (Hanken Grotesk TTF, Nudger pictogram, @mdi/js icons), so the output does
 * not depend on system fonts or on any tool beyond `sharp`. Text is laid out by sharp's Pango text
 * input; shapes and icons come from an SVG background. A text that does not fit its zone throws:
 * the cover is never truncated silently.
 *
 * See docs/markdown-mapping.md, section "Blog cover".
 */
import { readFile } from 'node:fs/promises'
import { fileURLToPath } from 'node:url'

import {
  mdiAirConditioner,
  mdiCellphone,
  mdiDishwasher,
  mdiFridgeOutline,
  mdiLaptop,
  mdiLeaf,
  mdiRecycle,
  mdiStove,
  mdiTelevision,
  mdiTumbleDryer,
  mdiWashingMachine,
  mdiWrenchOutline,
} from '@mdi/js'
import sharp from 'sharp'

const FRONTEND_DIR = fileURLToPath(new URL('../../', import.meta.url))

export const COVER_WIDTH = 1600
export const COVER_HEIGHT = 900

const FONT_FILE = `${FRONTEND_DIR}public/fonts/HankenGrotesk/HankenGrotesk-Regular.ttf`
const FONT_FAMILY = 'Hanken Grotesk'
const LOGO_FILE = `${FRONTEND_DIR}app/assets/themes/light/logo-inversed.svg`

const COLORS = {
  backgroundFrom: '#0B3A31',
  backgroundTo: '#0A6652',
  accent: '#00D19F',
  title: '#FFFFFF',
  caption: '#E8F5F1',
  muted: '#9CC9BD',
  iconOff: '#3E7F70',
}

/** Pictograms a cover may show, keyed by the value of `cover.icon`. */
export const COVER_ICONS = {
  'air-conditioner': mdiAirConditioner,
  dishwasher: mdiDishwasher,
  dryer: mdiTumbleDryer,
  fridge: mdiFridgeOutline,
  laptop: mdiLaptop,
  leaf: mdiLeaf,
  oven: mdiStove,
  recycle: mdiRecycle,
  repair: mdiWrenchOutline,
  smartphone: mdiCellphone,
  tv: mdiTelevision,
  'washing-machine': mdiWashingMachine,
}

/**
 * Safe area. Listing cards (about 1.53:1) and social previews (1.91:1) crop a 16:9 cover by up to
 * 7 % on each side, so no text or icon is drawn outside it; only the decorative circle is.
 */
const SAFE = { left: 150, right: 1450, top: 80, bottom: 800 }
const PAD_X = SAFE.left
const LEFT_WIDTH = 760
const WAFFLE = { left: 946, top: 400, size: 88, step: 104 }
const BIG_ICON = { left: 1038, top: 300, size: 320 }

/**
 * Text zones of the template. `maxLines` is the hard limit checked after layout; `size` is in
 * pixels (the text is rendered at 72 dpi, so points equal pixels).
 */
const ZONES = {
  kicker: { top: SAFE.top, height: 52, size: 24, maxWidth: 640 },
  titleWithFigure: { top: 170, size: 58, maxLines: 2 },
  titleAlone: { top: 164, size: 68, maxLines: 4 },
  figure: { top: 360, size: 250 },
  caption: {
    top: 620,
    size: 38,
    maxLines: 2,
    maxLinesAlone: 3,
    widthWithFigure: 620,
  },
  source: { top: 756, size: 24, maxLines: 1 },
  ratioLabel: {
    top: 640,
    size: 26,
    maxLines: 1,
    left: WAFFLE.left,
    width: SAFE.right - WAFFLE.left,
  },
  footer: { size: 32, logoSize: 72, centerY: SAFE.bottom - 36 },
}

/** Lowest pixel the title and caption may reach (above the source line). */
const TEXT_BOTTOM = ZONES.source.top - 16
const CAPTION_GAP = 32

/** Raised when a cover spec is invalid or one of its texts does not fit the template. */
export class BlogCoverError extends Error {
  constructor(message) {
    super(message)
    this.name = 'BlogCoverError'
  }
}

const optionalString = (value, field) => {
  if (value === undefined || value === null) {
    return undefined
  }
  if (typeof value !== 'string') {
    throw new BlogCoverError(`cover.${field} must be a string`)
  }
  const trimmed = value.trim()
  return trimmed === '' ? undefined : trimmed
}

/**
 * Validates and normalizes a post's `cover:` frontmatter block. `fallbackKicker` (the post's first
 * tag) is used when the block has no `kicker`.
 *
 * @param {unknown} cover the raw `cover:` value
 * @param {{ fallbackKicker?: string }} [options]
 * @returns {{ kicker?: string, title: string, figure?: string, caption?: string, source?: string,
 *   icon: string, ratio?: number, ratioLabel?: string }}
 */
export function normalizeCoverSpec(cover, { fallbackKicker } = {}) {
  if (!cover || typeof cover !== 'object' || Array.isArray(cover)) {
    throw new BlogCoverError(
      'the post has no `cover:` block in its frontmatter'
    )
  }
  const title = optionalString(cover.title, 'title')
  if (!title) {
    throw new BlogCoverError('cover.title is required')
  }
  const icon = optionalString(cover.icon, 'icon') ?? 'leaf'
  if (!(icon in COVER_ICONS)) {
    throw new BlogCoverError(
      `cover.icon "${icon}" is unknown; use one of: ${Object.keys(COVER_ICONS).join(', ')}`
    )
  }
  let ratio
  if (cover.ratio !== undefined && cover.ratio !== null) {
    if (typeof cover.ratio !== 'number' || !Number.isFinite(cover.ratio)) {
      throw new BlogCoverError('cover.ratio must be a number between 0 and 1')
    }
    if (cover.ratio < 0 || cover.ratio > 1) {
      throw new BlogCoverError('cover.ratio must be between 0 and 1')
    }
    ratio = cover.ratio
  }
  const figure = optionalString(cover.figure, 'figure')
  const caption = optionalString(cover.caption, 'caption')
  if (figure && !caption) {
    throw new BlogCoverError('cover.caption is required with cover.figure')
  }
  return {
    kicker:
      optionalString(cover.kicker, 'kicker') ??
      optionalString(fallbackKicker, 'kicker'),
    title,
    figure,
    caption,
    source: optionalString(cover.source, 'source'),
    icon,
    ratio,
    ratioLabel: optionalString(cover.ratioLabel, 'ratioLabel'),
  }
}

/** Number of highlighted icons, out of 10, in the waffle drawn for `ratio`. */
export function waffleHighlightCount(ratio) {
  return Math.min(10, Math.max(0, Math.round(ratio * 10)))
}

/** Escapes a plain string for Pango markup. */
export function escapePangoMarkup(text) {
  return text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
}

/**
 * Renders a text block with the repository font. Returns the RGBA PNG and its size, plus the
 * number of lines Pango produced (measured against a one-line and a two-line reference, so it
 * does not depend on how Pango spaces lines).
 */
async function renderText(text, { size, color, width, letterSpacing }) {
  const render = async markupText => {
    const spacingAttr = letterSpacing
      ? ` letter_spacing="${Math.round(letterSpacing * 1024)}"`
      : ''
    const markup = `<span foreground="${color}"${spacingAttr}>${markupText}</span>`
    const { data, info } = await sharp({
      text: {
        text: markup,
        font: `${FONT_FAMILY} ${size}`,
        fontfile: FONT_FILE,
        rgba: true,
        dpi: 72,
        ...(width ? { width, wrap: 'word' } : {}),
      },
    })
      .png()
      .toBuffer({ resolveWithObject: true })
    return { data, width: info.width, height: info.height }
  }
  const block = await render(escapePangoMarkup(text))
  const oneLine = await render('Ag')
  const twoLines = await render('Ag\nAg')
  const lineStep = twoLines.height - oneLine.height
  const lines =
    1 + Math.max(0, Math.round((block.height - oneLine.height) / lineStep))
  return { ...block, lines }
}

const assertLines = (rendered, zone, field, text) => {
  if (rendered.lines > zone.maxLines) {
    throw new BlogCoverError(
      `cover.${field} takes ${rendered.lines} lines, the template allows ${zone.maxLines}: shorten "${text}"`
    )
  }
}

const iconSvg = (path, { x, y, size, color }) =>
  `<path d="${path}" fill="${color}" transform="translate(${x} ${y}) scale(${size / 24})"/>`

function backgroundSvg(spec, { kickerWidth }) {
  const parts = [
    `<svg xmlns="http://www.w3.org/2000/svg" width="${COVER_WIDTH}" height="${COVER_HEIGHT}" viewBox="0 0 ${COVER_WIDTH} ${COVER_HEIGHT}">`,
    '<defs><linearGradient id="bg" x1="0" y1="0" x2="1" y2="1">',
    `<stop offset="0" stop-color="${COLORS.backgroundFrom}"/><stop offset="1" stop-color="${COLORS.backgroundTo}"/>`,
    '</linearGradient></defs>',
    `<rect width="${COVER_WIDTH}" height="${COVER_HEIGHT}" fill="url(#bg)"/>`,
    `<circle cx="1390" cy="170" r="420" fill="${COLORS.accent}" opacity="0.07"/>`,
  ]
  if (spec.kicker) {
    parts.push(
      `<rect x="${PAD_X}" y="${ZONES.kicker.top}" width="${kickerWidth}" height="${ZONES.kicker.height}" rx="${ZONES.kicker.height / 2}" fill="none" stroke="${COLORS.accent}" stroke-width="2.5"/>`
    )
  }
  const path = COVER_ICONS[spec.icon]
  if (spec.ratio !== undefined) {
    const highlighted = waffleHighlightCount(spec.ratio)
    for (let index = 0; index < 10; index += 1) {
      const column = index % 5
      const row = Math.floor(index / 5)
      parts.push(
        iconSvg(path, {
          x: WAFFLE.left + column * WAFFLE.step,
          y: WAFFLE.top + row * WAFFLE.step,
          size: WAFFLE.size,
          color: index < highlighted ? COLORS.accent : COLORS.iconOff,
        })
      )
    }
  } else {
    parts.push(
      iconSvg(path, {
        x: BIG_ICON.left,
        y: BIG_ICON.top,
        size: BIG_ICON.size,
        color: COLORS.accent,
      })
    )
  }
  parts.push('</svg>')
  return parts.join('')
}

/**
 * Renders the cover of a normalized spec (see normalizeCoverSpec) to a 1600x900 PNG buffer.
 *
 * @throws {BlogCoverError} when a text does not fit its zone
 */
export async function renderBlogCover(spec) {
  const layers = []

  let kickerWidth = 0
  if (spec.kicker) {
    const kicker = await renderText(spec.kicker.toLocaleUpperCase('fr-FR'), {
      size: ZONES.kicker.size,
      color: COLORS.accent,
      letterSpacing: 2,
    })
    kickerWidth = kicker.width + 72
    if (kickerWidth > ZONES.kicker.maxWidth) {
      throw new BlogCoverError(
        `cover.kicker is too long for its pill: shorten "${spec.kicker}"`
      )
    }
    layers.push({
      input: kicker.data,
      left: PAD_X + 36,
      top:
        ZONES.kicker.top +
        Math.round((ZONES.kicker.height - kicker.height) / 2),
    })
  }

  const titleZone = spec.figure ? ZONES.titleWithFigure : ZONES.titleAlone
  const title = await renderText(spec.title, {
    size: titleZone.size,
    color: COLORS.title,
    width: LEFT_WIDTH,
  })
  assertLines(title, titleZone, 'title', spec.title)

  const caption = spec.caption
    ? await renderText(spec.caption, {
        size: ZONES.caption.size,
        color: spec.figure ? COLORS.caption : COLORS.muted,
        width: spec.figure ? ZONES.caption.widthWithFigure : LEFT_WIDTH,
      })
    : undefined
  if (caption) {
    const maxLines = spec.figure
      ? ZONES.caption.maxLines
      : ZONES.caption.maxLinesAlone
    assertLines(caption, { maxLines }, 'caption', spec.caption)
  }

  let titleTop = titleZone.top
  let captionTop = ZONES.caption.top
  if (spec.figure) {
    const figure = await renderText(spec.figure, {
      size: ZONES.figure.size,
      color: COLORS.accent,
    })
    if (figure.width > LEFT_WIDTH) {
      throw new BlogCoverError(
        `cover.figure is too wide: shorten "${spec.figure}"`
      )
    }
    layers.push({ input: figure.data, left: PAD_X - 8, top: ZONES.figure.top })
  } else {
    // Without a figure, the title and its caption are centred in the band above the source line.
    const blockHeight =
      title.height + (caption ? CAPTION_GAP + caption.height : 0)
    titleTop = Math.max(
      titleZone.top,
      Math.round((titleZone.top + TEXT_BOTTOM - blockHeight) / 2)
    )
    captionTop = titleTop + title.height + CAPTION_GAP
  }
  layers.push({ input: title.data, left: PAD_X, top: titleTop })

  if (caption) {
    if (captionTop + caption.height > TEXT_BOTTOM) {
      throw new BlogCoverError(
        `cover.title and cover.caption do not fit together: shorten one of them ("${spec.caption}")`
      )
    }
    layers.push({ input: caption.data, left: PAD_X + 4, top: captionTop })
  }

  if (spec.source) {
    const source = await renderText(spec.source, {
      size: ZONES.source.size,
      color: COLORS.muted,
      width: LEFT_WIDTH,
    })
    assertLines(source, ZONES.source, 'source', spec.source)
    layers.push({ input: source.data, left: PAD_X + 4, top: ZONES.source.top })
  }

  if (spec.ratio !== undefined && spec.ratioLabel) {
    const label = await renderText(spec.ratioLabel, {
      size: ZONES.ratioLabel.size,
      color: COLORS.muted,
      width: ZONES.ratioLabel.width,
    })
    assertLines(label, ZONES.ratioLabel, 'ratioLabel', spec.ratioLabel)
    layers.push({
      input: label.data,
      left: ZONES.ratioLabel.left,
      top: ZONES.ratioLabel.top,
    })
  }

  // Footer, right-aligned on the safe area: pictogram, then "nudger.fr".
  const { logoSize, centerY } = ZONES.footer
  const footer = await renderText('nudger.fr', {
    size: ZONES.footer.size,
    color: COLORS.title,
  })
  const footerLeft = SAFE.right - footer.width
  const logo = await sharp(await readFile(LOGO_FILE))
    .resize(logoSize, logoSize)
    .png()
    .toBuffer()
  layers.push({
    input: logo,
    left: footerLeft - 16 - logoSize,
    top: centerY - logoSize / 2,
  })
  layers.push({
    input: footer.data,
    left: footerLeft,
    top: centerY - Math.round(footer.height / 2),
  })

  return sharp(Buffer.from(backgroundSvg(spec, { kickerWidth })))
    .composite(layers)
    .png({ compressionLevel: 9 })
    .toBuffer()
}
