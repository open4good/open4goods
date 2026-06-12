# Product Data API - B2B frontend UX and UI specification

> Companion document to [`master-prompt.md`](../implementation/master-prompt.md), [`data-coverage.md`](../business/data-coverage.md),
> and [`competition.md`](../business/competition.md). This document defines the
> target UX, page inventory, visual system, and frontend acceptance criteria for
> the future `b2b-frontend` project.

## 1. Purpose

`b2b-frontend` is the public and authenticated web experience for Product Data
API, served on `product-data-api.com`.

The v1 UI must help developers and business buyers understand, test, buy, and
operate the `product.price` API without sales assistance. It must also give
internal administrators enough visibility to support customers, inspect usage,
and grant credits.

The frontend is not a marketing microsite. It is a B2B developer platform with
public documentation, self-serve onboarding, account management, billing,
API-key operations, live playgrounds, and an admin control plane.

## 2. Product UX principles

### 2.1 Target users

| User | Primary goals | UX priorities |
|---|---|---|
| Developer | Validate API quality, create keys, test calls, inspect errors | Fast docs, exact examples, playground, copyable snippets |
| Technical buyer | Understand coverage, no-data-no-pay, billing, freshness | Clear pricing, trust signals, low-friction trial |
| Account owner | Manage organization, credits, invoices, members | Reliable dashboard, billing clarity, role control |
| Internal admin | Diagnose accounts, usage, keys, billing events | Dense tables, audit trail, safe manual actions |

### 2.2 Experience rules

- Make Product Data API the first-viewport brand signal on public pages.
- Lead with the contract: GTIN-first, fresh price data, provenance, and
  no-data-no-pay.
- Treat documentation, playground, and dashboard as one workflow:
  learn, test, create a key, run live calls, monitor consumption.
- Use progressive disclosure: public pages explain; docs specify; dashboard
  operates.
- Never expose stored API secrets in the browser bundle.
- Keep all static UI copy localized in English and French.
- Every route must have localized SEO metadata.

## 3. Visual direction

### 3.1 Style

Use a modern, light, orbital control-plane style.

The desired feel is closer to a clean API platform than to a consumer product
homepage:

- white and ice surfaces;
- strong typography and clear hierarchy;
- restrained blue, green, and cyan accents;
- subtle orbital/data-flow motion for public storytelling;
- dense Vuetify tables and forms in authenticated areas;
- rounded corners at 8px or less for cards, dialogs, and repeated items;
- no decorative gradient blobs, one-note purple palettes, or dark network clone.

### 3.2 Public visual metaphor

The orbital motif represents a product GTIN resolving into normalized price
offers, freshness metadata, provenance, and billable/non-billable states.

Use it in:

- homepage hero;
- docs overview hero;
- playground empty state;
- billing/no-data-no-pay explainer graphics.

Do not overuse it in:

- admin tables;
- API key management;
- billing/invoice screens;
- settings forms.

### 3.3 Layout basis

| Area | Layout |
|---|---|
| Public pages | Top navigation, first-viewport hero, content bands, pricing and docs CTAs |
| Docs | Left navigation, article content, right headings rail on desktop, mobile drawer |
| Playground | Request builder left/top, response and metadata right/bottom, examples rail |
| Dashboard | App shell with left navigation, KPI strip, operational panels, tables |
| Admin | Same shell pattern, denser tables, filters, drawers, audit trail |

### 3.4 State-of-the-art platform patterns

The UI should adopt proven API-platform conventions:

- Stripe-like API key handling: clear secret shown only on creation or rotation,
  strong key metadata, explicit revoke/rotate actions.
- Twilio-like key organization: key name, prefix, creation date, last-used
  timestamp, scope/role context, and safe destructive confirmations.
- Plaid-like sandbox/onboarding: quick trial path, dashboard setup checklist,
  sample data, and environment-aware examples.
- Postman-like API workflow: OpenAPI-backed docs, copyable requests, examples,
  live responses, headers, and error payloads in one place.

## 4. Frontend architecture constraints

`b2b-frontend` must follow the technical decisions in [`master-prompt.md`](../implementation/master-prompt.md):

- Nuxt 4, Vue 3, Vuetify 4, TypeScript.
- `@nuxtjs/i18n` with `prefix_except_default`, default locale `en`, French at
  `/fr/`.
- `@nuxt/content` for public docs.
- `@nuxtjs/seo` for localized metadata.
- Generated OpenAPI client from `b2b-api`.
- Repository-style composables and `domains/` mappers.
- Nuxt server routes for session-backed backend calls.
- No downstream backend calls directly from browser components when secrets,
  session cookies, or machine tokens are involved.

Recommended project shape (illustrative grouping):

