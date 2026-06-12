import { onMounted, ref, toValue, type MaybeRefOrGetter } from 'vue'

export interface ManufacturingSiteDistance {
  country: string
  countryName?: string | null
  city?: string | null
  type?: string | null
  operator?: string | null
  latitude?: number | null
  longitude?: number | null
  distanceKm?: number | null
}

export interface ManufacturingDistance {
  userCountry?: string | null
  userCity?: string | null
  latitude?: number | null
  longitude?: number | null
  sites: ManufacturingSiteDistance[]
}

/**
 * Client-side composable that fetches the per-user user→manufacturing distance
 * AFTER hydration. Kept out of SSR on purpose: the distance is personal and
 * uncacheable, while the manufacturing chain itself is rendered server-side.
 *
 * @param gtin the product GTIN (ref/getter so it can react to route changes)
 */
export function useManufacturingDistance(gtin: MaybeRefOrGetter<number | undefined>) {
  const data = ref<ManufacturingDistance | null>(null)
  const pending = ref(false)
  const error = ref<unknown>(null)

  async function load() {
    const id = toValue(gtin)
    if (!id) {
      return
    }
    pending.value = true
    error.value = null
    try {
      data.value = await $fetch<ManufacturingDistance>(
        `/api/products/${id}/manufacturing-distance`
      )
    } catch (e) {
      // 204 (no resolvable IP / no sites) surfaces as empty; treat as no-op.
      error.value = e
      data.value = null
    } finally {
      pending.value = false
    }
  }

  // Runs only on the client (onMounted is a no-op during SSR).
  onMounted(load)

  /**
   * @returns distance in km for a site matched by country (+ city when present)
   */
  function distanceFor(country?: string | null, city?: string | null): number | null {
    if (!data.value) {
      return null
    }
    const match = data.value.sites.find(
      site =>
        site.country === country &&
        (city == null || site.city == null || site.city === city)
    )
    return match?.distanceKm ?? null
  }

  return { data, pending, error, reload: load, distanceFor }
}
