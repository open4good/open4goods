import { fileURLToPath } from 'node:url'
import { defineCollection, defineContentConfig, z } from '@nuxt/content'

const DOCS_DIR = fileURLToPath(new URL('../docs', import.meta.url))
const GUIDES_DIR = fileURLToPath(
  new URL('../verticals/src/main/resources/guides', import.meta.url)
)
const BLOG_DIR = fileURLToPath(new URL('./content/blog', import.meta.url))

export default defineContentConfig({
  collections: {
    blog: defineCollection({
      type: 'page',
      source: { include: '**/*.md', cwd: BLOG_DIR, prefix: 'blog' },
      schema: z.object({
        title: z.string(),
        description: z.string().default(''),
        author: z.string(),
        language: z.string().default('fr'),
        tags: z.array(z.string()).default([]),
        date: z.string().nullable(),
        updatedAt: z.string().nullable(),
        draft: z.boolean().default(false),
        published: z.boolean().default(true),
        image: z.string().optional(),
      }),
      indexes: [
        { columns: ['path'], unique: true },
        { columns: ['language'] },
        { columns: ['published'] },
        { columns: ['draft'] },
        { columns: ['date'] },
      ],
    }),
    docs: defineCollection({
      type: 'page',
      source: [
        { include: 'en/**/*.md', cwd: DOCS_DIR, prefix: 'docs/en' },
        { include: 'fr/**/*.md', cwd: DOCS_DIR, prefix: 'docs/fr' },
        { include: '**/*.md', cwd: GUIDES_DIR, prefix: 'guides' },
      ],
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
