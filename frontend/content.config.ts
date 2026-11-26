import { fileURLToPath } from 'node:url'
import { defineCollection, defineContentConfig, z } from '@nuxt/content'

const DOCS_DIR = fileURLToPath(new URL('../docs', import.meta.url))
const GUIDES_DIR = fileURLToPath(
  new URL('../verticals/src/main/resources/guides', import.meta.url)
)
const BLOG_DIR = fileURLToPath(new URL('./content/blog', import.meta.url))
const CATEGORY_HUBS_DIR = fileURLToPath(
  new URL('./content/category-hubs', import.meta.url)
)

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
        // Input of `pnpm blog:cover <slug>`, which draws `image` (scripts/blog-cover/blog-cover.mjs
        // validates it further). Not read by the site itself.
        cover: z
          .object({
            kicker: z.string().optional(),
            title: z.string(),
            figure: z.string().optional(),
            caption: z.string().optional(),
            source: z.string().optional(),
            icon: z.string().optional(),
            ratio: z.number().min(0).max(1).optional(),
            ratioLabel: z.string().optional(),
          })
          .optional(),
        // Filled by Nuxt Content with the source file's raw Markdown, so the deployed .output bundle
        // can render BlogPostDto.body without content/ next to it (see server/utils/blog-content.ts).
        rawbody: z.string(),
      }),
      indexes: [
        { columns: ['path'], unique: true },
        { columns: ['language'] },
        { columns: ['published'] },
        { columns: ['draft'] },
        { columns: ['date'] },
      ],
    }),
    // Editorial block of a category hub, one file per vertical (GOU-2716). Format and word range:
    // content/category-hubs/README.md, checked by shared/utils/category-hub.content.spec.ts.
    categoryHubs: defineCollection({
      type: 'data',
      source: { include: '*.yml', cwd: CATEGORY_HUBS_DIR },
      schema: z.object({
        verticalId: z.string(),
        language: z.string().default('fr'),
        published: z.boolean().default(false),
        title: z.string().optional(),
        intro: z.string(),
        faq: z
          .array(z.object({ question: z.string(), answer: z.string() }))
          .default([]),
        subCategories: z.array(z.string()).default([]),
        guides: z
          .array(z.object({ slug: z.string(), label: z.string() }))
          .default([]),
      }),
      indexes: [{ columns: ['verticalId', 'language'] }],
    }),
    docs: defineCollection({
      type: 'page',
      source: [
        { include: '**/*.md', cwd: DOCS_DIR, prefix: 'docs' },
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
