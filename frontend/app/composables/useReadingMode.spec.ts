import { beforeEach, describe, expect, it, vi } from 'vitest'

const getReadingMode = async () => {
  const { useReadingMode } = await import('./useReadingMode')
  return useReadingMode()
}

describe('useReadingMode', () => {
  beforeEach(() => {
    vi.resetModules()
    localStorage.clear()
  })

  it('defaults to the standard reading mode', async () => {
    const { mode, isDyslexic } = await getReadingMode()

    expect(mode.value).toBe('default')
    expect(isDyslexic.value).toBe(false)
  })

  it('restores and persists the dyslexic mode preference', async () => {
    localStorage.setItem('reading-mode', 'dyslexic')

    const { mode, isDyslexic, toggle } = await getReadingMode()

    expect(mode.value).toBe('dyslexic')
    expect(isDyslexic.value).toBe(true)

    toggle()

    expect(mode.value).toBe('default')
    expect(localStorage.getItem('reading-mode')).toBe('default')
  })
})
