---
title: "ADR 0009: XWiki content and identity retirement"
status: accepted
normative: true
audience: PROJECT_SCOPED
decisions: [1, 8, 9]
---

# ADR 0009: XWiki content and identity retirement

## Context

XWiki supplies blog articles, reusable editorial blocs, full pages, attachments and password
authentication. It also contributes sitemap URLs and appears in backup, admin and deployment code.
Removing only the blog would leave a hidden runtime dependency.

## Decision

Blog and editorial content become distinct typed Nuxt Content collections stored as Markdown and
reviewed through Git. Public routes, canonical metadata, feeds, localized fallbacks, attachments
and redirects survive migration. Nuxt owns blog and editorial sitemaps; `ui` keeps product and
brand sitemaps, image delivery and open-data until separate replacements exist.

Authentication uses Google Authorization Code with PKCE through the Nuxt BFF. Front-api validates
the provider identity and applies a deny-by-default, environment-held email-to-role map. Provider
tokens never enter browser application code. Existing sessions use application roles rather than
XWiki group names.

XWiki infrastructure is removed only after both content migrations, SSO production evidence and
a tested offline recovery archive.

## Consequences

Editorial changes become repository changes and runtime availability no longer depends on the
wiki. Account authorization is explicit per environment. XWiki removal cannot delete the deployed
`ui` module or its unrelated responsibilities.
