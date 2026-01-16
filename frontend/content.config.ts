import { fileURLToPath } from 'node:url'
import { defineCollection, defineContentConfig, z } from '@nuxt/content'

const DOCS_DIR = fileURLToPath(new URL('../docs', import.meta.url))

export default defineContentConfig({
  sources: {
    docs: {
      driver: 'fs',
      base: DOCS_DIR,
      prefix: '/docs',
      extensions: ['.md'],
    },
  },
  collections: {
    docs: defineCollection({
      type: 'page',
      source: 'docs',
      schema: z.object({
        title: z.string().optional(),
        description: z.string().optional(),
        tags: z.array(z.string()).optional(),
        icon: z.string().optional(),
        weight: z.number().optional(),
        updatedAt: z.string().optional(),
        draft: z.boolean().optional(),
      }),
    }),
  },
  markdown: {
    anchorLinks: {
      depth: 4,
    },
  },
})
