# Packaging the PWA for app stores

The frontend ships with Capacitor so we can wrap the Nuxt build into native binaries for Google Play and the Apple App Store.

## Prerequisites

- Node.js ≥ 20 and pnpm ≥ 10.21
- Android Studio (latest stable) and an Apple machine with Xcode for iOS builds
- Developer accounts: [Google Play Console](https://play.google.com/console) + [Apple Developer Program](https://developer.apple.com/programs/)
- Environment variables (kept outside Git): signing keystores, provisioning profiles, API keys for the Play/App Store APIs

## Generate the web build

```bash
pnpm install
pnpm build
```

This emits the Nuxt client to `.output/public`, which Capacitor uses as the `webDir`.

## Sync Capacitor

`capacitor.config.ts` is already configured with `appId = fr.nudger.app` and `webDir = .output/public`.

Use the helper script to rebuild the web assets and copy them to both native shells:

```bash
pnpm cap:sync
```

> Under the hood this runs `pnpm build && pnpm exec cap sync`.

## Android build steps

1. Open the generated `android/` folder in Android Studio.
2. Update `android/app/src/main/AndroidManifest.xml` with the correct package name, icons and deep links if needed.
3. Create or import your keystore under _Build > Generate Signed Bundle/APK_.
4. Build an `.aab` for the Play Store (`Release` variant).
5. Upload the bundle through the Play Console and fill the store listing (screenshots from `app/public/pwa-assets/screenshots`).

## iOS build steps

1. Open `ios/App/App.xcworkspace` in Xcode on macOS.
2. Set the bundle identifier, display name and icons inside the _General_ tab.
3. Configure signing & capabilities with your Apple Developer Team.
4. Archive the build (`Product > Archive`) and distribute it to TestFlight/App Store Connect.

## Store assets & metadata

- Icons and splash assets come from `app/public/pwa-assets`. See the "Binary assets to add manually" section in `docs/pwa.md` for
  the exact filenames and drop them into the tree before packaging.
- Copy the long description/keywords from `i18n` so both stores stay aligned with the website copy.
- Store screenshots may reuse the generated desktop/mobile PNGs, but consider capturing real screens before the final submission.

## QA checklist before submission

- `pnpm lint`, `pnpm test`, `pnpm generate`
- `pnpm cap:sync` on both platforms without errors
- Install the PWA via Chrome/Edge/Safari and confirm the offline banner + install prompt behave as expected
- Run Lighthouse’s PWA audit (> 90 score) and attach the report to the release ticket
