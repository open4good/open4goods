# Homepage Images Mechanism

## Overview

The homepage utilizes a dynamic image loading mechanism for the "Pain" and "Gain" sections (Problem and Solution sections).

## Image Source

Images are stored in the following directories:

- `frontend/app/assets/homepage/pain/` (for the Problem section)
- `frontend/app/assets/homepage/gain/` (for the Solution section)

These folders should contain SVG, PNG, JPG, or WEBP images.

## Loading Logic

The loading logic is handled by the composable `useRandomHomepageImages.ts`.

- On initialization, it picks a random image from the respective folder.
- If the folder is empty or the image cannot be loaded, it falls back to a default image.

### Default Placeholders

- Pain: `/images/home/nudger-problem.webp` (initially)
- Gain: `/images/home/nudger-screaming.webp` (default fallback)

## Manual "Next" Image

The Solution section allows the user to manually pick a new random image by clicking a "Next" button. This triggers a function in the composable that selects a new random image from the `gain` assets folder, excluding the currently displayed image if possible.
