#!/usr/bin/env tsx
/**
 * Scaffold a new B2B facet: generates content pages (en + fr) and a Nuxt playground page.
 *
 * Usage:
 *   pnpm tsx scripts/scaffold-facet.ts <facet-slug>
 *
 * Example:
 *   pnpm tsx scripts/scaffold-facet.ts identity
 *
 * This creates:
 *   content/en/docs/products/<slug>.md
 *   content/en/docs/products/<slug>/documentation/java.md
 *   content/en/docs/products/<slug>/documentation/python.md
 *   content/fr/docs/products/<slug>.md
 *   content/fr/docs/products/<slug>/documentation/java.md
 *   content/fr/docs/products/<slug>/documentation/python.md
 *   pages/docs/products/<slug>/playground.vue
 *
 * After scaffolding:
 *   1. Add a FacetDescriptor constant to domains/b2b/facets.ts (copy FACET_PRICE as template).
 *   2. Register it in the FACETS map.
 *   3. Fill in the content pages and sample fixtures.
 *   4. Run the backend endpoint + add it to b2b-catalog.yml.
 */

import { existsSync, mkdirSync, writeFileSync } from 'fs'
import { join } from 'path'

const slug = process.argv[2]
if (!slug) {
  console.error('Usage: tsx scripts/scaffold-facet.ts <facet-slug>  (e.g. "identity")')
  process.exit(1)
}

const facetId = `product.${slug}`
const endpointPath = `/api/v1/products/{gtin}/${slug}`
const root = join(import.meta.dirname, '..')

function write (path: string, content: string) {
  const abs = join(root, path)
  const dir = abs.substring(0, abs.lastIndexOf('/'))
  if (!existsSync(dir)) mkdirSync(dir, { recursive: true })
  if (existsSync(abs)) {
    console.warn(`SKIP (exists): ${path}`)
    return
  }
  writeFileSync(abs, content, 'utf8')
  console.log(`CREATE: ${path}`)
}

// --- Content pages ---

const enRef = `---
title: "${slug.charAt(0).toUpperCase() + slug.slice(1)} facet reference"
description: "Full reference for GET ${endpointPath} — parameters, response schema, and billing."
tags:
  - ${slug}
  - facet
  - products
scope: public
---

# ${slug.charAt(0).toUpperCase() + slug.slice(1)} facet reference

> **TODO**: Fill in this page following the content outline in \`docs/b2b/facets/product-${slug}.md\`.

## Endpoint

\`\`\`http
GET ${endpointPath}
Authorization: Bearer pdapi_YOUR_KEY_HERE
\`\`\`

## Parameters

| Parameter | In | Type | Required | Description |
|---|---|---|---|---|
| \`gtin\` | path | string | Yes | GTIN-8, -12, -13, or -14 |
| \`language\` | query | string | No | Response language (\`en\`, \`fr\`). Default: \`en\` |

## Quickstarts

- [Java quickstart](/docs/products/${slug}/documentation/java)
- [Python quickstart](/docs/products/${slug}/documentation/python)
- [Live playground](/docs/products/${slug}/playground)
`

const frRef = `---
title: "Référence facette ${slug}"
description: "Référence complète pour GET ${endpointPath} — paramètres, schéma de réponse et facturation."
tags:
  - ${slug}
  - facette
  - produits
scope: public
---

# Référence facette ${slug}

> **TODO** : Compléter cette page selon le plan dans \`docs/b2b/facets/product-${slug}.md\`.

## Endpoint

\`\`\`http
GET ${endpointPath}
Authorization: Bearer pdapi_YOUR_KEY_HERE
\`\`\`

## Démarrages rapides

- [Démarrage rapide Java](/fr/docs/products/${slug}/documentation/java)
- [Démarrage rapide Python](/fr/docs/products/${slug}/documentation/python)
- [Playground en direct](/fr/docs/products/${slug}/playground)
`

