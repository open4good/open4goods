# Verticals Configuration and Generation

This document explains how verticals are configured, how the configuration is structured, and how the **Generation API** is used to assist in creating and updating these configurations.

## Configuration Structure

Vertical configurations are split into two primary locations within the `verticals` module resources (`verticals/src/main/resources/verticals/`):

### 1. Main Vertical Configuration (`/verticals/*.yml`)

These files (e.g., `tv.yml`, `refrigerator.yml`) contain the core definition of a vertical:

- **Identity**: ID, taxonomy mappings (Google, Icecat).
- **UI Configuration**: Images, icons, i18n texts (URLs, titles, meta tags).
- **Attributes**: Definitions of attributes specific to the vertical, including filters and display options.
- **Nudge Tool**: Configuration for the "Nudge" wizard (questions, subsets).
- **Categories Matching**: Rules for mapping retailer categories to this vertical.

### 2. Impact Score Configuration (`/verticals/impactscores/*.yml`)

These files (e.g., `impactscores/tv.yml`) are dedicated to the Impact Score (Eco-Score) logic for the corresponding vertical. They are loaded separately and merged into the vertical configuration at runtime.

- **Criteria Ponderation**: Weights for each attribute contributing to the score (must sum to 1.0).
- **Texts**: Localized explanations for _why_ specific criteria are used, the method purpose, available data analysis, and critical reviews.
- **AI Metadata**: Prompts and raw responses used to generate the configuration (useful for debugging and re-generation).

**Note**: A `_default.yml` exists in both directories to provide fallback values (e.g., default constants or thresholds).

## Hot Loading and Updates

The `VerticalsConfigService` is responsible for loading these files.

- It merges the `impactscores/{id}.yml` content into the main `VerticalConfig` object at runtime.
- This separation allows for cleaner management of the scoring logic, which can be complex and text-heavy, separate from the structural vertical definition.

## Generation API

The `VerticalsGenerationController` provides endpoints to generate and update these configurations, often leveraging Generative AI to draft the content.

### updating Impact Scores

**Endpoint**: `GET /api/update/{vertical}/impactscore/`

- **Function**: Generates or updates the Impact Score configuration for the specified vertical.
- **Logic**:
  1.  Analyzes available attributes and their coverage for the vertical.
  2.  Prompts an AI service (e.g., OpenAI) to propose relevant criteria, weights (`criteriasPonderation`), and explanatory texts (`texts`).
  3.  **Writes directly** to the `verticals/impactscores/{vertical}.yml` file.
  4.  The new configuration is immediately available if hot-reloading is active (or upon restart).

### Updating Attributes and Categories

**Endpoint**: `GET /api/update/{vertical}` (and sub-resources)

- **Attributes**: Analyzes product data to suggest relevant attributes and filters (`attributesConfig`).
- **Categories**: Updates category matching rules based on product data analysis.
- **Location**: These updates are written to the main `verticals/{vertical}.yml` file.
