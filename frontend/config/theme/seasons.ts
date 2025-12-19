import { DEFAULT_PARALLAX_PACK, PARALLAX_PACK_NAMES, type ParallaxPackName } from './assets'

export type SeasonalParallaxWindow = {
  id: string
  start: string
  end: string
  pack: ParallaxPackName
  description?: string
}

export const seasonalParallaxSchedule: SeasonalParallaxWindow[] = [
  {
    id: 'sdg-campaign',
    start: '04-15',
    end: '05-02',
    pack: 'sdg',
    description: 'Earth Day activation and SDG storytelling',
  },

  {
    id: 'christmas-festivities',
    start: '12-10',
    end: '12-31',
    pack: 'christmas',
    description: 'Festive visuals for the holiday season',
  },

  {
    id: 'winter-highlights',
    start: '12-01',
    end: '01-15',
    pack: 'default',
    description: 'Winter friendly visuals without SDG overlays',
  },
]

export const resolveParallaxPackName = (
  value: string | string[] | undefined
): ParallaxPackName | undefined => {
  if (!value) {
    return undefined
  }

  const packName = Array.isArray(value) ? value[0] : value

  return (PARALLAX_PACK_NAMES as readonly string[]).includes(packName)
    ? (packName as ParallaxPackName)
    : undefined
}

const isDateWithinWindow = (
  date: Date,
  window: SeasonalParallaxWindow
): boolean => {
  const month = date.getUTCMonth() + 1
  const day = date.getUTCDate()
  const dateStr = `${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}`

  if (window.start <= window.end) {
    return dateStr >= window.start && dateStr <= window.end
  } else {
    // Spans across year end (e.g., 12-01 to 01-15)
    return dateStr >= window.start || dateStr <= window.end
  }
}

export const resolveActiveParallaxPack = (
  date: Date = new Date()
): ParallaxPackName => {
  const window = seasonalParallaxSchedule.find(candidate =>
    isDateWithinWindow(date, candidate)
  )

  if (window) {
    return window.pack
  }

  return DEFAULT_PARALLAX_PACK
}
