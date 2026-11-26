# Category hub editorial blocks

One YAML file per vertical renders an editorial block at the bottom of its
category hub (`/televiseurs`, `/lave-linge`...), in the HTML served by the
server (GOU-2716). Sub-category pages do not show it. A vertical without a
file, or with `published: false`, is unchanged in production.

## Format

File name: `<verticalId>.yml` for French (`tv.yml`, `washing-machine.yml`;
ids of `verticals/src/main/resources/verticals/`). A future English file
would be `<verticalId>.en.yml` with `language: en`.

```yaml
verticalId: tv # vertical id, required
language: fr # fr (default) or en, the language of the domain that serves it
published: false # true renders the block in production
title: "Téléviseurs : l'essentiel pour bien choisir" # optional, default "<Category> : l'essentiel pour bien choisir"
intro: |-
  120 to 180 words. Simple Markdown: paragraphs (blank line), **bold**,
  *italic*, [links](/path). Raw HTML is not rendered.
faq: # exactly 4 items, shown expanded and copied as is into the FAQPage JSON-LD
  - question: "Quelle taille de téléviseur choisir ?"
    answer: >-
      Plain text, no Markdown.
subCategories: # slug (as in the URL) or id of the vertical's sub-categories
  - televiseur-55-pouces
guides: # slug of a guide under verticals/src/main/resources/guides/<verticalId>/
  - slug: meilleur-televiseur-caravane-camping-car
    label: "Téléviseur pour caravane et camping-car"
```

Rules:

- The intro word count and the 4 questions are checked by
  `shared/utils/category-hub.content.spec.ts` (`pnpm test`); an invalid file is
  never served (the route logs the error and renders no block).
- A sub-category or guide link is rendered only if the target exists in the
  vertical; its label is the sub-category title, or `label` for a guide.
- The block also shows the Top 5 of the hub listing (default ImpactScore sort)
  with "Classement calculé le <date>": the date is the last ImpactScore
  calculation of the vertical (`lastScoredAt` of
  `/api/categories/<id>/impact-score-stats`). Nothing to write for it.
- Do not write the page `<title>` or meta description here: they stay in the
  vertical YAML (`verticalMetaTitle`, `verticalMetaDescription`).

## Review before publishing

With `NUXT_CATEGORY_HUBS_PREVIEW=true` (beta or dev, never prod), a file with
`published: false` is also rendered, with a "preview" notice and without
caching. Publish with `published: true` once the text is approved.
