# Nudger Frontend Project

[![CI](https://github.com/open4good/nudger-front/actions/workflows/ci.yml/badge.svg)](https://github.com/open4good/nudger-front/actions/workflows/ci.yml)
[![Static Deploy](https://github.com/open4good/nudger-front/actions/workflows/deploy-static.yml/badge.svg)](https://github.com/open4good/nudger-front/actions/workflows/deploy-static.yml)
[![Release](https://github.com/open4good/nudger-front/actions/workflows/release.yml/badge.svg)](https://github.com/open4good/nudger-front/actions/workflows/release.yml)
[![CodeQL](https://github.com/open4good/nudger-front/actions/workflows/codeql.yml/badge.svg)](https://github.com/open4good/nudger-front/actions/workflows/codeql.yml)

## Nudger UI in action

Experience Nudger frontend online:
- [https://static.nudger.fr](https://static.nudger.fr) – statically generated version hosted on GitHub Pages.
- [https://demo.nudger.fr](https://demo.nudger.fr) – server-rendered demo.

The default home page now features a Vuetify-based landing page.

**Welcome** to the Nudger front-end project. This guide is a comprehensive overview of the Nudger UI application structure, coding conventions, and tooling.

The Nudger front-end is a Nuxt 3 app (Vue 3) that interfaces with an OpenAPI-described backend for core application data. We employ modern frameworks and best practices – from  Pinia, to Vitest – to maintain a robust, scalable codebase.

Use this document as the bible for:

- developing features
- understanding project architecture
- adhering to our coding standards

## Getting Started: Installation & Setup

To get the project up and running locally, follow these steps:

1. **Prerequisites**:
   Ensure you have Node.js `>=20` and `pnpm 10.12.1` installed. Install it globally via:

   ```bash
   npm install -g pnpm@10.12.1
   ```

2. **Clone the Repository**:

   ```bash
   git clone https://github.com/open4good/nudger-front.git
   ```

3. **Install Dependencies**:

   ```bash
   pnpm install --offline
   ```

4. **Environment Variables**:
   Create a `.env` file with the following variable:
   - `API_URL`: Base URL of the backend API (defaults to `http://localhost:8082`).
     The value is available via `config.public.apiUrl` in `nuxt.config.ts`.

5. **Run the Dev Server**:

   ```bash
   pnpm --offline dev
   ```

   Access it at [http://localhost:3000](http://localhost:3000)

6. **Production Build**:

   ```bash
   pnpm --offline build
   ```

   Nuxt's development tools are automatically disabled when
   `NODE_ENV` is set to `production`.

7. **Static Generation (optional)**:

   ```bash
   pnpm --offline generate
   ```
  note : Precautions (window): Place the project near the root of
  your disk to avoid problems. Build problems on generate are
  related to special characters in file names and spaces in window.


8. **Other Useful Scripts**:
   - `pnpm --offline lint` – run ESLint
   - `pnpm --offline format` – check formatting
   - `pnpm --offline test` – run tests with Vitest
   - `pnpm --offline generate:api` – regenerate the OpenAPI fetch client
   - `pnpm --offline preview` – serve the production build locally
   - `pnpm --offline build:ssr` – build with increased memory

## API environment variables

Runtime configuration only requires the backend API URL. It is declared in
`nuxt.config.ts`:

- **`API_URL`** – base URL of the backend API. Defaults to
  `http://localhost:8082` and is exposed as
  `config.public.apiUrl`.

## Design Tokens

Design tokens are configured in `tokens.config.json`. Replace the
`figmaFileId` placeholder with the ID from your Figma file URL
(e.g. `https://www.figma.com/file/<FILE_ID>/...`) and provide a
`FIGMA_TOKEN` environment variable. After setting these values, run
your design token generation command to pull the latest tokens.

## Project Structure

```
project/
├── assets/       # Images, fonts, global CSS
├── components/   # Reusable UI components
├── composables/  # Logic hooks (e.g., useX)
├── constants/    # Shared constants
├── layouts/      # Page layout wrappers
├── pages/        # File-based routes
├── services/     # API service abstractions
├── plugins/      # Nuxt plugins (e.g. `fetch-logger.ts` logs all backend requests)
├── stores/       # Pinia state stores
├── server/       # Server API endpoints
├── utils/        # Helper utilities
├── src/api/      # OpenAPI generated client
```

- `tests/` or `*.spec.ts` files live next to components.

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
- `nudger-api.json` – OpenAPI specification used by `generate:api`
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

## Vuetify
- (DOC)[https://vuetifyjs.com/en/getting-started/release-notes/?version=v3.9.0]
- (Tools: vscode)[https://marketplace.visualstudio.com/items?itemName=vuetifyjs.vuetify-vscode]

## Example Button Component
```vue
<template>
  <v-btn>
   Button
  </v-btn>
</template>
```

## Pinia (State Management)

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
  pnpm --offline generate:api
  ```
- Generated files under `src/api/`
- Example usage:
  ```ts
  import { DefaultApi } from '@/api'
  const api = new DefaultApi()
  const response = await api.listVersionsv2()
  ```

## Fetching Content from blog

```ts
const config = useRuntimeConfig()
const { data: postRes } = await useFetch(
  `${config.public.apiUrl}/blog/posts`,
  {
    params: { filters: { slug } },
    // TODO: temporarily disabled
    // headers: {
    //   Authorization: `Bearer ${BLOG token}`,
    // },
  },
)
const post = postRes.value?.data[0]?.attributes
```

## Fetching dynamic content blocs

The generated `ContentApi` client allows retrieval of HTML blocs from the
`front-api` service. The `<TextContent>` component wraps this logic.

```ts
import { ContentApi, Configuration } from '@/api'
const config = useRuntimeConfig()
const api = new ContentApi(new Configuration({ basePath: config.public.apiUrl }))
const bloc = await api.contentBloc({ blocId: 'Main.WebHome' })
```

Example usage in a page:

```vue
<TextContent blocId="Main.WebHome" />
```

## Vitest (Testing)

- Colocate tests as `Component.spec.ts`
- Run tests:
  ```bash
  pnpm --offline test
  ```

### Example Test

```ts
import { mount } from '@vue/test-utils'
import Button from '@/components/Button.vue'

describe('Button', () => {
  it('renders label', () => {
    const wrapper = mount(Button, { props: { label: 'Click me' } })
    expect(wrapper.text()).toContain('Click me')
  })
})
```

## Linting & Formatting

- Use ESLint with Vue/TypeScript rules
- Use Prettier for formatting (pnpm format + --write)
- Git hooks via Husky run `pnpm --offline lint` and `pnpm --offline test` on commit

## SSR Best Practices

- Use `useFetch`, `useAsyncData` for server-safe data fetching
- Avoid global state across requests
- Use `<ClientOnly>`, `<LazyHydrate>` for client-only interactivity
- Beware of hydration mismatches
- Use `useHead` for SEO metadata

## Deployment & CI/CD

- Build with:

  ```bash
  pnpm --offline build
  ```

- Deploy via:
  - Node server: `.output/server/index.mjs`
  - Vercel/Netlify (with Nitro adapter)
 - Static generation (if suitable): `pnpm --offline generate`

  Security headers such as `Content-Security-Policy` are configured in
  `public/_headers`.  Netlify and similar static hosts read this file to
  apply HTTP response headers on all routes.

  - Production deployments are served from **GitHub Pages** at
  [https://static.nudger.fr](https://static.nudger.fr).

- CI likely includes:
  - Tests and lint on PRs
  - Semantic release (automated versioning via commit messages)
  - Node SSR deployed to the beta server via
    `.github/workflows/frontend-deploy-ssr.yml`

## Contributing

We welcome pull requests! To contribute:

1. Fork this repository and create a feature branch.
2. Follow the [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/) specification.
3. Run `pnpm --offline lint`, `pnpm --offline generate`, and `pnpm --offline test` before committing.
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
