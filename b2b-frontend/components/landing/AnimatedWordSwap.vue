<template>
  <span class="animated-word-swap" :class="`animated-word-swap--${activeEffect}`">
    <span class="animated-word-swap__measure" aria-hidden="true">{{ longestWord }}</span>
    <span
      v-if="leavingWord"
      :key="`leave-${generation}`"
      class="animated-word-swap__layer animated-word-swap__layer--leave"
      aria-hidden="true"
    >
      <span
        v-for="(char, index) in leavingChars"
        :key="`leave-${generation}-${index}`"
        class="animated-word-swap__char"
        :style="charStyle(index)"
      >{{ renderChar(char) }}</span>
    </span>
    <span
      :key="`enter-${generation}`"
      class="animated-word-swap__layer animated-word-swap__layer--enter"
      :aria-label="currentWord"
    >
      <span
        v-for="(char, index) in currentChars"
        :key="`enter-${generation}-${index}`"
        class="animated-word-swap__char"
        :style="charStyle(index)"
        aria-hidden="true"
      >{{ renderChar(char) }}</span>
    </span>
  </span>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import type { CSSProperties } from 'vue'

type AnimatedWordEffect = 'rise' | 'fade' | 'flip'

const props = withDefaults(defineProps<{
  words: string[]
  word?: string
  interval?: number
  duration?: number
  stagger?: number
  delay?: number
  effect?: AnimatedWordEffect
  effects?: AnimatedWordEffect[]
  paused?: boolean
}>(), {
  word: undefined,
  interval: 3200,
  duration: 520,
  stagger: 28,
  delay: 0,
  effect: 'rise',
  effects: () => [],
  paused: false
})

const activeIndex = ref(0)
const previousWord = ref('')
const leavingWord = ref('')
const generation = ref(0)
let intervalTimer: ReturnType<typeof setInterval> | undefined
let delayTimer: ReturnType<typeof setTimeout> | undefined
let leaveTimer: ReturnType<typeof setTimeout> | undefined

const isControlled = computed(() => typeof props.word === 'string')
const sanitizedWords = computed(() => props.words.map((word) => word.trim()).filter(Boolean))
const currentWord = computed(() => isControlled.value ? props.word?.trim() ?? '' : sanitizedWords.value[activeIndex.value] ?? '')
const longestWord = computed(() => sanitizedWords.value.reduce((longest, word) => word.length > longest.length ? word : longest, currentWord.value))
const currentChars = computed(() => splitGraphemes(currentWord.value))
const leavingChars = computed(() => splitGraphemes(leavingWord.value))
const activeEffect = computed(() => {
  const effects = props.effects?.filter(Boolean)
  if (effects?.length) {
    return effects[generation.value % effects.length]
  }

  return props.effect
})

function charStyle(index: number): CSSProperties {
  return {
    '--char-index': String(index),
    '--word-duration': `${props.duration}ms`,
    '--word-stagger': `${props.stagger}ms`
  } as CSSProperties
}

function renderChar(char: string) {
  return char === ' ' ? '\u00A0' : char
}

function cycleWord() {
  if (isControlled.value || sanitizedWords.value.length <= 1 || props.paused) {
    return
  }

  previousWord.value = currentWord.value
  activeIndex.value = (activeIndex.value + 1) % sanitizedWords.value.length
  leavingWord.value = previousWord.value
  generation.value += 1

  if (leaveTimer) {
    clearTimeout(leaveTimer)
  }

  const visibleChars = Math.max(splitGraphemes(previousWord.value).length, currentChars.value.length)
  leaveTimer = setTimeout(() => {
    leavingWord.value = ''
  }, props.duration + visibleChars * props.stagger)
}

function start() {
  stop()
  activeIndex.value = 0
  previousWord.value = ''
  leavingWord.value = ''
  generation.value += 1

  if (isControlled.value || sanitizedWords.value.length <= 1 || props.paused) {
    return
  }

  delayTimer = setTimeout(() => {
    cycleWord()
    intervalTimer = setInterval(cycleWord, props.interval)
  }, props.interval + props.delay)
}

