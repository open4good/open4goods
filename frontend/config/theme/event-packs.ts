/**
 * Configuration centralisée des packs événementiels
 *
 * Ce fichier définit :
 * - La liste des packs disponibles
 * - Les périodes d'activation automatique
 * - Les fonctions de résolution
 *
 * @see /frontend/docs/EVENT-PACKS.md pour la documentation complète
 */

// ----------------------------------------------------------------------------
// Types
// ----------------------------------------------------------------------------

export type EventPackName = (typeof EVENT_PACK_NAMES)[number]

export type EventPackSchedule = {
  /** Identifiant unique de la fenêtre */
  id: string
  /** Date de début au format MM-DD (UTC) */
  start: string
  /** Date de fin au format MM-DD (UTC) */
  end: string
  /** Nom du pack à activer */
  pack: EventPackName
  /** Description optionnelle */
  description?: string
}

// ----------------------------------------------------------------------------
// Configuration des packs
// ----------------------------------------------------------------------------

/**
 * Liste des packs événementiels disponibles.
 * 'default' est le pack par défaut utilisé hors période événementielle.
 */
export const EVENT_PACK_NAMES = [
  'default',
  'sdg',
  'bastille-day',
  'hold',
] as const

export const DEFAULT_EVENT_PACK: EventPackName = 'default'

/**
 * Calendrier des périodes événementielles.
 * Les dates sont au format MM-DD (mois-jour) en UTC.
 * Les fenêtres peuvent chevaucher le changement d'année (ex: 12-01 à 01-15).
 */
export const eventPackSchedule: EventPackSchedule[] = [
  {
    id: 'sdg-campaign',
    start: '04-15',
    end: '05-02',
    pack: 'sdg',
    description: 'Journée de la Terre et sensibilisation aux ODD',
  },
  {
    id: 'bastille-day',
    start: '07-10',
    end: '07-16',
    pack: 'bastille-day',
    description: 'Fête nationale du 14 juillet',
  },
  {
    id: 'winter-highlights',
    start: '12-01',
    end: '01-15',
    pack: 'default',
    description: 'Période hivernale sans surcharge événementielle',
  },
]

// ----------------------------------------------------------------------------
// Fonctions de résolution
// ----------------------------------------------------------------------------

/**
 * Valide et normalise un nom de pack depuis une valeur de query string.
 */
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

/**
 * Vérifie si une date est dans une fenêtre événementielle.
 */
const isDateWithinWindow = (
  date: Date,
  window: EventPackSchedule
): boolean => {
  const month = date.getUTCMonth() + 1
  const day = date.getUTCDate()
  const dateStr = `${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}`

  if (window.start <= window.end) {
    // Fenêtre normale (ex: 07-10 à 07-16)
    return dateStr >= window.start && dateStr <= window.end
  } else {
    // Fenêtre chevauchant le changement d'année (ex: 12-01 à 01-15)
    return dateStr >= window.start || dateStr <= window.end
  }
}

/**
 * Détermine le pack événementiel actif pour une date donnée.
 * Retourne DEFAULT_EVENT_PACK si aucune fenêtre ne correspond.
 */
export const resolveActiveEventPack = (
  date: Date = new Date()
): EventPackName => {
  const window = eventPackSchedule.find(candidate =>
    isDateWithinWindow(date, candidate)
  )

  return window?.pack ?? DEFAULT_EVENT_PACK
}

// ----------------------------------------------------------------------------
// Clé i18n pour les packs
// ----------------------------------------------------------------------------

/**
 * Clé de base pour les ressources i18n des packs.
 * Les textes sont accessibles via : packs.{packName}.{path}
 */
export const EVENT_PACK_I18N_BASE_KEY = 'packs'
