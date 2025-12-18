# Nudger Frontend Project


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

> 📄 **Need to update the sitemap?** See [docs/sitemap-generation.md](docs/sitemap-generation.md) for the end-to-end workflow and configuration reference.

## Getting Started: Installation & Setup

To get the project up and running locally, follow these steps:

1. **Prerequisites**:
   Ensure you have Node.js `>=20` and `pnpm 10.26.0` installed. Install it globally via:

   ```bash
   npm install -g pnpm@10.26.0
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
   Create a `.env` file with the following variables:
   - `API_URL`: Base URL of the backend API (defaults to `http://localhost:8082`).
     The value is available via `config.apiUrl` in `nuxt.config.ts`.
   - `TOKEN_COOKIE_NAME`: Name of the cookie storing the JWT. Defaults to `access_token`.
   - `REFRESH_COOKIE_NAME`: Name of the cookie storing the refresh token. Defaults to `refresh_token`.
   - `MACHINE_TOKEN`: Shared secret used for server-to-server requests. This value is loaded only on the server and never exposed to the client.
   - `EDITOR_ROLES`: Comma-separated roles allowed to edit content blocs. Defaults to `ROLE_SITEEDITOR,XWIKIADMINGROUP`—the role names issued in the JWT—and is exposed as `config.public.editRoles`.

5. **Run the Dev Server**:

   ```bash
   pnpm dev
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
   - `pnpm test:visual` – run Playwright visual snapshots (requires dev server at `http://localhost:3000`)
   - `pnpm --offline generate:api` – regenerate the OpenAPI fetch client
   - `pnpm --offline preprocess:css` – prefix Bootstrap and XWiki styles for `<TextContent>`
   - `pnpm --offline preview` – serve the production build locally
   - `pnpm --offline build:ssr` – build with increased memory

## API environment variables

Runtime configuration uses the following variables defined in `nuxt.config.ts`:

- **`API_URL`** – base URL of the backend API. Defaults to
  `http://localhost:8082` and is exposed as
  `config.apiUrl`.
- **`TOKEN_COOKIE_NAME`** – cookie name for the JWT. Defaults to
  `access_token`.
- **`REFRESH_COOKIE_NAME`** – cookie name for the refresh token. Defaults to
  `refresh_token`.
- **`MACHINE_TOKEN`** – shared token for server requests. Only available on the server through `config.machineToken` and injected as `X-Shared-Token` when calling `config.apiUrl`.
- **`EDITOR_ROLES`** – comma-separated roles that enable edit links on content blocs. Defaults to `ROLE_SITEEDITOR,XWIKIADMINGROUP` (roles returned by the backend) and is exposed as `config.public.editRoles`.

## Authentication cookies

Credentials are submitted to `/auth/login`. The handler stores the returned JWT
and refresh token in HTTP‑only cookies. In production these cookies are marked
`Secure` and `SameSite=None` to allow usage across subdomains. During local
development the cookies fall back to `SameSite=Lax` and are not forced to be
secure.

## Role-based UI with Pinia

The `useAuthStore` decodes the JWT to keep the user's roles and login state.
Typical roles returned by the backend are `ROLE_SITEEDITOR` for content editors and `XWIKIADMINGROUP` for administrators.
Use the `useAuth()` composable inside components or pages:

```ts
const { isLoggedIn, hasRole } = useAuth()
```

Template example:

```vue
<v-alert v-if="hasRole('XWIKIADMINGROUP')">Admin specific content</v-alert>
```

Users with any role listed in `config.public.editRoles` will see an edit link on content blocs.

## Design Tokens

Design tokens are configured in `tokens.config.json`. Replace the
`figmaFileId` placeholder with the ID from your Figma file URL
(e.g. `https://www.figma.com/file/<FILE_ID>/...`) and provide a
`FIGMA_TOKEN` environment variable. After setting these values, run
your design token generation command to pull the latest tokens.

## Project Structure

