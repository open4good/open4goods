/**
 * Editorial block of a category hub (GOU-2716): one YAML file per vertical in
 * `content/category-hubs/`, read by `server/api/category-hubs/[verticalId].get.ts`
 * and rendered by `CategoryHubEditorial.vue`. The format is documented in
 * `content/category-hubs/README.md`.
 */

/** Word range of the intro, checked on every file by `category-hub.content.spec.ts`. */
export const CATEGORY_HUB_INTRO_MIN_WORDS = 120
export const CATEGORY_HUB_INTRO_MAX_WORDS = 180
/** Exact number of visible questions, also the size of the FAQPage JSON-LD. */
export const CATEGORY_HUB_FAQ_SIZE = 4

export interface CategoryHubFaqItem {
  question: string
  answer: string
}

/** Guide of the vertical, by its slug under `guides/<verticalId>/`. */
export interface CategoryHubGuideLink {
  slug: string
  label: string
}

/** Hub file as declared in the `categoryHubs` collection (content.config.ts). */
export interface CategoryHubDoc {
  verticalId: string
  language: string
  published: boolean
  title?: string | null
  intro: string
  faq: CategoryHubFaqItem[]
  subCategories: string[]
  guides: CategoryHubGuideLink[]
}

/** Response of `/api/category-hubs/<verticalId>`: the intro is already rendered to HTML. */
export interface CategoryHubDto {
  verticalId: string
  language: string
  published: boolean
  title: string | null
  introHtml: string
  faq: CategoryHubFaqItem[]
  subCategories: string[]
  guides: CategoryHubGuideLink[]
}

export interface CategoryHubResponse {
  hub: CategoryHubDto | null
}

/**
 * Words of the intro as a reader sees them: link targets, emphasis and heading
 * markers are not counted, only tokens holding a letter or a digit.
 */
export const countCategoryHubIntroWords = (markdown: string): number =>
  markdown
    .replace(/\[([^\]]*)\]\([^)]*\)/g, '$1')
    .replace(/[*_#>`]/g, ' ')
    .split(/\s+/)
    .filter(token => /[\p{L}\p{N}]/u.test(token)).length

/**
 * Format errors of a hub file, empty when it is valid. Used by the content
 * spec (all files) and by the server route, which never serves an invalid file.
 */
export const validateCategoryHubDoc = (doc: CategoryHubDoc): string[] => {
  const errors: string[] = []
  const words = countCategoryHubIntroWords(doc.intro ?? '')
  if (
    words < CATEGORY_HUB_INTRO_MIN_WORDS ||
    words > CATEGORY_HUB_INTRO_MAX_WORDS
  ) {
    errors.push(
      `intro has ${words} words, expected ${CATEGORY_HUB_INTRO_MIN_WORDS} to ${CATEGORY_HUB_INTRO_MAX_WORDS}`
    )
  }
  const faq = doc.faq ?? []
  if (faq.length !== CATEGORY_HUB_FAQ_SIZE) {
    errors.push(
      `faq has ${faq.length} items, expected ${CATEGORY_HUB_FAQ_SIZE}`
    )
  }
  faq.forEach((item, index) => {
    if (!item?.question?.trim() || !item?.answer?.trim()) {
      errors.push(`faq[${index}] needs a question and an answer`)
    }
  })
  return errors
}

/**
 * FAQPage JSON-LD built from the questions displayed on the page, never from
 * another source: no FAQ is declared without being visible.
 */
export const buildCategoryHubFaqJsonLd = (
  faq: ReadonlyArray<CategoryHubFaqItem>
) => {
  const items = faq.filter(item => item.question?.trim() && item.answer?.trim())
  if (!items.length) {
    return null
  }

  return {
    '@context': 'https://schema.org',
    '@type': 'FAQPage',
    mainEntity: items.map(item => ({
      '@type': 'Question',
      name: item.question.trim(),
      acceptedAnswer: {
        '@type': 'Answer',
        text: item.answer.trim(),
      },
    })),
  }
}
