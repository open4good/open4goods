import { onBeforeUnmount, onMounted, ref, type Ref } from 'vue'

/**
 * Reveals section blocks when they enter the viewport.
 * Starts visible on SSR so initial HTML is correct for SEO and avoids blank-section flashes.
 * On the client, elements already in the viewport stay visible immediately; off-screen
 * elements transition in as the user scrolls.
 */
export function useScrollReveal(prefersReducedMotion: Ref<boolean>) {
  const target = ref<HTMLElement | null>(null)
  const isVisible = ref(true) // SSR-safe default: always visible in initial HTML
  let observer: IntersectionObserver | null = null

  onMounted(() => {
    if (prefersReducedMotion.value || !target.value) {
      isVisible.value = true
      return
    }

    const rect = target.value.getBoundingClientRect()
    const alreadyInView = rect.top < window.innerHeight * 0.94

    if (alreadyInView) {
      return
    }

    isVisible.value = false

    observer = new IntersectionObserver(
      (entries) => {
        const [entry] = entries
        if (entry?.isIntersecting) {
          isVisible.value = true
          observer?.disconnect()
        }
      },
      {
        threshold: 0.12,
        rootMargin: '0px 0px -5% 0px'
      }
    )

    observer.observe(target.value)
  })

  onBeforeUnmount(() => {
    observer?.disconnect()
  })

  return {
    target,
    isVisible
  }
}
