# Frontend Asset Hardening

Nuxt chunks under `/_nuxt/` are content-hashed and can be cached as immutable.
The exception is `/_nuxt/builds/latest.json`, which stays revalidated so clients
can discover the active build.

During deploy, the GitHub workflows publish the new `.output` bundle into
`/opt/open4goods/latest/frontend-ssr`, copy any missing historical chunks from
`/opt/open4goods/bin/frontend-ssr` and `/opt/open4goods/previous/frontend-ssr`,
then publish the bundle. This keeps old browser sessions and cached HTML from
requesting chunks that were removed by `rsync --delete`.

After publish, the workflow fetches the SSR server through localhost, extracts
`/_nuxt/` assets from the returned HTML, and fails the deploy if any asset is not
reachable. Monitoring uses the same failure label, `frontend_asset_failure`, for
dynamic import errors and `_nuxt` 5xx responses.

Recommended Nginx behavior for production is to serve existing Nuxt assets from
disk before proxying to Nitro:

```nginx
location = /_nuxt/builds/latest.json {
    root /opt/open4goods/bin/frontend-ssr/.output/public;
    add_header Cache-Control "public, max-age=0, must-revalidate" always;
    try_files $uri @frontend_ssr;
}

location /_nuxt/ {
    root /opt/open4goods/bin/frontend-ssr/.output/public;
    add_header Cache-Control "public, max-age=31536000, immutable" always;
    try_files $uri @frontend_ssr;
}
```

Use `scripts/preserve-nuxt-assets.sh` to test asset preservation locally and
`scripts/sweep-nuxt-assets.py` to validate sampled pages outside GitHub Actions.
