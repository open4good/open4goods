#!/bin/bash
set -e

# Define expected URL base
EXPECTED_URL="https://nudger.fr"

echo "Using expected URL base: $EXPECTED_URL"

# Run nuxt generate (this will generate the .output directory)
echo "Generating static site..."
# We use pnpm generate which runs nuxt generate
# We can skip the full build if possible, but generate is needed for sitemap
pnpm generate

# Check if sitemap index exists
SITEMAP_INDEX=".output/public/sitemap_index.xml"
if [ ! -f "$SITEMAP_INDEX" ]; then
    echo "Error: Sitemap index not found at $SITEMAP_INDEX"
    exit 1
fi

echo "Sitemap index found."

# Check for localhost in sitemap index
if grep -q "localhost" "$SITEMAP_INDEX"; then
    echo "Error: 'localhost' found in $SITEMAP_INDEX"
    grep "localhost" "$SITEMAP_INDEX"
    exit 1
fi

# Check for correct URL in sitemap index
if ! grep -q "$EXPECTED_URL" "$SITEMAP_INDEX"; then
    echo "Error: Expected URL '$EXPECTED_URL' not found in $SITEMAP_INDEX"
    exit 1
fi

# Check specific app pages sitemap (main-pages.xml in config)
APP_SITEMAP=".output/public/sitemap/main-pages.xml"
if [ ! -f "$APP_SITEMAP" ]; then
    echo "Error: App pages sitemap not found at $APP_SITEMAP"
    exit 1
fi

echo "App pages sitemap found."

# Check for localhost in app pages sitemap
if grep -q "localhost" "$APP_SITEMAP"; then
    echo "Error: 'localhost' found in $APP_SITEMAP"
    grep "localhost" "$APP_SITEMAP"
    exit 1
fi

# Check for correct URL
if ! grep -q "$EXPECTED_URL" "$APP_SITEMAP"; then
    echo "Error: Expected URL '$EXPECTED_URL' not found in $APP_SITEMAP"
    exit 1
fi

echo "Success: Sitemap contains correct URLs and no localhost references."
