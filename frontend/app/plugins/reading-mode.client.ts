export default defineNuxtPlugin(() => {
  const readingMode = useReadingMode()

  if (typeof document === 'undefined') {
    return
  }

  const syncClass = (isDyslexic: boolean) => {
    document.documentElement.classList.toggle('is-dyslexic', isDyslexic)
  }

  const storedValue = localStorage.getItem(readingMode.storageKey)
  if (storedValue === 'default' || storedValue === 'dyslexic') {
    readingMode.setMode(storedValue)
  }

  syncClass(readingMode.isDyslexic.value)

  watch(
    readingMode.isDyslexic,
    value => {
      syncClass(value)
    },
    { immediate: true, flush: 'sync' }
  )
})