> **Layout note.** The actual Infera bootstrap source uses a **flat** Nuxt layout
> (`pages/`, `composables/`, `domains/`, `components/`, `server/`, `content/`,
> `i18n/` at the project root), not the `app/`-nested tree below. Treat the `app/`
> nesting here as conceptual grouping; the canonical layout and the OpenAPI codegen
> pipeline are in [`build.md`](build.md).

```text
b2b-frontend/
  app/
    components/
      admin/
      billing/
      dashboard/
      docs/
      keys/
      landing/
      playground/
      shared/
    composables/
    domains/
    layouts/
    pages/
    stores/
  content/
    en/
    fr/
  server/
    api/
    utils/
  shared/
    api-client/
```

## 5. Route map

### 5.1 Public pages

| Route | Purpose | Required content |
|---|---|---|
| `/` | Product Data API landing page | Value proposition, no-data-no-pay, sample response, pricing CTA, docs CTA |
| `/pricing` | Self-serve pricing | YAML-backed credit packs and subscriptions from backend catalog |
| `/docs` | Documentation home | Product overview, navigation, search, quickstarts |
| `/docs/getting-started` | First integration path | Signup, key creation, first curl call, expected response |
| `/docs/api-reference` | Contract index | OpenAPI-driven endpoint list and shared response envelope |
| `/docs/authentication` | API auth | `Authorization: Bearer pdapi_...`, key creation, rotation, security |
| `/docs/billing-and-credits` | Billing rules | Credits, freshness, billable states, no-data-no-pay |
| `/docs/errors` | Error handling | Problem Detail examples, 401, 402, 404, validation errors |
| `/docs/products/price` | Price facet reference | Endpoint, parameters, response fields, freshness and provenance |
| `/docs/products/price/playground` | Public and authenticated playground | Sample mode for visitors, live mode for authenticated users |
| `/docs/products/price/documentation/java` | Java quickstart | Copyable Java example, dependency notes |
| `/docs/products/price/documentation/python` | Python quickstart | Copyable Python example, dependency notes |
| `/faq` | Product FAQ | Coverage, data quality, billing, support, privacy |
| `/contact` | Contact and support | Sales/support form or mail links, enterprise CTA |
| `/legal` | Legal index | Links to terms, privacy, acceptable use if added later |
| `/privacy` | Privacy policy | Public legal content |
| `/terms` | Terms of service | Public legal content |

### 5.2 Authenticated customer pages

| Route | Purpose | Required content |
|---|---|---|
| `/auth/login` | OIDC login | Google, Microsoft, GitHub, Apple; next redirect support |
| `/dashboard` | Account overview | Credits, usage trend, active keys, setup checklist, recent calls |
| `/dashboard/usage` | Usage analytics | Requests, billable ratio, credits consumed, errors, endpoint filters |
| `/dashboard/api-keys` | API key operations | Create, reveal once, copy, rotate, revoke, last-used metadata |
| `/dashboard/billing` | Billing management | Balance, packs, subscriptions, Stripe checkout/portal actions |
| `/dashboard/invoices` | Invoices | Stripe invoice list, status, amount, download link when available |
| `/dashboard/settings` | Account management | Organization profile, members, roles, security, preferences |

### 5.3 Admin pages

| Route | Purpose | Required content |
|---|---|---|
| `/admin` | Admin overview | Global KPIs, recent incidents, risky accounts, quick actions |
| `/admin/organizations` | Organization list | Search, filters, credits, plan, owner, status |
| `/admin/organizations/[organizationId]` | Organization detail | Members, keys, usage, balance, transactions, manual grants |
| `/admin/usage` | Platform usage | Time-series usage, endpoint filters, billable/non-billable split |
| `/admin/api-keys` | Key oversight | Prefix search, status, owner, revoke action, last used |
| `/admin/billing` | Billing operations | Checkout state, subscriptions, invoices, failed payments |
| `/admin/audit` | Audit events | Admin grants, key revocations, billing events, organization changes |

## 6. Missing pages and v1 decisions

The existing [`master-prompt.md`](../implementation/master-prompt.md) page list covers the core launch, but the following pages
or page concepts are missing or under-specified.

