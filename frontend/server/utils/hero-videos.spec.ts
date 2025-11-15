import { afterEach, describe, expect, it, vi } from 'vitest'

type MockDirent = {
  name: string
  isFile: () => boolean
}

const createDirent = (name: string, isFile = true): MockDirent => ({
  name,
  isFile: () => isFile,
})

afterEach(() => {
  vi.resetModules()
  vi.unmock('node:fs/promises')
})

describe('getHeroVideoSources', () => {
  it('returns cached results when called repeatedly', async () => {
    const readdirMock = vi.fn().mockResolvedValue([
      createDirent('first.mp4'),
      createDirent('ignored.txt'),
    ])

    vi.doMock('node:fs/promises', () => ({
      readdir: readdirMock,
      default: { readdir: readdirMock },
    }))

    const { getHeroVideoSources } = await import('~~/server/utils/hero-videos')

    const firstCall = await getHeroVideoSources()
    const secondCall = await getHeroVideoSources()

    expect(firstCall).toEqual(['/videos/first.mp4'])
    expect(secondCall).toEqual(firstCall)
    expect(readdirMock).toHaveBeenCalledTimes(1)
  })
})
