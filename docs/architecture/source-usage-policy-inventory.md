---
title: "Source usage policy inventory"
normative: false
audience: PROJECT_SCOPED
---

# Source usage policy inventory

The Git-authored [policy inventory](../../services/data-reference/src/main/resources/policy/source-usage-policies.json)
is the publication authority for source evidence. Its typed loader denies a
missing policy, a source mismatch, an unreviewed policy, a prohibited
redistribution setting, an out-of-period policy and a revoked policy. Derived
fields never inherit a provider permission.

The inventory records the content observed in the reference contracts. An API,
a legacy public page or a source's receipt of merchant content is supporting
evidence, not an O4G redistribution approval. All entries below are intentionally
unreviewed and publish to no `NUDGER_WEB`, `B2B_API` or `ODBL_EXPORT` surface.

| Source / version | Content types inventoried | Supporting evidence | Period / retention | Attribution / media | Owner decision |
|---|---|---|---|---|---|
| EPREL public API / 1 | identity, classification, attribute, text, media, relation | [API terms](https://ec.europa.eu/assets/move-ener/eprel/EPREL%20Public/Public%20API%20Term%20and%20Conditions/API_TERMS_AND_CONDITIONS_EN.pdf), [Regulation 2024/994](https://eur-lex.europa.eu/legal-content/EN/TXT/?uri=CELEX:02024R0994-20240402) | 2024-06-03 onward / none | unset / no cache | approve exact public fields, surfaces, attribution and retention |
| Icecat open content / 1 | identity, classification, attribute, text, media, relation | [subscription](https://icecat.com/content-subscription/), [content overview](https://icecat.com/structured-data-content-users/) | 2026-09-12 onward / none | unset / no cache | identify the applicable license and approved brand/content scope |
| Merchant feed / 1 | identity, attribute, text, media, offer, price | [Merchant specification](https://support.google.com/merchants/answer/7052112?hl=en-GB) | 2026-09-12 onward / none | unset / no cache | obtain each merchant's grant, price retention and display terms |
| Legacy product backup / 1 | identity, classification, attribute, text, media, relation, offer, price | [private input contract](../operations/product-backup-input-contract.md) | 2026-09-12 onward / none | unset / no cache | establish provenance per field before any publication |
| Amazon PA-API / 1 | identity, classification, attribute, text, media, relation, offer, price | [Associates policies](https://affiliate-program.amazon.com/help/operating/policies?ac-ms-src=ac-nav), [API cache guide](https://webservices.amazon.co.uk/paapi5/documentation/best-programming-practices.html) | 2026-09-12 onward / none | unset / no cache | remains quarantined; no approval is requested for historical payloads |

## Owner review packet

Each unresolved row is a packet with the source/version and content/surface
matrix above, the linked supporting evidence, proposed effective dates,
retention, attribution wording and media-cache setting. The owner records one
of: an exact reviewed policy version, a revocation timestamp, or an explicit
denial. The policy resource retains all prior versions for replay; a changed
term produces a new record rather than editing historical permission.

`SourceUsagePolicyRegistryTest` loads both this deny inventory and a reviewed
fixture. It exercises every publication surface at effective, expiry and
revocation boundaries, requires attribution, rejects prohibited media caching,
and rejects derived-field inheritance.