| Missing page or concept | v1 decision |
|---|---|
| `/docs/getting-started` | Add. It is the primary developer onboarding page. |
| `/docs/api-reference` | Add. It gives an OpenAPI-backed index beyond the price article. |
| `/docs/authentication` | Add. Key security deserves a standalone page. |
| `/docs/billing-and-credits` | Add. No-data-no-pay and freshness must be easy to cite. |
| `/docs/errors` | Add. Error handling is central to developer trust. |
| Organization members page | Fold into `/dashboard/settings` as a `Members` tab for v1. |
| User profile page | Fold into `/dashboard/settings` as a `Profile` tab for v1. |
| Security page | Fold into `/dashboard/settings` as a `Security` tab for v1. |
| Support page | Do not add in v1. Use `/contact` and dashboard support links. |
| Status page | Future candidate. Do not block v1 on live status infrastructure. |
| Webhooks page | Future candidate. No v1 webhook product surface is defined. |
| Changelog page | Future candidate. Can be added later through Nuxt Content. |
| Admin catalog page | Future candidate. Billing catalog starts YAML-backed and code-reviewed. |
| CSV enrichment workflow | Out of scope for v1, as defined in [`master-prompt.md`](../implementation/master-prompt.md). |
| Enterprise invoicing | Out of scope for v1. Use Stripe billing only. |

## 7. Page-level UX requirements

### 7.1 Home page

The homepage must sell the API contract and lead developers into docs or signup.

Required sections:

1. Hero: Product Data API headline, short value proposition, primary CTA to
   create account, secondary CTA to docs, light orbital GTIN-to-price visual.
2. Proof strip: fresh price data, no-data-no-pay, GTIN-first, provenance.
3. API preview: copyable curl request and abbreviated JSON response envelope.
4. How billing works: invalid GTIN, missing product, empty/stale price data are
   not billed; fresh price data consumes credits.
5. Facet roadmap preview: price in v1, identity/attributes/impact/energy/review
   as future capabilities without implying v1 availability.
6. Pricing teaser: backend catalog values, not hardcoded.
7. Documentation CTA: quickstart and playground links.

The first viewport must show the product name and at least a hint of the next
section on desktop and mobile.

### 7.2 Account management

Account management lives under `/dashboard/settings` with tabs.

Required tabs:

- Profile: name, email, avatar from OIDC session; mostly read-only in v1.
- Organization: display name, billing email, default language.
- Members: members table, roles `OWNER`, `ADMIN`, `DEVELOPER`, `BILLING`.
- Security: active sessions if available, OIDC provider list, API-key guidance.
- Preferences: language and theme preference.

Role rules:

- `OWNER`: can transfer ownership, manage all settings, billing, members, keys.
- `ADMIN`: can manage members, keys, usage, billing except ownership transfer.
- `DEVELOPER`: can manage own API keys and use playground.
- `BILLING`: can view billing, invoices, and transactions.

Disabled actions must explain the missing role.

### 7.3 Admin section

Admin pages must prioritize diagnosis and reversible operations.

Required interaction patterns:

- Server-side pagination and filtering on large tables.
- Search by organization name, owner email, key prefix, request ID.
- Detail drawers for event and key metadata.
- Confirmation dialogs for revocation and manual grants.
- Audit entries for all manual admin actions.
- No destructive bulk actions in v1.

Manual credit grant flow:

1. Admin opens organization detail.
2. Admin selects `Manual grant`.
3. Form captures credits, reason, optional expiration, and internal note.
4. Confirmation dialog shows organization, amount, and reason.
5. Result creates ledger transaction and audit event.

### 7.4 Vue Content documentation pages

Use `@nuxt/content` with localized content roots:

```text
content/
  en/
    docs/
      getting-started.md
      api-reference.md
      authentication.md
      billing-and-credits.md
      errors.md
      products/
        price.md
        price/
          documentation/
            java.md
            python.md
  fr/
    docs/
      ...
```

Docs requirements:

- English and French pages must both exist.
- French pages must be real translated content, not empty placeholders.
- Use typed frontmatter for title, description, tags, navigation weight, and SEO.
- Use content collections and navigation queries instead of hand-coded menus.
- Use MDC components for API callouts, response examples, pricing notes, and
  playground links.
- The docs shell must support search, mobile navigation, copy buttons, heading
  anchors, and previous/next navigation.

### 7.5 API playground

The playground has two modes.

Public sample mode:

- visible to anonymous users;
- lets users change GTIN and language against mocked/sample responses;
- shows request, response, headers, metadata, and billing explanation;
- CTA to sign in for live calls.

Authenticated live mode:

- requires session auth;
- uses selected organization context;
- lets users select an API key by name/prefix;
- calls a backend session-authenticated playground proxy;
- shows the exact external API request that was executed;
- shows response body, headers, credits consumed, remaining balance, billable
  state, no-data-no-pay reason, and response time.

The live playground must not require the browser to know a stored clear API
secret. API secrets are shown only during creation or rotation.

Recommended backend endpoint:

```http
POST /api/v1/customer/playground/products/price
Content-Type: application/json
Cookie: session

{
  "apiKeyId": "key_...",
  "gtin": "0885909950805",
  "language": "en"
}
```

The proxy response should include:

