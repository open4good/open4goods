import { readdir } from 'node:fs/promises'
import { join } from 'node:path'

const HERO_VIDEOS_DIRECTORY = join(process.cwd(), 'app/public/videos')
const HERO_VIDEO_EXTENSIONS = ['.mp4']
export const HERO_VIDEOS_CACHE_TTL_MS = 1000 * 60 * 60 // 1 hour

type HeroVideosCacheEntry = {
  videos: string[]
  lastUpdated: number
}

let heroVideosCache: HeroVideosCacheEntry | null = null
let inflightReadPromise: Promise<string[]> | null = null

const hasValidCache = (now: number) => {
  if (!heroVideosCache) {
    return false
  }

  if (now - heroVideosCache.lastUpdated > HERO_VIDEOS_CACHE_TTL_MS) {
    heroVideosCache = null
    return false
  }

  return true
}

const extractHeroVideoPaths = (entries: Array<{ isFile: () => boolean; name: string }>): string[] =>
  entries
    .filter((entry) => entry.isFile() && HERO_VIDEO_EXTENSIONS.some((extension) => entry.name.toLowerCase().endsWith(extension)))
    .map((entry) => `/videos/${entry.name}`)

const discoverHeroVideos = async (): Promise<string[]> => {
  const entries = await readdir(HERO_VIDEOS_DIRECTORY, { withFileTypes: true })
  return extractHeroVideoPaths(entries)
}

const loadHeroVideos = async () => {
  try {
    const videos = await discoverHeroVideos()
    heroVideosCache = {
      videos,
      lastUpdated: Date.now(),
    }
    return videos
  } catch (error) {
    if (import.meta.dev) {
      console.warn('Unable to select hero video from directory', error)
    }

    heroVideosCache = {
      videos: [],
      lastUpdated: Date.now(),
    }

    return []
  } finally {
    inflightReadPromise = null
  }
}

export const getHeroVideoSources = async (): Promise<string[]> => {
  const now = Date.now()

  if (hasValidCache(now)) {
    return heroVideosCache!.videos
  }

  if (!inflightReadPromise) {
    inflightReadPromise = loadHeroVideos()
  }

  return inflightReadPromise
}
