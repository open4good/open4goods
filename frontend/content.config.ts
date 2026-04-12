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
        type: z.string().default('guide'),
        tags: z.array(z.string()).default([]),
        icon: z.string().optional(),
        weight: z.number().optional(),
        updatedAt: z.string().optional(),
        draft: z.boolean().default(false),
        published: z.boolean().default(true),
        requiresAuth: z.boolean().default(false),
        layout: z.string().default('default'),
        navigation: z.boolean().default(true),
        ogImage: z.string().optional(),
        noindex: z.boolean().default(false),
      }),
      indexes: [
        { columns: ['path'], unique: true },
        { columns: ['type'] },
        { columns: ['published'] },
        { columns: ['draft'] },
      ],
    }),
  },
  markdown: {
    anchorLinks: {
      depth: 4,
    },
  },
})
