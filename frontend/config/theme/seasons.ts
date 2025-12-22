import {
  DEFAULT_EVENT_PACK,
  EVENT_PACK_NAMES,
  type EventPackName,
} from './assets'

export type SeasonalEventWindow = {
  id: string
  start: string
  end: string
  pack: EventPackName
  description?: string
}

export const seasonalEventSchedule: SeasonalEventWindow[] = [
  {
    id: 'sdg-campaign',
    start: '04-15',
    end: '05-02',
    pack: 'sdg',
    description: 'Earth Day activation and SDG storytelling',
  },

  {
    id: 'bastille-day',
    start: '07-10',
    end: '07-16',
    pack: 'bastille-day',
    description: 'Bastille Day fireworks and civic celebrations',
  },

  {
    id: 'winter-highlights',
    start: '12-01',
    end: '01-15',
    pack: 'default',
    description: 'Winter friendly visuals without SDG overlays',
  },
]

export const resolveEventPackName = (
  value: string | string[] | undefined
): EventPackName | undefined => {
  if (!value) {
    return undefined
  }

  const packName = Array.isArray(value) ? value[0] : value

  return (EVENT_PACK_NAMES as readonly string[]).includes(packName)
    ? (packName as EventPackName)
    : undefined
}

const isDateWithinWindow = (
  date: Date,
  window: SeasonalEventWindow
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

export const resolveActiveEventPack = (
  date: Date = new Date()
): EventPackName => {
  const window = seasonalEventSchedule.find(candidate =>
    isDateWithinWindow(date, candidate)
  )

  if (window) {
    return window.pack
  }

  return DEFAULT_EVENT_PACK
}

/** @deprecated Use {@link resolveEventPackName} instead. */
export const resolveParallaxPackName = resolveEventPackName
/** @deprecated Use {@link resolveActiveEventPack} instead. */
export const resolveActiveParallaxPack = resolveActiveEventPack
/** @deprecated Use {@link SeasonalEventWindow} instead. */
export type SeasonalParallaxWindow = SeasonalEventWindow
/** @deprecated Use {@link seasonalEventSchedule} instead. */
export const seasonalParallaxSchedule = seasonalEventSchedule