const enJava = `---
title: "Java quickstart — ${slug} facet"
description: "Query the Product Data API ${slug} facet from Java."
tags:
  - java
  - quickstart
  - ${slug}
scope: public
---

# Java quickstart — ${slug} facet

> **TODO**: Adapt the code examples for the \`${facetId}\` facet response schema.

## Prerequisites

- Java 11 or higher
- A valid API key (\`pdapi_...\`) from [Dashboard → API Keys](/dashboard/api-keys)

## Example

\`\`\`java
// TODO: add Java example for ${facetId}
\`\`\`

## Next steps

- [${slug.charAt(0).toUpperCase() + slug.slice(1)} facet reference](/docs/products/${slug})
- [Error handling](/docs/errors)
`

const frJava = `---
title: "Démarrage rapide Java — facette ${slug}"
description: "Interroger la facette ${slug} de la Product Data API depuis Java."
tags:
  - java
  - démarrage rapide
  - ${slug}
scope: public
---

# Démarrage rapide Java — facette ${slug}

> **TODO** : Adapter les exemples de code pour le schéma de réponse \`${facetId}\`.

\`\`\`java
// TODO : ajouter l'exemple Java pour ${facetId}
\`\`\`
`

const enPython = `---
title: "Python quickstart — ${slug} facet"
description: "Query the Product Data API ${slug} facet from Python."
tags:
  - python
  - quickstart
  - ${slug}
scope: public
---

# Python quickstart — ${slug} facet

> **TODO**: Adapt the code examples for the \`${facetId}\` facet response schema.

## Prerequisites

- Python 3.8+
- A valid API key (\`pdapi_...\`) from [Dashboard → API Keys](/dashboard/api-keys)

## Example

\`\`\`python
# TODO: add Python example for ${facetId}
\`\`\`

## Next steps

- [${slug.charAt(0).toUpperCase() + slug.slice(1)} facet reference](/docs/products/${slug})
- [Error handling](/docs/errors)
`

const frPython = `---
title: "Démarrage rapide Python — facette ${slug}"
description: "Interroger la facette ${slug} de la Product Data API depuis Python."
tags:
  - python
  - démarrage rapide
  - ${slug}
scope: public
---

# Démarrage rapide Python — facette ${slug}

> **TODO** : Adapter les exemples de code pour le schéma de réponse \`${facetId}\`.

\`\`\`python
# TODO : ajouter l'exemple Python pour ${facetId}
\`\`\`
`

write(`content/en/docs/products/${slug}.md`, enRef)
write(`content/en/docs/products/${slug}/documentation/java.md`, enJava)
write(`content/en/docs/products/${slug}/documentation/python.md`, enPython)
write(`content/fr/docs/products/${slug}.md`, frRef)
write(`content/fr/docs/products/${slug}/documentation/java.md`, frJava)
write(`content/fr/docs/products/${slug}/documentation/python.md`, frPython)

// --- Nuxt playground page ---

const playgroundPage = `<template>
  <div class="py-4 py-md-8">
    <v-row justify="center">
      <v-col cols="12" lg="11">
        <B2bPageHeader
          :title="t('playground.title')"
          :subtitle="t('playground.subtitle')"
        />
        <B2bFacetPlayground :facet="facet" />
      </v-col>
    </v-row>
  </div>
</template>

<script setup lang="ts">
import { FACETS } from '~/domains/b2b/facets'

// TODO: Replace 'product.${slug}' with the constant exported from domains/b2b/facets.ts
const facet = FACETS['product.${slug}']
if (!facet) throw new Error('Facet product.${slug} not registered in domains/b2b/facets.ts')

definePageMeta({ width: 'fluid' })

const { t } = useI18n()

useLocalizedPageSeo({
  titleKey: 'playground.seo.title',
  descriptionKey: 'playground.seo.description'
})
</script>
`

write(`pages/docs/products/${slug}/playground.vue`, playgroundPage)

console.log(`
Done! Next steps:
  1. Add FACET_${slug.toUpperCase()} descriptor to domains/b2b/facets.ts (copy FACET_PRICE as template).
  2. Register it in the FACETS map: '${facetId}': FACET_${slug.toUpperCase()}.
  3. Update pages/docs/products/${slug}/playground.vue to import the new constant.
  4. Fill in content/en/docs/products/${slug}.md (and fr mirror).
  5. Implement the backend endpoint + add to b2b-catalog.yml.
  6. Write a facet spec in docs/b2b/facets/product-${slug}.md.
`)
