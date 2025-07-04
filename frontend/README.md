# Nudger Frontend Project

[![CI](https://github.com/open4good/nudger-front/actions/workflows/ci.yml/badge.svg)](https://github.com/open4good/nudger-front/actions/workflows/ci.yml)
[![Static Deploy](https://github.com/open4good/nudger-front/actions/workflows/deploy-static.yml/badge.svg)](https://github.com/open4good/nudger-front/actions/workflows/deploy-static.yml)
[![Release](https://github.com/open4good/nudger-front/actions/workflows/release.yml/badge.svg)](https://github.com/open4good/nudger-front/actions/workflows/release.yml)
[![CodeQL](https://github.com/open4good/nudger-front/actions/workflows/codeql.yml/badge.svg)](https://github.com/open4good/nudger-front/actions/workflows/codeql.yml)

## Nudger UI in action

Experience Nudger frontend online:

- [https://static.nudger.fr/storybook](https://static.nudger.fr/storybook) – Storybook component showcase.
- [https://static.nudger.fr](https://static.nudger.fr) – statically generated version hosted on GitHub Pages.
- [https://demo.nudger.fr](https://demo.nudger.fr) – server-rendered demo.

**Welcome** to the Nudger front-end project. This guide is a comprehensive overview of the Nudger UI application structure, coding conventions, and tooling.

The Nudger front-end is a Nuxt 3 app (Vue 3) that interfaces with a Strapi headless CMS for content and uses an OpenAPI-described backend for core application data. We employ modern frameworks and best practices – from Tailwind CSS and Pinia, to Vitest and Storybook – to maintain a robust, scalable codebase.

Use this document as the bible for:

- developing features
- understanding project architecture
- adhering to our coding standards

## Getting Started: Installation & Setup

To get the project up and running locally, follow these steps:

1. **Prerequisites**:  
   Ensure you have Node.js `22+` and `pnpm` installed. Install it globally via:

   ```bash
   npm install -g pnpm
   ```

2. **Clone the Repository**:

   ```bash
   git clone https://github.com/open4good/nudger-front.git
   ```

3. **Install Dependencies**:

   ```bash
   pnpm install
   ```

4. **Environment Variables**:  
   Copy `.env.example` to `.env` and set the following:
   - `BASE_URL`: Base path for the Nuxt app (default `/`)
   - Other optional variables: `NUXT_PUBLIC_SITE_URL`, etc.
     These are declared in `nuxt.config.ts` under `runtimeConfig`.

5. **Run the Dev Server**:

   ```bash
   pnpm run dev
   ```

   Access it at [http://localhost:3000](http://localhost:3000)

6. **Production Build**:

   ```bash
   pnpm build && pnpm preview
   ```

7. **Static Generation (optional)**:

   ```bash
   pnpm generate
   ```

8. **Other Useful Scripts**:
   - `pnpm lint` – run ESLint
   - `pnpm format` – check formatting
   - `pnpm test` – run tests with Vitest
   - `pnpm storybook` – launch Storybook
   - `pnpm generate:api` – regenerate the OpenAPI fetch client
   - `pnpm preview` – serve the production build locally
   - `pnpm build:ssr` – build with increased memory
   - `pnpm storybook:build` – build Storybook static site

## Design Tokens

Design tokens are configured in `tokens.config.json`. Replace the
`figmaFileId` placeholder with the ID from your Figma file URL
(e.g. `https://www.figma.com/file/<FILE_ID>/...`) and provide a
`FIGMA_TOKEN` environment variable. After setting these values, run
your design token generation command to pull the latest tokens.

## Project Structure

```
src/
├── server/          # API client (OpenAPI)
├── assets/       # Images, fonts, global CSS
├── components/   # Reusable UI components
├── layouts/      # Page layout wrappers
├── pages/        # File-based routes
├── stores/       # Pinia state stores
```

- `tests/` or `*.spec.ts` files live next to components or logic.

### Key Config Files:

- `nuxt.config.ts` – Nuxt modules and runtime configuration
- `tsconfig.json` – TypeScript compiler options and path aliases
- `eslint.config.mjs` and `.prettierrc` – linting and formatting rules
- `vitest.config.ts` – test runner configuration
- `tokens.config.json` – design tokens pulled from Figma
- `.releaserc.json` – Semantic Release setup
- `renovate.json` – Renovate bot configuration
- `pnpm-workspace.yaml` – workspace and package management
- `package.json` – scripts and dependencies
- `.storybook/` – Storybook configuration files
- `nudger-api.json` – OpenAPI specification used by `generate:api`
- `.env.example` – example environment variables
- `.husky/` – Git hooks executed on commit

## Vue 3 & Nuxt 3 Conventions

- Use `<script setup lang="ts">` in all components
- Write components in TypeScript
- Use `useFetch`, `useAsyncData` for SSR-friendly data fetching
- Avoid using `window`, `document`, etc. directly – guard with `if (process.client)`
- Use path aliases like `@/components/...`
- Prefer small, focused components
- Use `definePageMeta({ layout, middleware })` in pages
- Prefer server-side data fetching for SEO-critical content

## Tailwind CSS

## Vuetify

###Vuetify is a no design skills required Open Source UI Library with beautifully handcrafted Vue Components.

- (Doc) [https://vuetifyjs.com/en/]

## Pinia (State Management)

- (Doc) [https://pinia.vuejs.org/core-concepts/]

```ts
// src/stores/cart.ts
import { defineStore } from 'pinia'

interface Product {
  id: number
  name: string
  price: number
}

export const useCartStore = defineStore('cart', {
  state: () => ({
    items: [] as Product[],
  }),
  getters: {
    itemCount: (state) => state.items.length,
    totalPrice: (state) =>
      state.items.reduce((sum, item) => sum + item.price, 0),
  },
  actions: {
    addItem(product: Product) {
      this.items.push(product)
    },
    removeItem(productId: number) {
      this.items = this.items.filter((item) => item.id !== productId)
    },
  },
})
```

## Using the Store

```ts
<script setup lang="ts">
import { useCartStore } from '@/stores/cart';
const cart = useCartStore();
</script>

<template>
  <div>
    <p>Cart has {{ cart.itemCount }} items</p>
  </div>
</template>
```

## OpenAPI Integration

- We use `@openapitools/openapi-generator-cli` with the `typescript-fetch` generator:
  ```bash
  pnpm generate:api
  ```
- Generated files under `src/api/`
- Example usage:
  ```ts
  import { DefaultApi } from '@/api'
  const api = new DefaultApi()
  const response = await api.listVersionsv2()
  ```

## Vitest (Testing)

- Colocate tests as `Component.spec.ts`
- Run tests:
  ```bash
  pnpm test
  ```

## Storybook

- Run:
  ```bash
  pnpm storybook
  ```

### Example Story

```ts
import Button from '@/components/Button.vue'

export default {
  title: 'Components/Button',
  component: Button,
}

export const Primary = {
  args: { label: 'Primary Button' },
}
```

## Linting & Formatting

- Use ESLint with Vue/TypeScript rules
- Use Prettier for formatting
- Git hooks via Husky run `pnpm lint` and `pnpm test` on commit

## SSR Best Practices

- Use `useFetch`, `useAsyncData` for server-safe data fetching
- Avoid global state across requests
- Use `<ClientOnly>`, `<LazyHydrate>` for client-only interactivity
- Beware of hydration mismatches
- Use `useHead` for SEO metadata

## Deployment & CI/CD

- Build with:

  ```bash
  pnpm build
  ```

- Deploy via:
  - Node server: `.output/server/index.mjs`
  - Vercel/Netlify (with Nitro adapter)
- Static generation (if suitable): `pnpm generate`

- Production deployments are served from **GitHub Pages** at
  [https://static.nudger.fr](https://static.nudger.fr). The Storybook is
  published alongside the site under
  [https://static.nudger.fr/storybook/](https://static.nudger.fr/storybook/).

- CI likely includes:
  - Tests and lint on PRs
  - Semantic release (automated versioning via commit messages)

## Contributing

We welcome pull requests! To contribute:

1. Fork this repository and create a feature branch.
2. Follow the [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/) specification.
3. Run `pnpm lint`, `pnpm generate`, and `pnpm test` before committing.
4. Open a pull request against the `main` branch.

## Semantic Commit Examples

```
feat: add new pricing widget
fix: correct cart total rounding
docs: update README with new install steps
```

---

Happy coding!  
Keep this guide nearby as you work on Nudger.
