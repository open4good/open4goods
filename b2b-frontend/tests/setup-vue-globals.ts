/**
 * Provides Vue composition API globals for component tests.
 * In the real app, these are auto-imported by Nuxt. In plain @vue/test-utils tests
 * they must be available on the global scope.
 */
import {
  ref,
  computed,
  watch,
  watchEffect,
  reactive,
  readonly,
  toRef,
  toRefs,
  isRef,
  unref,
  shallowRef,
  shallowReactive,
  markRaw,
  nextTick,
  onMounted,
  onUnmounted,
  onBeforeMount,
  onBeforeUnmount,
  defineComponent,
} from 'vue'

Object.assign(globalThis, {
  ref,
  computed,
  watch,
  watchEffect,
  reactive,
  readonly,
  toRef,
  toRefs,
  isRef,
  unref,
  shallowRef,
  shallowReactive,
  markRaw,
  nextTick,
  onMounted,
  onUnmounted,
  onBeforeMount,
  onBeforeUnmount,
  defineComponent,
})
