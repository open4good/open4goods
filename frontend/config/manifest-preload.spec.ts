import { describe, expect, it } from 'vitest'
import { stripHeavyChunkPreloads } from './manifest-preload'

describe('stripHeavyChunkPreloads', () => {
  it('disables preload and prefetch of echarts and pdf chunks only', () => {
    const manifest = {
      a: { name: 'vendor-echarts', preload: true, prefetch: true },
      b: { name: 'vendor-pdf-viewer', preload: true, prefetch: true },
      c: { name: 'echarts-loader', preload: true, prefetch: true },
      d: { name: 'ProductPage', preload: true, prefetch: true },
    }
    stripHeavyChunkPreloads(manifest)
    expect(manifest.a).toMatchObject({ preload: false, prefetch: false })
    expect(manifest.b).toMatchObject({ preload: false, prefetch: false })
    expect(manifest.c).toMatchObject({ preload: false, prefetch: false })
    expect(manifest.d).toMatchObject({ preload: true, prefetch: true })
  })
})