```
project/
├── app/                 # Nuxt application source
│   ├── assets/          # Images, fonts, global CSS
│   ├── components/      # Reusable UI components
│   ├── composables/     # Logic hooks (e.g., useX)
│   ├── layouts/         # Page layout wrappers
│   ├── pages/           # File-based routes
│   ├── plugins/         # Nuxt plugins (e.g. `fetch-logger.ts` logs backend requests)
│   ├── stores/          # Pinia state stores
│   └── utils/           # Client-side helpers (e.g., image utilities)
├── shared/              # Code shared between client and server
│   ├── api-client/      # OpenAPI generated client and service wrappers
│   └── utils/           # Cross-cutting utilities (e.g., HTML sanitizer)
├── server/              # Server API endpoints and utilities
├── api-specs/           # OpenAPI specifications and generator config
├── public/              # Static public assets
└── scripts/             # Project maintenance scripts
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
- `package.json` – scripts and dependencies
- `api-specs/nudger-api.json` – OpenAPI specification used by `generate:api`
- `api-specs/openapitools.json` – OpenAPI generator CLI configuration
- `.husky/` – Git hooks executed on commit

## Route internationalisation

The application keeps translated slugs in a single place so every layer can share
them consistently. `shared/utils/localized-routes.ts` exposes:

- `LOCALIZED_ROUTE_PATHS` – a record that maps each named route to the slug that
  should be served per Nuxt locale.
- `resolveLocalizedRoutePath(routeName, locale, params?)` – returns the
  translated path for navigation components and programmatic redirects. Provide
  any dynamic parameters (e.g. `{ slug: article.slug }`) and the helper injects
  them into the template.
- `buildI18nPagesConfig()` – used by `nuxt.config.ts` to generate the
  `@nuxtjs/i18n` `pages` configuration so visiting translated slugs never yields
  a 404.

When you add a page that needs a translated slug:

1. Register the page with a stable route name (for example `team`).
2. Extend `LOCALIZED_ROUTE_PATHS` with the locale-to-path mapping, using Nuxt
   locales such as `fr-FR` and `en-US`.
3. Replace hard-coded strings in menus or components with
   `resolveLocalizedRoutePath('team', locale)` so SSR and CSR generate the same
   URL.

Call `normalizeLocale(locale)` if you receive user input that may not already be
a supported Nuxt locale. The helper falls back to the default locale to avoid
runtime errors.

## Translation bundles and Vuetify locales

Nuxt i18n now lazy-loads TypeScript wrappers located in
`frontend/i18n/locales/*.ts`. Each wrapper imports the existing JSON bundle as
the application source of truth and merges it with Vuetify's locale pack:

```ts
// i18n/locales/en-US.ts
import { en as $vuetify } from 'vuetify/locale'
import app from './en-US.json'

export default { ...app, $vuetify }
```

This keeps the authoring workflow unchanged (translations remain in JSON) while
Vuetify components automatically pick up strings such as pagination labels via
`$vuetify`. When adding a new locale, create the JSON file first, then add a
matching `.ts` wrapper that imports both the JSON content and the Vuetify pack
for that language.

Shared runtime options (e.g. default locale, fallback, warning behaviour) are
defined once in `frontend/i18n.config.ts` and referenced from `nuxt.config.ts`
through the `vueI18n` option so that server and client renderers stay aligned.

## Vue 3 & Nuxt 3 Conventions
- Use `<script setup lang="ts">` in all components
- Write components in TypeScript
- Use `useFetch`, `useAsyncData` for SSR-friendly data fetching
- Avoid using `window`, `document`, etc. directly – guard with `if (process.client)`
- Use path aliases like `@/components/...`
- Prefer small, focused components
- Use `definePageMeta({ layout, middleware })` in pages
- Prefer server-side data fetching for SEO-critical content

# NUXT MCP
#### Source : [`https://github.com/antfu/nuxt-mcp?tab=readme-ov-file`](Antfu: nuxt-mcp)
### Benefits: 
#### Project Understanding:
- Nuxt folder structure
- Configuration (nuxt.config.ts)
- Available auto-imports
- Existing components and pages

#### Available Actions:
- List components/pages/layouts
- Analyze Vite/Nuxt configuration
- Generate contextual code
- Scaffold new elements

#### Practical Benefits:
- Claude/Codex knows your exact stack
- Code suggestions tailored to your project
- Consistent component generation
- Respects your app conventions
## install the mcp server of nuxt for Claude-code or Codex usage
- pnpm add nuxt-mcp
- pnpm approve-builds
- Select all packages (tab & space)
- yes
- For Claude-code cli : install the server : claude mcp add --transport sse nuxt-local http://localhost:3000/__mcp/sse
- Important : start project first : pnpm dev & use the same port for mcp-server

## Vuetify & vuetify MCP
- (DOC)[https://vuetifyjs.com/en/getting-started/release-notes/?version=v3.9.0]
- (Tools: vscode)[https://marketplace.visualstudio.com/items?itemName=vuetifyjs.vuetify-vscode]
 ### Vuetify MCP & Claude-code
  - Install Vuetify MCP server: `pnpm add @vuetify/mcp@latest`
  - Configure in Claude: `npx @vuetify/mcp config`
  - **Start MCP server**: `node node_modules/@vuetify/mcp/dist/index.js` (run in background)
  
  The Vuetify MCP server provides tools for:
    - Generating Vuetify components with the correct props
    - Accessing APIs and documentation (`get_component_api_by_version`, `get_directive_api_by_version`)
    - Installation guides (`get_installation_guide`)
    - Release notes (`get_release_notes_by_version`)
    - Available features (`get_available_features`)
    - FAQ (`get_frequently_asked_questions`)
  
  **Quick start next time**:
  ```bash
  # Start the MCP server in background in Claude Code
  node node_modules/@vuetify/mcp/dist/index.js &
  
  # Or with npm
  npx @vuetify/mcp
  ```
  
**Important**: The MCP server must be running **while** you use Claude Code to benefit from Vuetify tools. It communicates directly with Claude Code via the MCP protocol.

### Vuetify MCP API Tools
  - get_vuetify_api_by_version: Download and cache Vuetify API types by version
  - get_component_api_by_version: Get the API for a specific component (props, events, slots, methods)
  - get_directive_api_by_version: Get the API for a Vuetify directive (v-ripple, v-scroll, etc.)

  Documentation Tools

  - get_installation_guide: Installation guides for Vue CLI, Nuxt, Vite
  - get_available_features: List of available components, directives, and composables
  - get_exposed_exports: Available exports of the Vuetify package
  - get_frequently_asked_questions: FAQs from the Vuetify documentation
  - get_release_notes_by_version: Release notes to understand the changes

  Practical Use Cases

  - Generate Vuetify components with the correct props
  - Create layouts Best-practice UI
  - Access documentation without leaving your IDE
  - Get AI help that understands Vuetify's component structure
    ```

## Example Button Component
```vue
<template>
  <v-btn>
   Button
  </v-btn>
</template>
```

## CSS/SASS : BEM Convention
  SASS classes (excluding Vuetify classes) must follow the BEM convention:
    - Block/Element/Modifiers
      `.block__elem--mod`
    Documentation here: `https://getbem.com/naming/`
    Note for Claude users : use slash-command `/css-class-validator` for validate & auto-fix your class.

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
- Doc: [docs/backend-services.md](Doc-services-usage)

- We use `@openapitools/openapi-generator-cli` with the `typescript-fetch` generator:
  ```bash
  pnpm --offline generate:api
  ```
- Generated files under `shared/api-client/`
- Example usage:
  ```ts
  import { DefaultApi } from '~~/shared/api-client'
  const api = new DefaultApi()
  const response = await api.listVersionsv2()
  ```
- Service wrappers keep those generated clients SSR-only, inject the shared
  token, and expose domain-language aware helpers for server routes. Follow the
  [backend services guide](./docs/backend-services.md) for the full pattern,
  including caching, error translation, and parameter validation.

## Fetching Content from blog

```ts
const config = useRuntimeConfig()
const { data: postRes } = await useFetch(
  `${config.apiUrl}/blog/posts`,
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
const api = new ContentApi(new Configuration({ basePath: config.apiUrl }))
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

  ### use cSpell:words
    Doc : `https://cspell.org/` 

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
    `.github/workflows/frontend-deploy-ssr.yml`, which injects
    `API_URL` and `MACHINE_TOKEN` from repository secrets during
    the build.

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

## Frontend internationalisation

Hostname-driven language resolution is centralised in [`shared/utils/domain-language.ts`](./shared/utils/domain-language.ts). The helper normalises the incoming host, maps it to a domain language (`'en' | 'fr'`), and exposes the matching Nuxt locale (`'en-US' | 'fr-FR'`).

- The i18n hostname plugin imports the helper so both SSR and CSR pick the same locale for a given domain.
- Server API routes reuse the helper to read the `x-forwarded-host`/`host` headers and forward the resolved `domainLanguage` to downstream services (e.g. blog and CMS clients).
- Unknown domains fall back to English and emit a warning on the server so misconfigurations surface in logs without breaking the response.

Refer to [`/frontend/docs/internationalisation.md`](/frontend/docs/internationalisation.md) for the full workflow, current mappings, and update procedure.

---

Happy coding!
Keep this guide nearby as you work on Nudger.
