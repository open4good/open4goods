import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { computed, defineComponent, h, nextTick, ref } from 'vue'

import ParallaxWidget from './ParallaxWidget.vue'

const scrollY = ref(0)
const motionPreference = ref<'reduce' | 'no-preference'>('no-preference')
const displayWidth = ref(1280)

const windowHeight = ref(1000)
const elementHeight = ref(300)

vi.mock('@vueuse/core', () => ({
  usePreferredReducedMotion: () => motionPreference,
  useWindowScroll: () => ({ x: ref(0), y: scrollY }),
  useWindowSize: () => ({ width: ref(1000), height: windowHeight }),
  useElementBounding: () => ({
    top: computed(() => 350 - scrollY.value),
    height: elementHeight,
    bottom: ref(0),
    left: ref(0),
    right: ref(0),
    width: ref(1000),
    x: ref(0),
    y: ref(0),
    update: () => {},
  }),
}))

vi.mock('vuetify', () => ({
  useDisplay: () => ({ width: displayWidth }),
  useTheme: () => ({
    global: {
      name: ref('light'),
      current: ref({ dark: false }),
    },
  }),
}))

const stubs = {
  VContainer: defineComponent({
    name: 'VContainerStub',
    setup(_, { slots, attrs }) {
      return () =>
        h('div', { class: 'v-container', ...attrs }, slots.default?.())
    },
  }),
}

const mountParallax = (props?: Record<string, unknown>) =>
  mount(ParallaxWidget, {
    props: {
      backgrounds: ['/images/sample.svg'],
      ...props,
    },
    global: {
      stubs,
    },
  })

describe('ParallaxWidget', () => {
  beforeEach(() => {
    scrollY.value = 0
    motionPreference.value = 'no-preference'
    displayWidth.value = 1280
    windowHeight.value = 1000
    elementHeight.value = 300
  })

  it('applies per-layer speeds and blend modes', async () => {
    scrollY.value = 300
    const wrapper = mountParallax({
      backgrounds: [
        { src: '/layers/back.svg', speed: 0.5, blendMode: 'screen' },
        { src: '/layers/front.svg', speed: 0.2 },
      ],
      parallaxAmount: 0.3,
    })

    await nextTick()

    const layers = wrapper.findAll('.parallax-widget__layer')
    expect(layers).toHaveLength(2)
    expect(layers[0].element.style.backgroundImage).toContain('layers/back.svg')
    expect(layers[0].element.style.transform).toBe('translate3d(0, -150px, 0)')
    expect(layers[0].element.style.mixBlendMode).toBe('screen')
    expect(layers[1].element.style.transform).toBe('translate3d(0, -60px, 0)')
  })
  it('reverses parallax direction when reverse is enabled', async () => {
    scrollY.value = 300

    const wrapper = mountParallax({
      backgrounds: [{ src: '/layers/back.svg', speed: 0.5 }],
      parallaxAmount: 0.3,
      reverse: true,
    })

    await nextTick()

    const layer = wrapper.get('.parallax-widget__layer')
    expect(layer.element.style.transform).toBe('translate3d(0, 150px, 0)')
  })


  it('halts parallax when reduced motion is preferred', async () => {
    motionPreference.value = 'reduce'
    scrollY.value = 500

    const wrapper = mountParallax({
      backgrounds: [{ src: '/layers/back.svg', speed: 0.6 }],
      parallaxAmount: 0.2,
    })

    await nextTick()

    const layer = wrapper.get('.parallax-widget__layer')
    expect(layer.element.style.transform).toBe('translate3d(0, 0px, 0)')
  })

  it('disables parallax below the configured breakpoint', async () => {
    displayWidth.value = 640
    scrollY.value = 400

    const wrapper = mountParallax({
      backgrounds: ['/images/parallax.svg'],
      disableParallaxBelow: 960,
      parallaxAmount: 0.4,
    })

    await nextTick()

    const layer = wrapper.get('.parallax-widget__layer')
    expect(layer.element.style.transform).toBe('translate3d(0, 0px, 0)')
  })

  it('caps translation when a max offset ratio is provided', async () => {
    const innerHeightSpy = vi
      .spyOn(window, 'innerHeight', 'get')
      .mockReturnValue(1000)

    scrollY.value = 3000

    const wrapper = mountParallax({
      backgrounds: [{ src: '/images/long-scroll.svg', speed: 1 }],
      parallaxAmount: 1,
      maxOffsetRatio: 0.1,
    })

    await nextTick()

    const layer = wrapper.get('.parallax-widget__layer')
    expect(layer.element.style.transform).toBe('translate3d(0, -100px, 0)')

    innerHeightSpy.mockRestore()
  })
})
