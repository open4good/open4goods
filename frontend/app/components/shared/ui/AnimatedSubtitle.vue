<template>
  <component
    :is="tag"
    v-if="isVisible && resolvedText"
    class="animated-subtitle"
    :class="animationClass"
    :style="animationStyle"
    v-bind="$attrs"
  >
    {{ resolvedText }}
  </component>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'

type AnimationPreset = 'fade' | 'fade-up' | 'fade-down' | 'fade-scale' | 'fade-blur'

type I18nValues = Record<string, string | number>

defineOptions({
  inheritAttrs: false,
})

const props = withDefaults(
  defineProps<{
    i18nKey?: string
    text?: string
    values?: I18nValues
    delay?: number
    durationMs?: number
    animation?: AnimationPreset | string
    tag?: keyof HTMLElementTagNameMap
  }>(),
  {
    i18nKey: undefined,
    text: undefined,
    values: undefined,
    delay: 0,
    durationMs: 420,
    animation: 'fade',
    tag: 'p',
  }
)

const { t } = useI18n()

const isVisible = ref(false)
let timeoutId: ReturnType<typeof setTimeout> | null = null

const resolvedText = computed(() => {
  if (props.text && props.text.trim().length > 0) {
    return props.text
  }

  if (props.i18nKey) {
    return t(props.i18nKey, props.values)
  }

  return ''
})

const animationClass = computed(() => {
  if (!props.animation) {
    return null
  }

  return `animated-subtitle--${props.animation}`
})

const animationStyle = computed(() => ({
  '--animated-subtitle-duration': `${props.durationMs}ms`,
}))

const reveal = () => {
  if (timeoutId) {
    clearTimeout(timeoutId)
  }

  timeoutId = setTimeout(() => {
    isVisible.value = true
  }, Math.max(props.delay ?? 0, 0))
}

onMounted(() => {
  reveal()
})

onBeforeUnmount(() => {
  if (timeoutId) {
    clearTimeout(timeoutId)
    timeoutId = null
  }
})
</script>

<style scoped>
.animated-subtitle {
  margin: 0;
  animation-duration: var(--animated-subtitle-duration, 420ms);
  animation-timing-function: ease-out;
  animation-fill-mode: both;
  opacity: 0;
}

.animated-subtitle--fade {
  animation-name: animated-subtitle-fade;
}

.animated-subtitle--fade-up {
  animation-name: animated-subtitle-fade-up;
}

.animated-subtitle--fade-down {
  animation-name: animated-subtitle-fade-down;
}

.animated-subtitle--fade-scale {
  animation-name: animated-subtitle-fade-scale;
}

.animated-subtitle--fade-blur {
  animation-name: animated-subtitle-fade-blur;
}

@keyframes animated-subtitle-fade {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes animated-subtitle-fade-up {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes animated-subtitle-fade-down {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes animated-subtitle-fade-scale {
  from {
    opacity: 0;
    transform: scale(0.98);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

@keyframes animated-subtitle-fade-blur {
  from {
    opacity: 0;
    filter: blur(6px);
  }
  to {
    opacity: 1;
    filter: blur(0);
  }
}

@media (prefers-reduced-motion: reduce) {
  .animated-subtitle {
    animation: none;
    opacity: 1;
    transform: none;
    filter: none;
  }
}
</style>