function stop() {
  if (intervalTimer) {
    clearInterval(intervalTimer)
    intervalTimer = undefined
  }

  if (delayTimer) {
    clearTimeout(delayTimer)
    delayTimer = undefined
  }

  if (leaveTimer) {
    clearTimeout(leaveTimer)
    leaveTimer = undefined
  }
}

function splitGraphemes(value: string) {
  const Segmenter = Intl.Segmenter
  if (Segmenter) {
    return Array.from(new Segmenter(undefined, { granularity: 'grapheme' }).segment(value), (part) => part.segment)
  }

  return Array.from(value)
}

onMounted(start)
watch(() => [props.words, props.interval, props.duration, props.stagger, props.delay, props.effect, props.effects, props.paused] as const, start, { deep: true })
watch(() => props.word, (nextWord, previousControlledWord) => {
  if (!isControlled.value || props.paused || nextWord === previousControlledWord) {
    return
  }

  leavingWord.value = previousControlledWord?.trim() ?? ''
  generation.value += 1

  if (leaveTimer) {
    clearTimeout(leaveTimer)
  }

  const visibleChars = Math.max(splitGraphemes(leavingWord.value).length, currentChars.value.length)
  leaveTimer = setTimeout(() => {
    leavingWord.value = ''
  }, props.duration + visibleChars * props.stagger)
})

onBeforeUnmount(stop)
</script>

<style scoped lang="scss">
.animated-word-swap {
  position: relative;
  display: inline-grid;
  vertical-align: baseline;
  white-space: nowrap;
}

.animated-word-swap__measure,
.animated-word-swap__layer {
  grid-area: 1 / 1;
}

.animated-word-swap__measure {
  visibility: hidden;
}

.animated-word-swap__layer {
  display: inline-block;
}

.animated-word-swap__char {
  display: inline-block;
  animation-duration: var(--word-duration);
  animation-fill-mode: both;
  animation-delay: calc(var(--char-index) * var(--word-stagger));
  animation-timing-function: cubic-bezier(0.2, 0.8, 0.2, 1);
  will-change: opacity, transform, filter;
}

.animated-word-swap__layer--enter .animated-word-swap__char {
  animation-name: word-rise-in;
}

.animated-word-swap__layer--leave .animated-word-swap__char {
  animation-name: word-rise-out;
}

.animated-word-swap--fade {
  .animated-word-swap__layer--enter .animated-word-swap__char {
    animation-name: word-fade-in;
  }

  .animated-word-swap__layer--leave .animated-word-swap__char {
    animation-name: word-fade-out;
  }
}

.animated-word-swap--flip {
  .animated-word-swap__char {
    transform-origin: 50% 62%;
  }

  .animated-word-swap__layer--enter .animated-word-swap__char {
    animation-name: word-flip-in;
  }

  .animated-word-swap__layer--leave .animated-word-swap__char {
    animation-name: word-flip-out;
  }
}

@keyframes word-rise-in {
  from {
    opacity: 0;
    filter: blur(4px);
    transform: translateY(0.34em);
  }

  to {
    opacity: 1;
    filter: blur(0);
    transform: translateY(0);
  }
}

@keyframes word-rise-out {
  from {
    opacity: 1;
    filter: blur(0);
    transform: translateY(0);
  }

  to {
    opacity: 0;
    filter: blur(4px);
    transform: translateY(-0.28em);
  }
}

@keyframes word-fade-in {
  from {
    opacity: 0;
  }

  to {
    opacity: 1;
  }
}

@keyframes word-fade-out {
  from {
    opacity: 1;
  }

  to {
    opacity: 0;
  }
}

@keyframes word-flip-in {
  from {
    opacity: 0;
    transform: rotateX(-82deg) translateY(0.18em);
  }

  to {
    opacity: 1;
    transform: rotateX(0deg) translateY(0);
  }
}

@keyframes word-flip-out {
  from {
    opacity: 1;
    transform: rotateX(0deg) translateY(0);
  }

  to {
    opacity: 0;
    transform: rotateX(82deg) translateY(-0.12em);
  }
}

@media (prefers-reduced-motion: reduce) {
  .animated-word-swap__char {
    animation: none !important;
  }
}
</style>
