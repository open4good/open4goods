import { computed, onMounted, ref } from 'vue'
import { useDisplay } from 'vuetify'

/**
 * Hydration-safe wrapper around Vuetify's `useDisplay()`.
 *
 * On the server (no viewport) Vuetify reports a small screen, while the client
 * reads the real viewport during hydration. Branching the template on
 * `mdAndDown` therefore produces a different tree on desktop and Vue logs
 * "Hydration completed but contains mismatches".
 *
 * Until the component is mounted, `mdAndDown` keeps the value rendered by the
 * server (`true`); afterwards it follows the real viewport. The SSR HTML is
 * unchanged.
 */
export const useHydrationSafeDisplay = () => {
  const display = useDisplay()
  const mounted = ref(false)

  onMounted(() => {
    mounted.value = true
  })

  const mdAndDown = computed(() =>
    mounted.value ? display.mdAndDown.value : true
  )

  return { mdAndDown }
}
