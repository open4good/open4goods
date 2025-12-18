import { DEFAULT_PARALLAX_PACK, type ParallaxPackName } from './assets'

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
    id: 'winter-highlights',
    start: '12-01',
    end: '01-15',
    pack: 'default',
    description: 'Winter friendly visuals without SDG overlays',
  },
]

const parseMonthDay = (value: string, year: number) => {
  const [month, day] = value.split('-').map(part => Number.parseInt(part, 10))

  return new Date(Date.UTC(year, Math.max(0, month - 1), Math.max(1, day)))
}

const isDateWithinWindow = (
  date: Date,
  window: SeasonalParallaxWindow
): boolean => {
  const currentYear = date.getUTCFullYear()
  const start = parseMonthDay(window.start, currentYear)
  const end = parseMonthDay(window.end, currentYear)
  const normalizedEnd = end < start ? parseMonthDay(window.end, currentYear + 1) : end

  const inSameYear = date >= start && date <= normalizedEnd
  const inSpanningYear = date <= normalizedEnd && end < start

  return inSameYear || inSpanningYear
}

export const resolveActiveParallaxPack = (date: Date = new Date()): ParallaxPackName => {
  const window = seasonalParallaxSchedule.find(candidate =>
    isDateWithinWindow(date, candidate)
  )

  if (window) {
    return window.pack
  }

  return DEFAULT_PARALLAX_PACK
}
