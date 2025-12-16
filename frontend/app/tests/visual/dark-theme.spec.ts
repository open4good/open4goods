import { describe, expect, it } from 'vitest'
import {
  vuetifyPalettes,
  type ThemeColors,
} from '../../../config/theme/palettes'

const darkColors: ThemeColors = vuetifyPalettes.dark ?? {}

const expectedDarkSurfaceTokens: Record<string, string> = {
  'surface-default': '#0F172A',
  'surface-muted': '#111827',
  'surface-alt': '#1E293B',
  'surface-glass': '#1E293B',
  'surface-glass-strong': '#111827',
  'surface-primary-050': '#0B1220',
  'surface-primary-080': '#13213B',
  'surface-primary-100': '#1B2A44',
  'surface-primary-120': '#22304C',
  'surface-ice-050': '#152238',
  'surface-ice-100': '#0F172A',
  'surface-muted-contrast': '#1F2937',
  'text-neutral-strong': '#F8FAFC',
  'text-neutral-secondary': '#CBD5F5',
  'text-neutral-soft': '#94A3B8',
  'text-on-accent': '#E2E8F0',
}

type Rgb = [number, number, number]

const hexToRgb = (hex: string): Rgb => {
  const normalized = hex.replace('#', '')
  const value =
    normalized.length === 3
      ? normalized
          .split('')
          .map(char => char + char)
          .join('')
      : normalized
  const int = Number.parseInt(value, 16)
  return [(int >> 16) & 255, (int >> 8) & 255, int & 255]
}

const luminance = ([r, g, b]: Rgb): number => {
  const normalizeChannel = (channel: number) => {
    const proportion = channel / 255
    return proportion <= 0.03928
      ? proportion / 12.92
      : ((proportion + 0.055) / 1.055) ** 2.4
  }

  const [red, green, blue] = [r, g, b].map(normalizeChannel) as Rgb
  return 0.2126 * red + 0.7152 * green + 0.0722 * blue
}

const contrastRatio = (
  foregroundHex: string,
  backgroundHex: string
): number => {
  const foregroundLum = luminance(hexToRgb(foregroundHex))
  const backgroundLum = luminance(hexToRgb(backgroundHex))
  const [lighter, darker] = [foregroundLum, backgroundLum].sort((a, b) => b - a)

  return (lighter + 0.05) / (darker + 0.05)
}

describe('vuetify dark palette', () => {
  it('aligns surface and text tokens with the glossary', () => {
    Object.entries(expectedDarkSurfaceTokens).forEach(
      ([token, expectedValue]) => {
        expect(darkColors[token]).toBe(expectedValue)
      }
    )
  })

  it('keeps readable contrast for menu, hero search, and callouts', () => {
    const assertions: Array<[string, string, string]> = [
      ['text-neutral-strong', 'surface-default', 'Main menu surface'],
      ['text-neutral-strong', 'surface-glass-strong', 'Hero search input'],
      ['text-neutral-strong', 'surface-callout-start', 'Callout cards base'],
      ['text-on-accent', 'accent-callout', 'Accent callout foreground'],
    ]

    assertions.forEach(([foregroundToken, backgroundToken, context]) => {
      const foreground = darkColors[foregroundToken]
      const background = darkColors[backgroundToken]

      expect(foreground).toBeTruthy()
      expect(background).toBeTruthy()

      const ratio = contrastRatio(foreground, background)
      expect(
        ratio,
        `${context} should keep WCAG AA contrast`
      ).toBeGreaterThanOrEqual(4.5)
    })
  })
})
