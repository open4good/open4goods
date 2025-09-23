import { mount, type VueWrapper } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import TheHeroVideo from './The-hero-video.vue'

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

  const createWrapper = (props = {}) => {
    return mount(TheHeroVideo, {
      props: {
        videoSrc: 'test-video.mp4',
        ...props,
      },
      attachTo: document.body,
    })
  }

  describe('Video Loading', () => {
    it('should display loading indicator initially', () => {
      wrapper = createWrapper()

      const loadingIndicator = wrapper.find('.loading-indicator')
      expect(loadingIndicator.exists()).toBe(true)
      expect(loadingIndicator.classes()).not.toContain('hidden')
    })

    it('should display placeholder initially', () => {
      wrapper = createWrapper()

      const placeholder = wrapper.find('.video-placeholder')
      expect(placeholder.exists()).toBe(true)
    })

    it('should render video element with correct attributes', () => {
      wrapper = createWrapper()

      const video = wrapper.find('video')
      expect(video.exists()).toBe(true)
      expect(video.attributes('playsinline')).toBeDefined()
      expect(video.attributes('muted')).toBeDefined()
      expect(video.attributes('loop')).toBeDefined()
      expect(video.attributes('autoplay')).toBeDefined()
      expect(video.attributes('preload')).toBe('metadata')
    })

    it('should set video source correctly', () => {
      const testVideoSrc = 'custom-video.mp4'
      wrapper = createWrapper({ videoSrc: testVideoSrc })

      const videoSource = wrapper.find('source')
      expect(videoSource.attributes('src')).toBe(testVideoSrc)
      expect(videoSource.attributes('type')).toBe('video/mp4')
    })

    it('should use custom poster when provided', () => {
      const customPoster = 'custom-poster.jpg'
      wrapper = createWrapper({ posterUrl: customPoster })

      const video = wrapper.find('video')
      expect(video.attributes('poster')).toBe(customPoster)
    })

    it('should use default poster when not provided', () => {
      wrapper = createWrapper()

      const video = wrapper.find('video')
      expect(video.attributes('poster')).toContain('data:image/svg+xml;base64')
    })

    it('should handle video load event', async () => {
      wrapper = createWrapper()

      const video = wrapper.find('video')
      const videoElement = video.element as HTMLVideoElement

      // Mock readyState to simulate loaded video
      Object.defineProperty(videoElement, 'readyState', {
        writable: true,
        value: 4, // HAVE_ENOUGH_DATA
      })

      // Trigger loadeddata event
      await video.trigger('loadeddata')
      await wrapper.vm.$nextTick()

      // Check that video has loaded class
      expect(video.classes()).toContain('loaded')
    })

    it('should handle video error', async () => {
      wrapper = createWrapper()

      const video = wrapper.find('video')

      // Trigger error event
      await video.trigger('error')
      await wrapper.vm.$nextTick()

      // Check that video is hidden on error
      expect(wrapper.vm.isLoadingError).toBe(true)
      expect(wrapper.find('video').exists()).toBe(false)

      // Check that placeholder is still visible
      const placeholder = wrapper.find('.video-placeholder')
      expect(placeholder.exists()).toBe(true)
    })

    it('should hide loading indicator after video loads', async () => {
      wrapper = createWrapper()

      const video = wrapper.find('video')

      // Trigger loadeddata event
      await video.trigger('loadeddata')
      await wrapper.vm.$nextTick()

      const loadingIndicator = wrapper.find('.loading-indicator')
      expect(loadingIndicator.classes()).toContain('hidden')
    })

    it('should setup IntersectionObserver on mount', () => {
      wrapper = createWrapper()

      expect(mockIntersectionObserver).toHaveBeenCalledWith(
        expect.any(Function),
        { threshold: 0.5 }
      )
    })

    it('should handle already cached video', async () => {
      wrapper = createWrapper()

      const video = wrapper.find('video')
      const videoElement = video.element as HTMLVideoElement

      // Mock readyState to simulate cached video
      Object.defineProperty(videoElement, 'readyState', {
        writable: true,
        value: 4, // HAVE_ENOUGH_DATA
      })

      // Simulate the onMounted logic for cached video
      wrapper.vm.onVideoLoaded()
      wrapper.vm.attemptAutoplay()

      await wrapper.vm.$nextTick()

      // Check that video is marked as loaded
      expect(wrapper.vm.isVideoLoaded).toBe(true)
      expect(wrapper.vm.showPlaceholder).toBe(false)
    })
  })

  describe('Component Structure', () => {
    it('should render hero content with slots', () => {
      wrapper = createWrapper()

      const heroContent = wrapper.find('.hero-content')
      expect(heroContent.exists()).toBe(true)

      const title = wrapper.find('.hero-title')
      const subtitle = wrapper.find('.hero-subtitle')
      const cta = wrapper.find('.hero-cta')

      expect(title.exists()).toBe(true)
      expect(subtitle.exists()).toBe(true)
      expect(cta.exists()).toBe(true)
    })

    it('should render overlay', () => {
      wrapper = createWrapper()

      const overlay = wrapper.find('.hero-overlay')
      expect(overlay.exists()).toBe(true)
    })
  })

  describe('Cleanup', () => {
    it('should cleanup event listeners on unmount', () => {
      wrapper = createWrapper()

      const video = wrapper.find('video')
      const videoElement = video.element as HTMLVideoElement

      const removeEventListenerSpy = vi.spyOn(videoElement, 'removeEventListener')

      wrapper.unmount()

      expect(removeEventListenerSpy).toHaveBeenCalledTimes(4)
    })
  })

  afterEach(() => {
    if (wrapper) {
      wrapper.unmount()
    }
  })
})