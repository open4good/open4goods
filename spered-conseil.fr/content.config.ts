import { defineContentConfig, defineCollection, z } from '@nuxt/content'

export default defineContentConfig({
  collections: {
    content: defineCollection({
      type: 'page',
      source: '**/*.md',
      schema: z.object({
        locale: z.enum(['fr', 'en']),
        page: z.string(),
        section: z.string(),
        order: z.number().default(0),
        title: z.string().optional(),
        description: z.string().optional(),
      }),
    }),
  },
})
