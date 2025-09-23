import { mountSuspended } from '@nuxt/test-utils/runtime'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { VueWrapper } from '@vue/test-utils'
import TheHeroVideo from './The-hero-video.vue'

// Mock vue-i18n
vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    t: (key: string) => {
      const translations: Record<string, string> = {
        'siteIdentity.hero.mainTitle': 'Test Main Title',
        'siteIdentity.hero.mainSubtitle': 'Test Main Subtitle'
      }
      return translations[key] || key
    }
  })
}))

// Mock IntersectionObserver
const mockIntersectionObserver = vi.fn()
mockIntersectionObserver.mockReturnValue({
  observe: vi.fn(),
  unobserve: vi.fn(),
  disconnect: vi.fn(),
})

// Setup DOM environment
Object.defineProperty(global, 'IntersectionObserver', {
  writable: true,
  configurable: true,
  value: mockIntersectionObserver,
})

// Mock HTMLVideoElement methods
Object.defineProperty(HTMLVideoElement.prototype, 'play', {
  writable: true,
  value: vi.fn().mockResolvedValue(undefined),
})

Object.defineProperty(HTMLVideoElement.prototype, 'pause', {
  writable: true,
  value: vi.fn(),
})

describe('TheHeroVideo', () => {
  let wrapper: VueWrapper<InstanceType<typeof TheHeroVideo>>

  beforeEach(() => {
    vi.clearAllMocks()
  })

  const createWrapper = async (props = {}) => {
    return await mountSuspended(TheHeroVideo, {
      props: {
        videoSrc: 'test-video.mp4',
        ...props,
      },
    })
  }

  describe('Video Loading', () => {
    it('should display loading indicator initially', async () => {
      wrapper = await createWrapper()

      const loadingIndicator = wrapper.find('.loading-indicator')
      expect(loadingIndicator.exists()).toBe(true)
      expect(loadingIndicator.classes()).not.toContain('hidden')
    })

    it('should display placeholder initially', async () => {
      wrapper = await createWrapper()

      const placeholder = wrapper.find('.video-placeholder')
      expect(placeholder.exists()).toBe(true)
    })

    it('should render video element with correct attributes', async () => {
      wrapper = await createWrapper()

      const video = wrapper.find('video')
      expect(video.exists()).toBe(true)
      expect(video.attributes('playsinline')).toBeDefined()
      expect(video.attributes('muted')).toBeDefined()
      expect(video.attributes('loop')).toBeDefined()
      expect(video.attributes('autoplay')).toBeDefined()
      expect(video.attributes('preload')).toBe('none')
    })

    it('should set video source correctly', async () => {
      const testVideoSrc = 'custom-video.mp4'
      wrapper = await createWrapper({ videoSrc: testVideoSrc })

      const videoSource = wrapper.find('source')
      expect(videoSource.attributes('src')).toBe(testVideoSrc)
      expect(videoSource.attributes('type')).toBe('video/mp4')
    })

    it('should use custom poster when provided', async () => {
      const customPoster = 'custom-poster.jpg'
      wrapper = await createWrapper({ posterUrl: customPoster })

      const video = wrapper.find('video')
      expect(video.attributes('poster')).toBe(customPoster)
    })

    it('should use empty poster when not provided', async () => {
      wrapper = await createWrapper()

      const video = wrapper.find('video')
      expect(video.attributes('poster')).toBe('')
    })

    it('should handle video load event', async () => {
      wrapper = await createWrapper()

      const video = wrapper.find('video')
      const videoElement = video.element as HTMLVideoElement

      // Mock readyState to simulate loaded video
      Object.defineProperty(videoElement, 'readyState', {
        writable: true,
        value: 4, // HAVE_ENOUGH_DATA
      })

      // Trigger loadstart event (which the component actually listens to)
      await video.trigger('loadstart')
      await wrapper.vm.$nextTick()

      // Check that video has loaded class
      expect(video.classes()).toContain('loaded')
    })

    it('should handle video error', async () => {
      wrapper = await createWrapper()

      const video = wrapper.find('video')

      // Trigger error event
      await video.trigger('error')
      await wrapper.vm.$nextTick()

      // Check that video is hidden on error
      expect(wrapper.find('video').exists()).toBe(false)

      // Check that placeholder is still visible
      const placeholder = wrapper.find('.video-placeholder')
      expect(placeholder.exists()).toBe(true)
    })

    it('should hide loading indicator after video loads', async () => {
      wrapper = await createWrapper()

      const video = wrapper.find('video')

      // Trigger loadstart event (which the component actually listens to)
      await video.trigger('loadstart')
      await wrapper.vm.$nextTick()

      const loadingIndicator = wrapper.find('.loading-indicator')
      expect(loadingIndicator.classes()).toContain('hidden')
    })

    it('should setup IntersectionObserver on mount', async () => {
      wrapper = await createWrapper()

      expect(mockIntersectionObserver).toHaveBeenCalledWith(
        expect.any(Function),
        { threshold: 0.5 }
      )
    })

    it('should handle already cached video', async () => {
      wrapper = await createWrapper()

      const video = wrapper.find('video')
      const videoElement = video.element as HTMLVideoElement

      // Mock readyState to simulate cached video
      Object.defineProperty(videoElement, 'readyState', {
        writable: true,
        value: 4, // HAVE_ENOUGH_DATA
      })

      // Simulate the onMounted logic for cached video
      const componentInstance = wrapper.getCurrentComponent().exposed
      expect(componentInstance).toBeTruthy()

      componentInstance!.onVideoLoaded()
      componentInstance!.attemptAutoplay()

      await wrapper.vm.$nextTick()

      // Check that video is marked as loaded
      expect(componentInstance!.isVideoLoaded.value).toBe(true)
      expect(componentInstance!.showPlaceholder.value).toBe(false)
    })
  })

  describe('Component Structure', () => {
    it('should render hero title component', async () => {
      wrapper = await createWrapper()

      const heroTitleContainer = wrapper.find('.hero-title-container')
      expect(heroTitleContainer.exists()).toBe(true)

      const title = wrapper.find('.hero-title-text')
      const subtitle = wrapper.find('.hero-subtitle-text')

      expect(title.exists()).toBe(true)
      expect(subtitle.exists()).toBe(true)
    })

    it('should render overlay', async () => {
      wrapper = await createWrapper()

      const overlay = wrapper.find('.hero-overlay')
      expect(overlay.exists()).toBe(true)
    })
  })

  describe('Cleanup', () => {
    it('should cleanup event listeners on unmount', async () => {
      wrapper = await createWrapper()

      const video = wrapper.find('video')
      const videoElement = video.element as HTMLVideoElement

      const removeEventListenerSpy = vi.spyOn(videoElement, 'removeEventListener')

      wrapper.unmount()

      expect(removeEventListenerSpy).toHaveBeenCalledTimes(3)
    })
  })

  afterEach(() => {
    if (wrapper) {
      wrapper.unmount()
    }
  })
})