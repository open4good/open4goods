import { ref } from 'vue'

/**
 * Controls FAQ expansion state.
 */
export function useFaqState() {
  const openedPanel = ref<number | null>(0)

  return {
    openedPanel
  }
}