```json
{
  "request": {
    "method": "GET",
    "path": "/api/v1/products/0885909950805/price?language=en",
    "headers": {
      "Authorization": "Bearer pdapi_...masked"
    }
  },
  "response": {
    "status": 200,
    "headers": {},
    "body": {}
  },
  "metering": {
    "billable": true,
    "creditsConsumed": 5,
    "creditsRemaining": 2495,
    "reason": "fresh-offer"
  }
}
```

## 8. Reusable UI components

Build reusable components before page-specific copies.

| Component | Purpose |
|---|---|
| `B2bPageHeader` | Title, subtitle, breadcrumbs, actions |
| `B2bKpiCard` | Dashboard/admin metrics |
| `B2bAsyncState` | Loading, empty, error, permission-denied states |
| `B2bDataTable` | Server-side tables with filters and pagination |
| `B2bStatusChip` | Key, billing, request, and organization states |
| `B2bCodeBlock` | Copyable code and JSON examples |
| `B2bProblemDetail` | RFC 9457 error display |
| `B2bCreditMeter` | Credits remaining, consumed, projected exhaustion |
| `B2bApiKeyTable` | Shared customer/admin key table foundation |
| `B2bApiKeyDialog` | Create, rotate, reveal once, revoke flows |
| `B2bUsageChart` | Usage and credit time series |
| `B2bBillingCatalog` | Packs and subscriptions from backend catalog |
| `B2bPlaygroundShell` | Request/response playground layout |
| `B2bOrbitalDataVisual` | Light orbital public-page visual |

## 9. Data and state requirements

### 9.1 Dashboard states

Every dashboard page must support:

- loading;
- empty organization;
- no credits yet;
- insufficient role;
- backend error;
- partial data unavailable;
- successful populated state.

### 9.2 API key states

Show:

- name;
- prefix;
- role/scope if present;
- status: active, revoked, rotated;
- created at;
- last used at;
- created by;
- rotation/revocation actions based on role.

The clear secret is displayed only once after create or rotate. The UI must
provide copy and "I have saved this key" confirmation before closing the dialog.

### 9.3 Usage states

Usage analytics must distinguish:

- total requests;
- billable requests;
- non-billable no-data-no-pay requests;
- invalid GTIN;
- product not found;
- empty or stale price data;
- 401 unauthorized;
- 402 insufficient credits;
- latency and response-time percentiles when available.

## 10. Content and copy guidelines

Preferred messaging:

- "No data, no pay."
- "Fresh GTIN-first product price data."
- "You are billed only when the requested facet returns usable fresh data."
- "Invalid GTINs, missing products, and stale or empty price data cost zero
  credits."

Avoid:

- claiming full future facet availability in v1;
- implying marketplace stock or shipping data exists in v1;
- promising full global coverage;
- exposing internal nudger crawler, compensation, or affiliate details.

## 11. Acceptance criteria

The frontend implementation should be considered v1-ready when:

- Public pages render in English and French with localized SEO metadata.
- Pricing is loaded from `GET /api/v1/customer/billing/catalog`.
- A visitor can reach docs, search docs, and open price API examples.
- A user can log in with OIDC and land on `/dashboard`.
- A first organization receives the free credit grant once.
- A user can create an API key and see the clear secret once.
- A user can rotate and revoke keys with confirmation.
- The live playground can execute the price endpoint through the session proxy.
- Playground states cover success, 401, 402, invalid GTIN, product not found,
  and no-data-no-pay.
- Billing screens show packs, subscriptions, balance, transactions, and invoices.
- Admin pages are hidden from non-admin users.
- Admin can inspect organizations, keys, usage, billing, and audit events.
- Admin can create a manual credit grant with an audit event.
- Mobile layouts have no overlapping text or controls.
- Reduced-motion preference disables non-essential orbital animation.

## 12. Validation plan

Required frontend validation after implementation:

```bash
pnpm --dir b2b-frontend lint
pnpm --dir b2b-frontend typecheck
pnpm --dir b2b-frontend test
pnpm --dir b2b-frontend build
```

Required focused tests:

- i18n routing for `/` and `/fr/`;
- localized SEO metadata on public pages;
- docs route rendering and docs search;
- pricing catalog rendering from backend data;
- login/session state;
- dashboard balance and usage states;
- API key create, reveal, rotate, revoke UI;
- playground success, 401, 402, and no-data-no-pay states;
- admin role gating;
- admin manual grant flow.

## 13. Open decisions for future iterations

These are deliberately not v1 blockers:

- Dedicated `/status` page backed by uptime and incident data.
- Dedicated `/support` portal beyond `/contact`.
- Public changelog under Nuxt Content.
- Webhook UI if billing or usage webhooks become customer-facing.
- CSV enrichment workflow.
- SDK package download pages if Java/Python SDKs are published.
- Enterprise invoicing outside Stripe.
- Admin billing catalog editor.

