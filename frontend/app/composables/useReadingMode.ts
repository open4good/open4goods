export type ReadingMode = 'default' | 'dyslexic'

const STORAGE_KEY = 'reading-mode'

const isBrowser = (): boolean =>
  typeof window !== 'undefined' && typeof localStorage !== 'undefined'

/**
 * Reading mode persists a user preference (default/dyslexic) and can be extended
 * by adding new union values and mapping them to CSS classes in the plugin.
 */
export const useReadingMode = () => {
  const mode = useState<ReadingMode>('reading-mode', () => {
    if (isBrowser()) {
      const storedValue = localStorage.getItem(STORAGE_KEY)

      if (storedValue === 'default' || storedValue === 'dyslexic') {
        return storedValue
      }
    }

    return 'default'
  })
  const hasWatcher = useState('reading-mode-watcher', () => false)

  if (isBrowser()) {
    const storedValue = localStorage.getItem(STORAGE_KEY)

    if (storedValue === 'default' || storedValue === 'dyslexic') {
      mode.value = storedValue
    }
  }

  if (isBrowser() && !hasWatcher.value) {
    watch(
      mode,
      value => {
        localStorage.setItem(STORAGE_KEY, value)
      },
      { immediate: true, flush: 'sync' }
    )
    hasWatcher.value = true
  }

  const isDyslexic = computed(() => mode.value === 'dyslexic')
  const setMode = (value: ReadingMode) => {
    mode.value = value
  }
  const toggle = () => {
    mode.value = isDyslexic.value ? 'default' : 'dyslexic'
  }

  return {
    mode,
    isDyslexic,
    setMode,
    toggle,
    storageKey: STORAGE_KEY,
  }
}
