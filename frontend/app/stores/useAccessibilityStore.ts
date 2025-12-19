import { useStorage } from '@vueuse/core'
import { defineStore } from 'pinia'
import { watch } from 'vue'
import { applyAccessibilityLayout } from '~/utils/accessibilityLayout'

export const useAccessibilityStore = defineStore('accessibility', () => {
  const isZoomed = useStorage('is-zoomed', false)

  const setZoomed = (value: boolean) => {
    isZoomed.value = value
  }

  const toggleZoom = () => {
    isZoomed.value = !isZoomed.value
  }

  watch(
    isZoomed,
    zoomed => {
      applyAccessibilityLayout(zoomed)
    },
    { immediate: true }
  )

  return { isZoomed, setZoomed, toggleZoom }
})
