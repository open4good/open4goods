# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 📚 Documentation

For detailed guides and architecture documentation, see:

- **[Frontend API Integration Guide (FR)](./FRONTEND-API-GUIDE.md)** - Guide complet d'intégration des API pour les développeurs frontend
- **[Frontend API Integration Guide (EN)](./FRONTEND-API-GUIDE.en.md)** - Complete API integration guide for frontend developers
- **[Server Architecture (Clean Architecture/DDD)](./server/DOC.md)** - Server-side architecture documentation

## Development Commands

### Package Manager

This project uses `pnpm` as the package manager. All commands should be run with `pnpm`.

### Essential Commands

- `pnpm dev` - Start development server (http://localhost:3000)
- `pnpm build` - Production build
- `pnpm build:ssr` - Production build with increased memory (NODE_OPTIONS='--max-old-space-size=8192')
- `pnpm generate` - Static site generation
- `pnpm preview` - Serve production build locally
- `pnpm lint` - Run ESLint
- `pnpm lint:fix` - Run ESLint and fix issues + Prettier formatting
- `pnpm format` - Run Prettier formatting
- `pnpm format:check` - Check Prettier formatting
- `pnpm test` - Run Vitest tests
- `pnpm generate:api` - Regenerate OpenAPI client from remote spec (Unix/Linux/macOS)
- `pnpm generate:api:win` - Regenerate OpenAPI client from remote spec (Windows)
- `pnpm preprocess:css` - Process Bootstrap/XWiki styles for TextContent component

### Prerequisites

- Node.js >=20
- pnpm 10.12.1 (install with `npm install -g pnpm@10.12.1`)

## Architecture Overview

This is a **Nuxt 3** frontend application built with **Vue 3** and **TypeScript**. Key architectural patterns:

### Core Stack

- **Nuxt 3** - Meta-framework with SSR/SSG support
- **Vue 3** with Composition API and `<script setup>`
- **TypeScript** - Type safety throughout
- **Vuetify** - Material Design component library
- **Pinia** - State management
- **Vitest** - Testing framework

### Project Structure

```
├── app/                # Nuxt application source (components, pages, layouts, stores)
│   ├── components/     # Reusable UI components (auto-imported)
│   ├── pages/          # File-based routing
│   ├── layouts/        # Page layout wrappers
│   ├── stores/         # Pinia stores (useAuthStore, useAppStore)
│   ├── composables/    # Composition API hooks
│   ├── assets/         # Static assets, SASS files
│   ├── plugins/        # Nuxt plugins
│   └── utils/          # Client-side helper utilities
├── shared/             # Code shared between client and server
│   ├── api-client/     # Generated OpenAPI client and typed service wrappers
│   ├── constants/      # Shared constants
│   └── utils/          # Cross-cutting helpers (sanitizers, etc.)
├── server/             # Server API endpoints and utilities
├── i18n/               # Internationalization resources
└── public/             # Static public assets
```

### Key Patterns

#### Authentication & Authorization

- JWT-based auth with HTTP-only cookies
- Roles stored in `useAuthStore` (decoded from JWT)
- Common roles: `ROLE_SITEEDITOR`, `XWIKIADMINGROUP`
- Use `useAuth()` composable for role checks

#### API Integration

- OpenAPI-generated client in `shared/api-client/` (regenerate with `pnpm generate:api`)
- Server-side auth headers injected via `MACHINE_TOKEN`
- Runtime config for API URLs and tokens

#### Content Management

- `<TextContent>` component for dynamic HTML blocs from backend
- Content editing available to users with editor roles
- XWiki-style content blocs with prefixed CSS

#### Internationalization

- French (fr-FR) and English (en-US) support
- `prefix_except_default` strategy (English is default, French has `/fr-FR` prefix)

### Environment Variables

Key variables in `.env`:

- `API_URL` - Backend API URL (default: http://localhost:8082)
- `TOKEN_COOKIE_NAME` - JWT cookie name (default: access_token)
- `REFRESH_COOKIE_NAME` - Refresh token cookie name
- `MACHINE_TOKEN` - Server-to-server auth token
- `EDITOR_ROLES` - Comma-separated editor roles

### Code Conventions

- Use `<script setup lang="ts">` in all components
- Prefer `useFetch`/`useAsyncData` for SSR-friendly data fetching
- Use path aliases: `@/components`, `@/stores`, etc.
- ESLint configured with Vue/TypeScript rules, Prettier for formatting
- Tests colocated as `*.spec.ts` files

### Route Configuration

- Admin routes (`/admin/**`) are client-side only (`ssr: false`)
- Blog routes use SWR caching (60s)
- File-based routing with dynamic segments

### Build & Deployment

- Supports both SSR and static generation
- Nitro preset: `node-server`
- GitHub Pages deployment for static builds
- Husky git hooks run lint and tests on commit

### Vuetify

- Responsive block control must be done at the level of the parent components: pages or layouts: via v-row and v-col

# Mcp servers

- Check that the Nuxt mcp server is running and if not, launch it on port 3000.
- Always use the nuxt mcp server to better understand the structure and best practices of Nuxt js
