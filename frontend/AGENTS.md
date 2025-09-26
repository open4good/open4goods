# Warnings:
- Only develop what is explicitly requested, or ask a question if you want to suggest an improvement.
- If you understand that the code produces a regression, ask questions.
- Translate all code comments in english curent language.
- Never delete code comments without ask to user.
- Always explain your choices (at least briefly)
- Composables must call server API routes for backend communication; do not reimplement service logic or talk to backend services directly.
- When the proper layer is unclear, ask for clarification before coding.

# Global context
Nudger is a search engine for electronics and household appliances, aggregating energy data from different sources to create an "Impact Score." This allows users to make the best choice of appliance before purchasing.

# Nuxt 3 Frontend Development Guide (Nudger Project)
[Doc Nuxt3](https://nuxt.com/docs/getting-started/introduction)
This guide is a comprehensive overview of the Nudger UI project. It covers the Nuxt 3 / vue 3  application structure, coding conventions and tooling. It is written for new contributors and AI code-generation agents alike, aiming to ensure consistency and clarity in development.

---

## Getting Started: Installation & Setup

1. **Prerequisites**: Node.js 20+, `pnpm@10.12.1` (`npm install -g pnpm@10.12.1`)
2. **Clone the Repository**: `git clone https://github.com/your-org/nudger-nuxt-front.git`
3. **Install Dependencies**: `pnpm install --offline`
4. **Environment Variables**:
   - `API_URL` – base URL of the backend API (defaults to `http://localhost:8082`).
5. **Run Dev Server**: `pnpm --offline dev` → http://localhost:3000
6. **Production Build**: `pnpm --offline build` then `pnpm --offline preview`
7. **Static Generation**: `pnpm --offline generate`
8. **Helper Scripts**:
   - `pnpm --offline lint`
   - `pnpm --offline format:check`
   - `pnpm --offline format`
   - `pnpm --offline test`
   - `pnpm --offline generate:api`
---

## Project Structure and Directories

- `app/pages/` → file-based routing
- `app/components/` → reusable components
- `app/layouts/` → page layouts
- `app/composables/` → reusable logic (e.g. `useFeature.ts`)
- `app/stores/` → Pinia stores
- `app/assets/` → static assets & global CSS
- `app/plugins/` → Nuxt plugins
- `app/utils/` → client-side helpers
- `shared/api-client/` → OpenAPI generated client & API helpers
- `shared/utils/` → utilities shared between client and server
- `server/` → API routes for server-side logic
- `tests/` or `*.spec.ts` → colocated or standalone test files

---

## Vue 3 & Nuxt 3 Coding Conventions

- Use `<script setup lang="ts">`
- Use `defineProps`, `ref`, `computed`, etc.
- Prefer TypeScript everywhere
- SSR-safe code: guard `window`, `document`, etc.
- Auto-imports: `useRouter`, `useHead`, etc.
- Use `@/` or `~/` aliases
- Component structure: separate logic/UI
- Nuxt page features: `definePageMeta`, `useFetch`, `useAsyncData`
- Utility function names should start with an underscore: for example `_sanitizeHtml`.

---

## Vuetify v3.9.0
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

Example:
```vue
<template>
  <button class="px-4 py-2 bg-primary text-white font-semibold rounded hover:bg-primary-dark disabled:opacity-50">
    {{ label }}
  </button>
</template>
```

---

## State Management with Pinia

- Define store with `defineStore`
- State as a function
- Use getters and actions
- Example:
```ts
export const useCartStore = defineStore('cart', {
  state: () => ({ items: [] }),
  getters: {
    itemCount: (state) => state.items.length,
    totalPrice: (state) => state.items.reduce((sum, item) => sum + item.price, 0),
  },
  actions: {
    addItem(product) { this.items.push(product); },
    removeItem(id) { this.items = this.items.filter(i => i.id !== id); },
  }
});
```

---

## OpenAPI Client Generation & Integration

The `shared/api-client` folder is **fully generated** from the specification exposed by `front-api` (`/v3/api-docs/front`).

Workflow:
1. Modify controllers or DTOs in the `front-api` project to evolve the API.
2. Build `front-api` (`mvn -pl nudger-front-api -am clean install`) to publish the new contract.
3. In this module, run `pnpm --offline generate:api` to update `shared/api-client/`.
4. Use the newly generated classes.

Never edit the generated files manually.

---

## Content from Strapi CMS

- Env vars: `STRAPI_URL`, `STRAPI_TOKEN`
- Fetch using `useFetch`
```ts
const { data } = await useFetch(`${config.public.strapiUrl}/api/pages`, {
  headers: { Authorization: `Bearer ${config.strapiToken}` },
});
```
- Handle nested `data[].attributes`
- Consider `useStrapiContent()` composable abstraction

---

## Testing with Vitest

- Colocate test files (`.spec.ts`)
- Run tests: `pnpm --offline test` or `--watch`
- Use `@vue/test-utils`
- Test stores with `setActivePinia`
- Use snapshots carefully

---




## Documentation
- Always document produced code
- Always update existing documentation (for example README.md, AGENTS.md) with features update, architecturals changes or considerations.
- Dependency updates are handled by Renovate using the configs `renovate.json` at
  the repository root and `frontend/renovate.json`. Updates run nightly.


## Linting and Formatting

- ESLint with Nuxt/Vue recommended rules
- Prettier integrated via ESLint
- Run: `pnpm --offline lint`, `pnpm --offline lint --fix`
- Run: `pnpm --offline format` to check formatting
- Husky hooks enforce checks on commits



## Architecture & SSR Best Practices

- Split UI and logic (composables, container/presentational components)
- SSR-safe: avoid global state leakage
- Lazy hydrate non-critical components (e.g., `vue-lazy-hydration`)
- Code-splitting with dynamic imports
- Use `useHead` for SEO


## Pull request
- Use Conventional commits (e.g., `feat:`, `fix:`)
- Generate a clear and complete PR description (**why** and **what**).
- Add a footer indicating this PR is generatEd by AI agent, and the estimatated time an average developper would have spent on this task

Before issueing a PR, systematically validate and check global non regession using
- pnpm --offline lint
- pnpm --offline test run
- pnpm --offline generate
- pnpm --offline build

# Best practices for Nuxt3 project

You have extensive expertise in Vue3 Nuxt 3, TypeScript, Node.js,  Pinia, VueUse, Nuxt and Vuetify. You possess deepknowledge of best practiceandperformance optimizatiotechniquesacross these technologies.
Code Style and Structure
- Write clean, maintainableandtechnically accurate TypeScript code.
- Prioritize functional andeclarativeprogramming patterns; avoiusingclasses.
- Emphasize iteration andmodularizatioto follow DRY principlesand minimize codduplication.
- Use Composition API <scripsetup lang="ts">style.
- Use Composables to encapsulate andsharreusable client-side logic orstate acrosmultiple components inyour Nuxapplication.
Nuxt 3 Specifics
- Nuxt 3 provides auto imports, sothereno need to manually import'ref''useState', or 'useRouter'.
- Take advantage of VueUse functiontoenhance reactivity and performan(except for color mode management).
- Use the Server API (within thserverapi directory) to handlserver-sideoperations like databasinteractions,authentication, oprocessing sensitivedata that must remaiconfidential.
- use useRuntimeConfig to accesandmanage runtime configuratiovariablesthat differ between environmentandare needed both on the serveandclient sides.
- The plugin `plugins/fetch-logger.ts` wraps `fetch` on the server to log each backend request to `API_URL`.
- For SEO use useHead and useSeoMeta.
- For images use <NuxtImageor<NuxtPicture> component and foIconsuse Nuxt Icons module.

Fetching Data
1. Use useFetch for standard datafetchinin components that benefitfrom SSRcaching, and reactivelyupdating based oURL changes.
2. Use $fetch for client-sidrequestswithin event handlers or wheSSRoptimization is not needed.
3. Use useAsyncData wheimplementingcomplex data fetching logilikecombining multiple API calls ocustomcaching and error handling.
4. Set server: false in useFetcoruseAsyncData options to fetch datonlyon the client side, bypassing SSR.
5. Set lazy: true in useFetcoruseAsyncData options tdefernon-critical data fetching untiafterthe initial render.

Naming Conventions
- Utilize composables, naming it use[COMPOSABLE_NAME] (eg. useBlog)
- Use **PascalCase** for componenfilenames (e.g., componentMyComponentvue).
- Favor named exports for functiontomaintain consistency and readability.
TypeScript Usage
- Use TypeScript throughoutpreferinterfaces over types fobetterextendability and merging.
- Avoid enums, opting for mapforimproved type safety and flexibility.
- Use functional componentwithTypeScript interfaces.
UI and Styling
- Use Vuetify UI forcomponents and styling.
- Implement responsive Vuetify approach and mobile-firstapproach.
- Do not introduce custom CSS or component-scoped `<style>` blocks; use Vuetify components, utility classes, and theme tokens configured via `nuxt.config.ts` for all styling requirements.

Pages structure : 

Pages naming will respect kebab-case.

To respect components loading and initialisation, be aware :
- Pages (located under /pages/*) must contains ONLY components
- Direct use of Html code, or vuetify components is stricly prohibited
- When needed, you will always create intermediary components (under /components folder)



# Model error journal
## Here are listed all recurring errors of the model.

# Mcp servers 
- Check that the Nuxt mcp server is running and if not, launch it on port 3000.
- Always use the nuxt mcp server to better understand the structure and best practices of Nuxt js
