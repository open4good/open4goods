import { z } from 'zod'
import { defineCollection, defineContentConfig } from '@nuxt/content'

export default defineContentConfig({
  collections: {
    pages: defineCollection({
      type: 'page',
      source: '**/*.md',
      schema: z.object({
        title: z.string(),
        description: z.string().default(''),
        tags: z.array(z.string()).default([]),
        author: z.string().optional(),
        contributors: z.array(z.string()).default([]),
        scope: z.enum(['public', 'admin']).default('public'),
        seo: z
          .object({
            title: z.string().optional(),
            description: z.string().optional(),
            canonical: z.string().optional(),
            robots: z.string().optional()
          })
          .optional()
      })
    })
  }
})
