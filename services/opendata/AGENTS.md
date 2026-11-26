# OpenData Service

> **Service Guide**: See [/services/AGENTS.md](../AGENTS.md#opendata) for detailed guidance.

**Purpose**: Generate GTIN, ISBN and per-vertical impact CSV datasets and their manifests, publish them on R2, and
expose the generation status, health and metrics. It never serves a file: downloads come from `data.nudger.fr`
(GOU-2290). Generation runs in `api` only; `front-api` uses the configuration, the manifest records and the CSV
headers; `ui` does not depend on this module.
