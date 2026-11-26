const HEAVY_CHUNK_NAMES = /^(vendor-echarts|vendor-pdf-viewer|echarts-loader)$/

interface ManifestEntryLike {
  name?: string
  preload?: boolean
  prefetch?: boolean
}

/**
 * ECharts and the PDF reader are loaded on demand (GOU-2711): their chunks
 * must never be preloaded or prefetched from the SSR head.
 */
export const stripHeavyChunkPreloads = (
  manifest: Record<string, ManifestEntryLike>
): void => {
  for (const entry of Object.values(manifest)) {
    if (entry.name && HEAVY_CHUNK_NAMES.test(entry.name)) {
      entry.preload = false
      entry.prefetch = false
    }
  }
}
