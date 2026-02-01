# Progressive Web App reference

This document explains how the Nudger Nuxt frontend is configured as a PWA and what to touch when shipping new capabilities.

## Manifest & metadata

- The manifest lives at `app/public/site.webmanifest`. It contains the product name, categories, shortcuts, screenshots and the link to all generated icons.
- Nuxt exposes it through `app.head.link` inside `nuxt.config.ts`. Update the `link` + `meta` entries whenever a new icon or theme color ships.
- The same manifest is injected into `@vite-pwa/nuxt` (see `nuxt.config.ts > pwa.manifest`). Keep both in sync by editing the JSON file only, then re-running `pnpm lint` to ensure type safety.

## Icon & screenshot pipeline

Icons and screenshots live under `app/public/pwa-assets`. Regenerate the set via Pillow whenever branding evolves:

```bash
pnpm exec python3 scripts/generate_pwa_assets.py
```

> The script writes transparent + maskable icons and the two store screenshots referenced by the manifest.

After regenerating assets run `pnpm lint` so the manifest import stays type-checked.

## Service worker & caching

`@vite-pwa/nuxt` is registered in `nuxt.config.ts` with:

- `registerType: 'autoUpdate'` so users see updates as soon as the build is deployed.
- Workbox runtime caches for API calls (`NetworkFirst`) and media (`StaleWhileRevalidate`).
- An offline fallback route served from `server/routes/offline.get.ts`.

When touching caching rules adjust the `runtimeCaching` array near the top of `nuxt.config.ts`.

## Install & offline UX

- The composable `app/composables/usePwaPrompt.ts` wraps the `beforeinstallprompt` event and service-worker registration state.
- UI entry points live in `app/components/pwa` and are mounted from the default layout + error layout so all routes share the same UX.
- All strings belong to `i18n/locales/*` under the `pwa.*` namespace.

### Offline indicator & responsive hints

- `app/components/pwa/PwaOfflineNotice.vue` renders a floating indicator instead of a blocking banner.
  - Desktop viewports show a permanent tooltip anchored to a warning icon in the top-right corner.
  - Mobile / PWA shells receive an inline pill with retry/close icons sized for touch.
- `app/composables/pwa/usePwaOfflineNoticeBridge.ts` still manages the shared dismissal state; the indicator simply consumes it together with `useDisplay()` from `nuxt-device` to react to viewport changes.

### Barcode scanner pipeline

- `app/components/pwa/PwaBarcodeScanner.vue` owns the capture logic.
  - When supported, it uses the native [`BarcodeDetector`](https://developer.mozilla.org/docs/Web/API/BarcodeDetector) API tied to `useDevice()` to prioritise the rear camera in mobile shells.
  - If the detector or camera APIs are unavailable, it lazily loads the `vue-barcode-reader` stream component as a fallback and keeps emitting `decode` events with the normalised GTIN/EAN value.
- `app/components/search/SearchSuggestField.vue` embeds the component inside the search dialog and forwards detections directly to the `/[gtin]` route.
- This keeps the scanner self-contained and ready for reuse (e.g. in future contribution flows).

### Notifications readiness

- `app/plugins/pwa-capabilities.client.ts` captures whether the browser supports the Notifications + Service Worker combo and stores the registration once it is ready.
- `app/composables/pwa/usePwaCapabilities.ts` exposes `supportsNotifications`, the cached permission and a `requestNotificationPermission()` helper so future UI can prompt users consistently.
- The state is SSR-safe and remains accessible from any component or store without leaking browser globals.

### Runtime caching reference

In addition to the default image/API caches, `nuxt.config.ts` now pre-defines the following Workbox strategies:

1. `nudger-font-styles` - `StaleWhileRevalidate` for Google Fonts stylesheets.
2. `nudger-font-files` - `CacheFirst` for Google Fonts binaries with a one-year TTL.
3. `nudger-static-cdn` - `StaleWhileRevalidate` for JS/CSS fetched from `cdn.jsdelivr.net` or `unpkg.com`.

These extra caches keep typography and remote UI widgets available offline and reduce layout shifts when the network is flaky.

## Validation checklist

Before merging PWA-related work run the standard frontend checks:

```bash
pnpm lint
pnpm test
pnpm generate
```

Additionally, confirm the PWA plugin output using Lighthouse (Chrome DevTools) or `pnpm build && pnpm preview` followed by a manual install test on desktop + Android.
